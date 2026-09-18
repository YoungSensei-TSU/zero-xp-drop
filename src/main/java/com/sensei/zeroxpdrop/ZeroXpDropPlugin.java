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

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.EnumID;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.ParamID;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.ScriptID;
import net.runelite.api.Skill;
import net.runelite.api.StructComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * Shows an XP drop (default +0) on the exact tick you attack and MISS, so that
 * xp-drop-timed techniques (e.g. the Bandos altar-door ranging flinch) stay in
 * sync even when a hit rolls a 0 and the game gives no real drop.
 *
 * Core idea: we never try to detect the miss directly (the server sends no early
 * signal for a miss). Instead we detect the ATTACK - which is identical for hits
 * and misses - and treat "attacked this tick but gained no combat XP this tick"
 * as the miss, firing the fake drop on that same tick.
 *
 * This build also logs the tick numbers of attack / xp / hitsplat events. That
 * lets us confirm the key assumption for ranged: that XP is credited on the tick
 * you SHOOT, not the later tick the projectile lands. Watch the console, then
 * turn logging off once calibrated.
 */
@Slf4j
@PluginDescriptor(
	name = "0 XP Drop",
	description = "Shows a +0 XP drop when your attack misses, so every attack gets a drop. Keeps xp-drop timing in sync for flinching (Bandos door, etc.)",
	tags = {"xp", "drop", "xpdrop", "experience", "miss", "missed", "zero", "0", "tick", "timing", "flinch", "flinching", "bandos", "graardor", "gwd", "godwars", "safespot", "ranged", "range", "melee", "combat", "pvm"}
)
public class ZeroXpDropPlugin extends Plugin
{
	private static final Skill[] COMBAT_SKILLS = {
		Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC, Skill.HITPOINTS
	};

	@Inject
	private Client client;

	@Inject
	private ZeroXpDropConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ZeroXpDropOverlay overlay;

	// Tracks projectiles we've already counted so each one only registers as one attack.
	// Weak keys let the client GC despawned projectiles for us.
	private final Set<Projectile> knownProjectiles = Collections.newSetFromMap(new WeakHashMap<>());

	// Last known combat XP totals, to detect a real gain (delta > 0) this tick.
	private final Map<Skill, Integer> combatXp = new EnumMap<>(Skill.class);

	// Active miss drops the overlay is animating. Both the game thread (add) and
	// the overlay (render/remove) touch this on the client thread.
	@Getter
	private final List<FakeDrop> fakeDrops = new ArrayList<>();

	// Tick numbers of the most recent signal of each kind. Compared against the
	// current tick in onGameTick; stale values simply won't match.
	private int animationAttackTick = -1;
	private int animationAttackId = -1;
	private int combatXpTick = -1;
	private int lastMissDropTick = -1;

	// The last non-Hitpoints combat skill we gained xp in - i.e. the current attack
	// style. Used to pick the skill icon shown on the custom overlay drop.
	private Skill lastAttackSkill = null;

	// Player's world location at the previous game tick, so a projectile whose
	// source tile we read a tick later still matches (handles moving between shots).
	private WorldPoint prevPlayerLocation = null;

