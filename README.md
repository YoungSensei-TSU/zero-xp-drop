# Sensei 0 XP Drop

Shows a **+0 XP drop whenever your attack misses**, so every attack you make produces an XP drop - hit or miss.

## Why?

A lot of PvM tricks are timed off XP drops. The drop appears on the same tick you attack, so it works as a
visual metronome for your attack speed.

The problem is misses. Hitting a 0 gives no XP, so **no drop appears** and you lose your rhythm at exactly the
wrong moment. The classic example is **flinching General Graardor** (ranging him from the altar door at Bandos):
one missed shot and you no longer know when your next attack is ready.

This plugin fills that gap. When you attack and gain no combat XP on that tick, it shows a drop anyway - on the
**same tick** a real drop would have appeared, so your timing stays consistent.

## Features

- **Same-tick drops** - XP is awarded on the tick you attack, not when the hitsplat lands a few ticks later. The
  miss drop fires on that attack tick too, so it lines up exactly with where a real drop would be.
- **Two display styles**
  - **Custom overlay** *(default)* - draws a literal `+0` next to your XP drops, with your current attack style's
    skill icon. Text, colour, size, drift direction/distance, duration and fade are all configurable.
  - **Native** - uses the game's own XP drop instead, so it looks exactly like a real one. The game can't display
    a 0, so this shows a small amount of your choice (default `1`) on a skill of your choice.
- **Works with ranged and melee** - the icon follows your selected attack style and updates the instant you
  switch.
- **Ignores block animations** - getting hit plays a block/defend animation. Those are filtered out, so taking
  damage never triggers a fake drop.
- **Only in combat** - by default, drops only fire while you're attacking an NPC, so skilling and other
  animations won't set it off.

## Setup

1. Install **Sensei 0 XP Drop** from the Plugin Hub.
2. Make sure in-game **XP drops are turned on** (click the XP button next to the minimap). The custom overlay
   positions itself next to the game's XP drop area, so it needs that to be visible.
3. Attack something. Misses now show a `+0`.

If the `+0` doesn't line up perfectly with your real drops (this depends on your in-game XP drop position and
size settings), adjust **X offset** / **Y offset** in the *Custom overlay* section.

## Settings

### Native drop
| Setting | What it does |
|---|---|
| Render mode | **Custom overlay** (default) draws a real `+0`. **Native** uses the game's own XP drop. |
| Drop skill | *Native only.* Which skill the fake drop appears on. Set it to what you're training. |
| Fake XP amount | *Native only.* The number shown. Must be at least 1, because the game can't show a 0. |

### Custom overlay
| Setting | What it does |
|---|---|
| Miss drop text | The text shown on a miss (default `0`). |
| Miss drop colour | Text colour, including transparency. |
| Drop duration (ms) | How long each drop stays on screen. |
| Drop drift | How far the drop moves while visible. Positive moves up, negative moves down. |
| Fade out | Gradually fade the drop out instead of it disappearing all at once. |
| X / Y offset | Nudge the drop to line it up with your real drops. |
| Show skill icon | Show your attack style's skill icon next to the number, like a real drop. |
| Text size / Icon size | Size of the text and the icon. |

### Detection
| Setting | What it does |
|---|---|
| Detect via animation | Treat your attack animation as an attack. This is the main, most reliable signal. |
| Detect via projectile | Also treat a projectile leaving you as an attack. A backup; off by default. |
| Require NPC target | Only show miss drops while you're interacting with an NPC. On by default. |
| Ignored animation IDs | Animations that should never count as an attack (see below). |
| Log timing to console | Writes attack/XP/hitsplat events to the client log, for troubleshooting. Leave it off normally. |

## Troubleshooting

**A drop appears when I didn't attack.**
Some non-combat actions have their own animation - for example, applying poison to Duke Sucellus to start the
fight. To stop one from triggering a drop:

1. Turn on **Log timing to console**.
2. Do the action.
3. Open your client log (`.runelite/logs/client.log`) and find the line `[Sensei0] ... ANIM <number>`.
4. Put that number in **Ignored animation IDs** (separate several with commas, e.g. `1234, 5678`).
5. Turn **Log timing to console** back off.

**No drop appears when I miss.**
- Check XP drops are enabled in-game (see *Setup*).
- If **Require NPC target** is on, you need to be attacking an NPC.
- Try turning on **Detect via projectile** as a backup signal.

**Nothing shows up for magic.**
Standard spells give casting XP even when they splash, so you already get a real drop and the plugin has nothing
to fill in.

## Development

This is a standard RuneLite Plugin Hub plugin. To run it in a development client:

```
./gradlew run
```

Then log in with a Jagex account (see the
[Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts)
wiki page) and enable the plugin from the sidebar.
