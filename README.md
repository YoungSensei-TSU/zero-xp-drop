# 0 XP Drop

Shows a **+0 XP drop whenever your attack misses**, so every attack you make produces an XP drop - hit or miss.

## What it does

Normally an XP drop only appears when you hit. Hitting a 0 gives no XP, so nothing appears at all.

This plugin shows a `+0` drop on every attack that misses. It appears on the same tick a real drop would have,
so your drops keep a steady rhythm that matches your attack speed, no matter how often you miss.

- Works with ranged and melee.
- Shows the skill icon of your current attack style next to the `+0`, just like a real drop.
- Only triggers on your own attacks. Getting hit, blocking and non-combat animations don't cause a drop.
- By default, only triggers while you're attacking an NPC.

## Where it's useful

- **Flinching** - bosses like General Graardor at Bandos are commonly flinched by attacking from the altar door and
  timing each attack off your XP drops. A single missed attack normally leaves a gap in your drops and throws off
  your timing. With this plugin, every attack shows a drop, so you always know when your next one is ready.
- **Safespotting and tick-based kiting** - anywhere you step between attacks or time movement around your attack
  speed, every attack now gives you the same visual cue.
- **Low-accuracy combat** - on low-level accounts or against high-defence monsters where you miss often, you can
  still see every attack land in rhythm.
- **Learning attack speeds** - see the exact interval between your attacks with any weapon, including the ones
  that miss.

## Display options

- **Custom overlay** *(default)* - draws a literal `+0` next to your XP drops. The text, colour, size, icon,
  duration, drift direction/distance, fade and position can all be customised.
- **Native** - uses the game's own XP drop, so it looks exactly like a real one. The game can't show a 0, so this
  shows a small amount of your choice (default `1`) on a skill of your choice.

## Setup

1. Install **0 XP Drop** from the Plugin Hub.
2. Make sure in-game **XP drops are turned on** (the XP button next to the minimap). The `+0` appears next to
   them.
3. Attack something - misses now show a `+0`.

If the `+0` doesn't line up with your real drops, use the **X offset** / **Y offset** settings to move it.