	@Provides
	ZeroXpDropConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ZeroXpDropConfig.class);
	}

	@Override
	protected void startUp()
	{
		reset();
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		reset();
	}

	private void reset()
	{
		knownProjectiles.clear();
		combatXp.clear();
		fakeDrops.clear();
		animationAttackTick = -1;
		animationAttackId = -1;
		combatXpTick = -1;
		lastMissDropTick = -1;
		prevPlayerLocation = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		// Clear stale XP baselines so the first StatChanged after a login/hop
		// doesn't get misread as a gain.
		if (e.getGameState() == GameState.LOGGING_IN
			|| e.getGameState() == GameState.HOPPING
			|| e.getGameState() == GameState.LOGIN_SCREEN)
		{
			reset();
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged e)
	{
		final Player local = client.getLocalPlayer();
		if (local == null || e.getActor() != local)
		{
			return;
		}

		final int anim = local.getAnimation();
		if (anim == -1)
		{
			return;
		}

		// Record the animation; onGameTick decides whether it's an attack by checking
		// it against the known block/defend animation IDs (PlayerBlockAnimations).
		animationAttackTick = client.getTickCount();
		animationAttackId = anim;

		if (config.logToConsole())
		{
			log.info("[0XpDrop] t={} ANIM {}", animationAttackTick, anim);
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged e)
	{
		final Skill skill = e.getSkill();
		if (!isCombatSkill(skill))
		{
			return;
		}

		final int xp = e.getXp();
		final Integer prev = combatXp.put(skill, xp);
		if (prev != null && xp > prev)
		{
			combatXpTick = client.getTickCount();

			// Remember the attack style (the non-Hitpoints combat skill we're gaining)
			// so miss drops can show the matching skill icon, like native drops do.
			if (skill != Skill.HITPOINTS)
			{
				lastAttackSkill = skill;
			}

			if (config.logToConsole())
			{
				log.info("[0XpDrop] t={} XP +{} {}", combatXpTick, xp - prev, skill);
			}
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied e)
	{
		// Calibration only: log our own hitsplats so we can see how many ticks
		// AFTER the shot the (0 or real) hitsplat actually lands.
		if (!config.logToConsole())
		{
			return;
		}

		final Hitsplat h = e.getHitsplat();
		if (!h.isMine())
		{
			return;
		}

		log.info("[0XpDrop] t={} HITSPLAT mine amount={} type={} on {}",
			client.getTickCount(), h.getAmount(), h.getHitsplatType(),
			e.getActor() == null ? "?" : e.getActor().getName());
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		// Everything is evaluated here, on the logic tick, so the drop lands at the
		// same moment real drops do. onAnimationChanged and the XP StatChanged both
		// fire before GameTick, and new projectiles are already in the projectile
		// list by now (they're added during packet processing, before GameTick -
		// unlike the ProjectileMoved event, which is a later render-pass event).
		final int tick = client.getTickCount();
		final Player local = client.getLocalPlayer();
		final WorldPoint myLoc = local == null ? null : local.getWorldLocation();

		evaluateAnimation(tick);

		if (config.detectProjectile() && local != null)
		{
			evaluateProjectiles(tick, local, myLoc);
		}

		prevPlayerLocation = myLoc;
	}

	private void evaluateAnimation(int tick)
	{
		if (animationAttackTick != tick || !config.detectAnimation())
		{
			return;
		}

		// Block/defend animations are identified directly by ID (PlayerBlockAnimations)
		// rather than inferred from xp/hitsplat timing. This needs no per-weapon
		// learning - so it's correct from the very first swing of any weapon or
		// style, including right after a switch - and unlike a "did we take damage
		// this tick" heuristic, it can't mistake our own attack for a block on a tick
		// where we're also hit by the enemy.
		if (PlayerBlockAnimations.isBlock(animationAttackId))
		{
			if (config.logToConsole())
			{
				log.info("[0XpDrop] t={} ANIM {} ignored (block/defend animation)", tick, animationAttackId);
			}
			return;
		}

		// Non-combat action animations (e.g. applying poison to start a boss fight)
		// aren't blocks, but also aren't attacks. These can't be enumerated up front
		// like blocks - every quest/boss can have its own - so the user adds ids as
		// they're found, via the "Ignored animation IDs" setting.
		if (parseIgnoredAnimationIds(config.ignoredAnimationIds()).contains(animationAttackId))
		{
			if (config.logToConsole())
			{
				log.info("[0XpDrop] t={} ANIM {} ignored (user-ignored animation)", tick, animationAttackId);
			}
			return;
		}

		tryFireMiss(tick);
	}

	private static Set<Integer> parseIgnoredAnimationIds(String csv)
	{
		if (csv == null || csv.isBlank())
		{
			return Collections.emptySet();
		}

		final Set<Integer> ids = new HashSet<>();
		for (String token : csv.split(","))
		{
			final String trimmed = token.trim();
			if (trimmed.isEmpty())
			{
				continue;
			}
			try
			{
				ids.add(Integer.parseInt(trimmed));
			}
			catch (NumberFormatException ignored)
			{
				// Skip malformed entries rather than breaking detection entirely.
			}
		}
		return ids;
	}

	private void evaluateProjectiles(int tick, Player local, WorldPoint myLoc)
	{
		final int gameCycle = client.getGameCycle();
		for (Projectile p : client.getProjectiles())
		{
			// Each projectile is only considered once (first tick we see it).
			if (!knownProjectiles.add(p))
			{
				continue;
			}

			// Only projectiles that started this tick - avoids counting an in-flight
			// projectile as a fresh attack when the plugin is enabled mid-combat.
			if (gameCycle - p.getStartCycle() > 15)
			{
				continue;
			}

			// Prefer the source actor; fall back to the source tile matching where we
			// are this tick (or were last tick, in case the list read lags a tick).
			final boolean byActor = p.getSourceActor() == local;
			final WorldPoint sp = p.getSourcePoint();
			final boolean byPoint = sp != null && (sp.equals(myLoc) || sp.equals(prevPlayerLocation));
			final boolean fromUs = byActor || byPoint;

			// An enemy's attack on us is a projectile aimed AT the local player. Our
			// attacks are aimed at the enemy, never at ourselves. So exclude anything
			// targeting us, even if its source tile coincides with where we stood -
			// this is what stops enemy hits being tracked as our own.
			final Actor target = p.getTargetActor();
			final boolean aimedAtUs = target == local;
			final boolean mine = fromUs && !aimedAtUs;

			if (config.logToConsole())
			{
				log.info("[0XpDrop] t={} PROJECTILE id={} mine={} (byActor={} byPoint={} aimedAtUs={}) start={} cycle={} src={} me={}",
					tick, p.getId(), mine, byActor, byPoint, aimedAtUs, p.getStartCycle(), gameCycle, sp, myLoc);
			}

			if (mine)
			{
				tryFireMiss(tick);
			}
		}
	}

	/**
	 * Fire a miss drop for {@code tick} if that attack gained no combat xp. Deduped
	 * so the animation and projectile detectors can't both drop on the same tick.
	 */
	private void tryFireMiss(int tick)
	{
		if (lastMissDropTick == tick)
		{
			return;
		}

		final boolean gainedCombatXp = combatXpTick == tick;

		if (config.logToConsole())
		{
			log.info("[0XpDrop] t={} EVAL attacked=true gainedXp={} -> {}",
				tick, gainedCombatXp, gainedCombatXp ? "hit (real drop)" : "MISS (fake drop)");
		}

		if (gainedCombatXp)
		{
			// A real drop is already showing this tick - nothing to do.
			return;
		}

		if (config.requireNpcTarget())
		{
			final Player local = client.getLocalPlayer();
			if (local == null || !(local.getInteracting() instanceof NPC))
			{
				return;
			}
		}

		lastMissDropTick = tick;
		showFakeDrop();
	}

	private void showFakeDrop()
	{
		if (config.renderMode() == ZeroXpDropConfig.RenderMode.OVERLAY)
		{
			// Custom drawing - can show a literal +0. Read the current attack style
			// live (from the weapon + attack-style varbits) so the icon is correct the
			// instant you switch styles, rather than lagging behind the last skill we
			// gained xp in. Fall back to that last skill only if it can't be resolved.
			Skill iconSkill = currentAttackStyleSkill();
			if (iconSkill == null)
			{
				iconSkill = lastAttackSkill;
			}
			fakeDrops.add(new FakeDrop(System.currentTimeMillis(), config.dropText(), iconSkill));
		}
		else
		{
			// Native game drop - same call the core XP Drop plugin uses. Note the
			// game won't render a 0, so this needs a non-zero Fake XP amount.
			final Skill skill = config.dropSkill().getSkill();
			client.runScript(ScriptID.XPDROP_DISABLED, skill.ordinal(), config.fakeXpAmount());
		}
	}

	/**
	 * The skill the current attack style trains, read live from the weapon type and
	 * attack-style varbits (the same source the Attack Styles plugin uses). Returns
	 * null if it can't be resolved. This reflects a style switch immediately, unlike
	 * xp-derived tracking which only updates on a hit.
	 */
	private Skill currentAttackStyleSkill()
	{
		try
		{
			final int weaponType = client.getVarbitValue(VarbitID.COMBAT_WEAPON_CATEGORY);
			int styleIndex = client.getVarpValue(VarPlayerID.COM_MODE);

			final int weaponStyleEnum = client.getEnum(EnumID.WEAPON_STYLES).getIntValue(weaponType);
			if (weaponStyleEnum == -1)
			{
				return null;
			}

			// Staves: index 4 is the spell slot; the defensive-casting mode bumps it to 5.
			if (styleIndex == 4)
			{
				styleIndex += client.getVarbitValue(VarbitID.AUTOCAST_DEFMODE);
			}

			final int[] structs = client.getEnum(weaponStyleEnum).getIntVals();
			if (styleIndex < 0 || styleIndex >= structs.length)
			{
				return null;
			}

			final StructComposition struct = client.getStructComposition(structs[styleIndex]);
			final String name = struct.getStringValue(ParamID.ATTACK_STYLE_NAME);
			return skillForStyleName(name, styleIndex);
		}
		catch (RuntimeException ex)
		{
			return null;
		}
	}

	private static Skill skillForStyleName(String name, int styleIndex)
	{
		if (name == null)
		{
			return null;
		}

		switch (name.toLowerCase())
		{
			case "accurate":
			case "controlled":
				return Skill.ATTACK;
			case "aggressive":
				return Skill.STRENGTH;
			case "defensive":
				// At the spell slot "Defensive" means defensive casting -> Magic.
				return styleIndex >= 5 ? Skill.MAGIC : Skill.DEFENCE;
			case "ranging":
			case "longrange":
				return Skill.RANGED;
			case "casting":
				return Skill.MAGIC;
			default:
				return null;
		}
	}

	@Value
	static class FakeDrop
	{
		long spawnMillis;
		String text;
		Skill skill; // attack style at the time of the miss; may be null (cold start)
	}

	private static boolean isCombatSkill(Skill skill)
	{
		for (Skill s : COMBAT_SKILLS)
		{
			if (s == skill)
			{
				return true;
			}
		}
		return false;
	}
}
