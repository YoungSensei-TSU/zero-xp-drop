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
import net.runelite.api.Skill;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(ZeroXpDropConfig.GROUP)
public interface ZeroXpDropConfig extends Config
{
	String GROUP = "zeroxpdrop";

	enum RenderMode
	{
		/**
		 * Native game xp drop via the XPDROP_DISABLED script. Renders in the exact
		 * spot / style as real drops, but cannot show a literal 0 (the game skips a
		 * drop whose total is 0), so use a non-zero amount to see it.
		 */
		NATIVE,
		/**
		 * Custom drawn overlay. Can show a literal +0 and any colour/direction, but
		 * position/animation are approximated rather than pixel-identical to native.
		 */
		OVERLAY
	}

	enum DropSkill
	{
		RANGED(Skill.RANGED),
		MAGIC(Skill.MAGIC),
		ATTACK(Skill.ATTACK),
		STRENGTH(Skill.STRENGTH),
		DEFENCE(Skill.DEFENCE),
		HITPOINTS(Skill.HITPOINTS);

		private final Skill skill;

		DropSkill(Skill skill)
		{
			this.skill = skill;
		}

		public Skill getSkill()
		{
			return skill;
		}
	}

	@ConfigSection(
		name = "Native drop",
		description = "Choose the render mode, and set up the drop the game itself renders.",
		position = 0
	)
	String nativeSection = "native";

	@ConfigSection(
		name = "Custom overlay",
		description = "Alternative drop drawn by the plugin. Can show a real +0.",
		position = 1,
		closedByDefault = true
	)
	String overlaySection = "overlay";

	@ConfigSection(
		name = "Detection",
		description = "How an attack is detected.",
		position = 2,
		closedByDefault = true
	)
	String detectionSection = "detection";

	@ConfigItem(
		keyName = "renderMode",
		name = "Render mode",
		description = "Overlay: custom drawing that shows a literal +0 (default). Native: the game's own xp drop, which can't show 0.",
		position = 0,
		section = nativeSection
	)
	default RenderMode renderMode()
	{
		return RenderMode.OVERLAY;
	}

	@ConfigItem(
		keyName = "dropSkill",
		name = "Drop skill",
		description = "Native mode only. Which skill's icon the drop shows on. Match what you train.",
		position = 1,
		section = nativeSection
	)
	default DropSkill dropSkill()
	{
		return DropSkill.RANGED;
	}

	@Range(min = 1)
	@ConfigItem(
		keyName = "fakeXpAmount",
		name = "Fake XP amount",
		description = "Native mode only. The number the drop shows. Minimum 1 - the game can't render a 0 drop natively.",
		position = 2,
		section = nativeSection
	)
	default int fakeXpAmount()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "dropText",
		name = "Miss drop text",
		description = "Overlay mode. The text drawn when you attack and miss.",
		position = 0,
		section = overlaySection
	)
	default String dropText()
	{
		return "0";
	}

	@Alpha
	@ConfigItem(
		keyName = "dropColor",
		name = "Miss drop colour",
		description = "Overlay mode. Colour of the miss drop text.",
		position = 1,
		section = overlaySection
	)
	default Color dropColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "dropDurationMs",
		name = "Drop duration (ms)",
		description = "Overlay mode. How long the drop stays on screen.",
		position = 2,
		section = overlaySection
	)
	default int dropDurationMs()
	{
		return 2000;
	}

	@ConfigItem(
		keyName = "dropDriftPixels",
		name = "Drop drift (px, + = up)",
		description = "Overlay mode. How far the drop travels over its lifetime. Positive drifts up, negative drifts down.",
		position = 3,
		section = overlaySection
	)
	default int dropDriftPixels()
	{
		return 200;
	}

	@ConfigItem(
		keyName = "fadeEnabled",
		name = "Fade out",
		description = "Overlay mode. Gradually fade the drop's opacity over its lifetime. When off, the drop stays fully opaque until it disappears.",
		position = 4,
		section = overlaySection
	)
	default boolean fadeEnabled()
	{
		return false;
	}

	@Range(min = -5000, max = 5000)
	@ConfigItem(
		keyName = "dropXOffset",
		name = "X offset (px)",
		description = "Overlay mode. Nudge the drop horizontally to line it up with your real drops. (or you can alt drag the plugin window itself)",
		position = 5,
		section = overlaySection
	)
	default int dropXOffset()
	{
		return 0;
	}

	@Range(min = -5000, max = 5000)
	@ConfigItem(
		keyName = "dropYOffset",
		name = "Y offset (px)",
		description = "Overlay mode. Nudge the drop vertically to line it up with your real drops. (or you can alt drag the plugin window itself)",
		position = 6,
		section = overlaySection
	)
	default int dropYOffset()
	{
		return 200;
	}

	@ConfigItem(
		keyName = "showSkillIcon",
		name = "Show skill icon",
		description = "Overlay mode. Draw your current attack style's skill icon next to the drop, like native xp drops.",
		position = 7,
		section = overlaySection
	)
	default boolean showSkillIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dropTextSize",
		name = "Text size (px)",
		description = "Overlay mode. Font size of the drop text.",
		position = 8,
		section = overlaySection
	)
	default int dropTextSize()
	{
		return 14;
	}

	@ConfigItem(
		keyName = "iconSize",
		name = "Icon size (px)",
		description = "Overlay mode. Height the skill icon is scaled to. Around 18 matches native xp drops.",
		position = 9,
		section = overlaySection
	)
	default int iconSize()
	{
		return 12;
	}

	@ConfigItem(
		keyName = "detectAnimation",
		name = "Detect via animation",
		description = "Count your attack animation as an attack. This is the reliable signal for ranged/magic/melee.",
		position = 0,
		section = detectionSection
	)
	default boolean detectAnimation()
	{
		return true;
	}

	@ConfigItem(
		keyName = "detectProjectile",
		name = "Detect via projectile",
		description = "Also count a projectile leaving you as an attack. Backup signal; some weapons don't report a source actor.",
		position = 1,
		section = detectionSection
	)
	default boolean detectProjectile()
	{
		return false;
	}

	@ConfigItem(
		keyName = "requireNpcTarget",
		name = "Require NPC target",
		description = "Only fire the miss drop while you are interacting with an NPC.",
		position = 2,
		section = detectionSection
	)
	default boolean requireNpcTarget()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ignoredAnimationIds",
		name = "Ignored animation IDs",
		description = "Comma-separated animation IDs to never treat as an attack, e.g. non-combat actions like applying poison to start a boss fight. "
			+ "Turn on 'Log timing to console', trigger the action, and read the id off the 'ANIM <id>' line in the console.",
		position = 3,
		section = detectionSection
	)
	default String ignoredAnimationIds()
	{
		return "";
	}

	@ConfigItem(
		keyName = "logToConsole",
		name = "Log timing to console",
		description = "Log the tick numbers of attack / xp / hitsplat events. Use this to calibrate, then turn it off.",
		position = 4,
		section = detectionSection
	)
	default boolean logToConsole()
	{
		return false;
	}
}
