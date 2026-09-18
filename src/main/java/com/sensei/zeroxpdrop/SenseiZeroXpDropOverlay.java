/*
 * Copyright (c) 2026, Sensei
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.sensei.zeroxpdrop;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

/**
 * Draws the fake "miss" drop (default "+0"). The native xp-drop script can't
 * render a 0 - it feeds the value into the game's drop accumulator, which skips
 * a slot whose total is 0 - so we paint our own, anchored to the real xp-drop
 * widget so it lands in the same place as your genuine drops.
 */
class SenseiZeroXpDropOverlay extends Overlay
{
	private final Client client;
	private final SenseiZeroXpDropPlugin plugin;
	private final SenseiZeroXpDropConfig config;
	private final SpriteManager spriteManager;

	// Lazily-loaded, cached skill icons.
	private final Map<Skill, BufferedImage> iconCache = new EnumMap<>(Skill.class);

	// Last known screen anchor (right edge / vertical centre of the xp tracker
	// widget), kept so drops still render on the odd frame the widget reports
	// hidden / zero-sized.
	private int anchorRightX = -1;
	private int anchorY = -1;

	@Inject
	private SenseiZeroXpDropOverlay(Client client, SenseiZeroXpDropPlugin plugin, SenseiZeroXpDropConfig config,
		SpriteManager spriteManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.spriteManager = spriteManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_MED);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final List<SenseiZeroXpDropPlugin.FakeDrop> drops = plugin.getFakeDrops();
		if (drops.isEmpty())
		{
			return null;
		}

		// Anchor to the movable XP tracker widget (top-right by default) so we land
		// where real drops do, not at screen centre like the full-width drops layer.
		Widget anchor = client.getWidget(InterfaceID.XpDrops.CONTAINER);
		if (anchor == null || anchor.isHidden())
		{
			anchor = client.getWidget(InterfaceID.XpDrops.DROPS_CONTAINER);
		}
		if (anchor != null && !anchor.isHidden())
		{
			final Rectangle b = anchor.getBounds();
			if (b != null && b.width > 0 && b.height > 0)
			{
				anchorRightX = b.x + b.width;
				anchorY = b.y + b.height / 2;
			}
		}

		if (anchorRightX < 0)
		{
			// Never seen the widget - can't place the drop yet. Age the queue out
			// anyway so nothing gets stuck.
			ageOut(drops);
			return null;
		}

		final int durationMs = Math.max(1, config.dropDurationMs());
		final int drift = config.dropDriftPixels();
		final int xOffset = config.dropXOffset();
		final int yOffset = config.dropYOffset();
		final Color color = config.dropColor();
		final long now = System.currentTimeMillis();

		graphics.setFont(graphics.getFont().deriveFont((float) Math.max(1, config.dropTextSize())));
		final FontMetrics fm = graphics.getFontMetrics();

		for (Iterator<SenseiZeroXpDropPlugin.FakeDrop> it = drops.iterator(); it.hasNext(); )
		{
			final SenseiZeroXpDropPlugin.FakeDrop drop = it.next();
			final long elapsed = now - drop.getSpawnMillis();
			if (elapsed >= durationMs)
			{
				it.remove();
				continue;
			}

			final float progress = (float) elapsed / durationMs;
			final int alpha = config.fadeEnabled()
				? Math.max(0, Math.min(255, (int) (255 * (1f - progress))))
				: 255;
			final String text = drop.getText();

			// Right-aligned to the widget edge like native drops; positive drift = up.
			final int textWidth = fm.stringWidth(text);
			final int x = anchorRightX + xOffset - textWidth;
			final int y = anchorY + yOffset + fm.getAscent() / 2 - (int) (drift * progress);

			graphics.setColor(ColorUtil.colorWithAlpha(Color.BLACK, alpha));
			graphics.drawString(text, x + 1, y + 1);
			graphics.setColor(ColorUtil.colorWithAlpha(color, alpha));
			graphics.drawString(text, x, y);

			// Skill icon to the left of the number, like native drops. Scaled to the
			// configured height (keeping aspect) since the raw sprite is oversized.
			final BufferedImage icon = config.showSkillIcon() ? icon(drop.getSkill()) : null;
			if (icon != null)
			{
				final int targetH = Math.max(1, config.iconSize());
				final int targetW = Math.max(1, icon.getWidth() * targetH / icon.getHeight());
				final int iconX = x - 2 - targetW;
				final int iconY = y - fm.getAscent() / 2 - targetH / 2;
				graphics.drawImage(ImageUtil.alphaOffset(icon, alpha - 255), iconX, iconY, targetW, targetH, null);
			}
		}

		return null;
	}

	private BufferedImage icon(Skill skill)
	{
		if (skill == null)
		{
			return null;
		}

		BufferedImage img = iconCache.get(skill);
		if (img == null)
		{
			final int spriteId = spriteFor(skill);
			if (spriteId >= 0)
			{
				img = spriteManager.getSprite(spriteId, 0);
				if (img != null)
				{
					iconCache.put(skill, img);
				}
			}
		}
		return img;
	}

	private static int spriteFor(Skill skill)
	{
		switch (skill)
		{
			case ATTACK:
				return SpriteID.Staticons.ATTACK;
			case STRENGTH:
				return SpriteID.Staticons.STRENGTH;
			case DEFENCE:
				return SpriteID.Staticons.DEFENCE;
			case RANGED:
				return SpriteID.Staticons.RANGED;
			case MAGIC:
				return SpriteID.Staticons.MAGIC;
			case HITPOINTS:
				return SpriteID.Staticons.HITPOINTS;
			default:
				return -1;
		}
	}

	private static void ageOut(List<SenseiZeroXpDropPlugin.FakeDrop> drops)
	{
		final long now = System.currentTimeMillis();
		drops.removeIf(d -> now - d.getSpawnMillis() >= 5000);
	}
}
