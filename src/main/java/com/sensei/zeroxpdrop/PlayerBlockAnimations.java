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

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.runelite.api.gameval.AnimationID;

/**
 * The player's block/defend animation IDs, keyed by melee weapon category (the
 * same category system used for attack styles - {@code VarbitID.COMBAT_WEAPON_CATEGORY}).
 * There is one shared block pose per category, not per individual weapon, and the
 * set below is small and finite because OSRS only has a limited number of weapon
 * categories.
 * <p>
 * Ranged and magic weapons have no dedicated block pose of their own - bows,
 * crossbows, thrown weapons, and casting all reuse {@link AnimationID#HUMAN_UNARMEDBLOCK}
 * / {@link AnimationID#HUMAN_UNARMED_DEF} (staves reuse the staff block instead, since
 * the staff is still held). So this same set also covers ranged and magic combat.
 * <p>
 * Any animation NOT in this set, played while not idle, is treated as an attack -
 * this needs no per-weapon learning and has no cold start, unlike inferring "attack"
 * from xp gain or hitsplat timing.
 */
final class PlayerBlockAnimations
{
	static final Set<Integer> IDS = ImmutableSet.<Integer>builder()
		.add(AnimationID.HUMAN_DDAGGER_BLOCK)
		.add(AnimationID.HUMAN_DDAGGER_DEF)
		.add(AnimationID.HUMAN_DSPEAR_BLOCK)
		.add(AnimationID.HUMAN_DSPEAR_DEF)
		.add(AnimationID.HUMAN_SWORD_BLOCK)
		.add(AnimationID.HUMAN_SWORD_DEF)
		.add(AnimationID.HUMAN_SWORD_TRANSDEF)
		.add(AnimationID.HUMAN_AXE_BLOCK)
		.add(AnimationID.HUMAN_AXE_DEF)
		.add(AnimationID.HUMAN_TRANS_AXE_DEF)
		.add(AnimationID.HUMAN_BLUNT_BLOCK)
		.add(AnimationID.HUMAN_BLUNT_DEF)
		.add(AnimationID.HUMAN_DHSWORD_BLOCK)
		.add(AnimationID.HUMAN_DHSWORD_DEF)
		.add(AnimationID.HUMAN_STAFF_BLOCK)
		.add(AnimationID.HUMAN_STAFF_DEF)
		.add(AnimationID.HUMAN_STAFFORB_BLOCK)
		.add(AnimationID.HUMAN_STAFFORB_DEF)
		.add(AnimationID.HUMAN_UNARMEDBLOCK)
		.add(AnimationID.HUMAN_UNARMED_DEF)
		.add(AnimationID.HUMAN_UNARMED_DEF_LONG)
		.add(AnimationID.HUMAN_SPEAR_BLOCK)
		.add(AnimationID.HUMAN_SPEAR_DEF)
		.add(AnimationID.HUMAN_SCYTHE_BLOCK)
		.add(AnimationID.HUMAN_SCYTHE_DEF)
		.add(AnimationID.HUMAN_FARMERSFORK_DEF)
		.add(AnimationID.HUMAN_BANNER_BLOCK)
		.add(AnimationID.HUMAN_ZAMORAKSPEAR_BLOCK)
		.add(AnimationID.HUMAN_BOXING_BLOCK)
		.add(AnimationID.HUMAN_DINHS_BULWARK_BLOCK)
		.add(AnimationID.HUMAN_ELDER_MAUL_BLOCK)
		.add(AnimationID.HUMAN_2H_AXE_DEFEND)
		.add(AnimationID.HUMAN_CHINCHOMPA_DEFEND)
		// Alternate variants that play with certain leather-armour graphics.
		.add(AnimationID.HUMAN_SWORD_DEF_LEATHER_HIT_3)
		.add(AnimationID.HUMAN_STAFF_BLOCK_LEATHER_HIT_3)
		.add(AnimationID.HUMAN_UNARMEDBLOCK_LEATHER_HIT_3)
		.add(AnimationID.HUMAN_SPEAR_BLOCK_LEATHER_HIT_3)
		.build();

	static boolean isBlock(int animationId)
	{
		return IDS.contains(animationId);
	}

	private PlayerBlockAnimations()
	{
	}
}
