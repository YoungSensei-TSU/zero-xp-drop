# Sensei 0 XP Drop

Shows a +0 XP drop on the tick you attack and miss. A miss normally gives no XP
drop at all, which breaks xp-drop-based timing (e.g. ranging the Bandos altar
door flinch). This fills the gap so every attack produces a drop.

Two render modes:

- **Native** (default) - fires the game's own xp drop. The game can't draw a 0, so
  it shows a configurable small amount on a configurable skill.
- **Custom overlay** - draws a literal "+0" (text, colour, size, drift, fade and
  skill icon are all configurable) next to the XP drop area.

Attacks are detected from your attack animation (block/defend animations are
filtered out), with projectile detection as an optional backup. Non-combat
animations that get mistaken for attacks can be added to "Ignored animation IDs".

## Development

This is a standard RuneLite Plugin Hub plugin. To run it in a development client:

```
./gradlew run
```

Then log in with a Jagex account (see the
[Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts)
wiki page) and enable the plugin from the sidebar.
