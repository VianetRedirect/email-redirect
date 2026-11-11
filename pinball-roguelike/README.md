# Pinball Roguelike

A pinball game with roguelike runs and a base you build between them.
Android first, portrait, pixel art. Built in Godot 4.4.

This is a grey-box build of the run loop. The Sump is the only sector, so
it repeats until Night Market exists. Light three valves, open the lift,
pick one of three power-ups, climb again. Six power-ups work. The Sump
uses real pinball parts: ramp and wire rail, outlane gates, kickback,
drop targets, pop bumpers, saucer, spinner and upper flippers
(docs/04-table-elements.md). There is no art or sound yet. That is
deliberate.

New to the project? Read `HANDOFF.md` first.

## Opening the project

1. Install Godot 4.4 or newer (standard build, not .NET) from godotengine.org.
2. Open Godot, click Import, pick `project.godot` in this folder.
3. Press F5 to run. The window opens at 2x scale of the 270x480 internal
   resolution.

## Controls

Desktop, for quick testing:

| Action        | Keys                 |
|---------------|----------------------|
| Left flipper  | Left arrow or Z      |
| Right flipper | Right arrow or /     |
| Launch        | Space or Down, hold to charge |
| Nudge         | Up arrow             |

Touch, which is what the game is designed for:

- Left half of the screen is the left flipper, right half is the right.
- While the ball is in the plunger lane, touch anywhere and hold to charge,
  release to launch.
- Mouse clicks are turned into touches, so touch controls also work on desktop.

## Folder map

See `HANDOFF.md` section 4 for the full map and how the pieces talk.

```
scenes/sectors/Sump.tscn   sector one layout. Edit geometry here
scenes/sectors/sector.gd   ball, plunger, input, camera, objective, drain rule
scenes/table/              flipper, ball, bumper, slingshot, valve, drone, gate, lift
scenes/ui/Hud.tscn         shield, scrap, valves, timer
scripts/autoload/          Events (signal bus) and GameState (run state)
tests/                     headless tests
tools/                     build scripts
docs/                      design documents
```

## Tuning knobs

All in the inspector, no code needed:

- `project.godot`: gravity (420) and physics ticks (120). Keep ticks at 120
  or higher or the ball tunnels through flippers at speed.
- Ball: `max_speed`, `radius`, bounce and friction on the physics material.
- Flipper: `kick_base` and `kick_tip` set shot strength. Also `rest_degrees`,
  `active_degrees`, `angular_speed`, `length`.
- Sector: `launch_min`, `launch_max`, `charge_time`, `camera_follow`,
  `stuck_seconds`.
- Drone: `speed`, `patrol_half_width`, `max_hp`, `deflect_speed`.
- Bumper: `kick_speed`. Slingshot: `kick_speed`, `normal` and `scatter_degrees`.

## Android export

1. Editor > Manage Export Templates, download the templates for your version.
2. Install Android Studio or the command line SDK, set the path in
   Editor > Editor Settings > Export > Android.
3. Project > Export > Add > Android. Set the package name, keystore and
   orientation (portrait is already set in project settings).
4. Export a debug APK, install it with `adb install`, and test on the real
   device. Flipper latency and launch feel can only be judged on hardware.

### Building without a full Android SDK

```
tools/setup_build_env.sh   # once: Godot, export templates, signer
tools/build_apk.sh         # tests, then export/pinball-roguelike-debug.apk
```

The APK is arm64 only and signed with the debug key in `tools/`. Install
it with "install from unknown sources" enabled.

## Phase 1 exit gate

You and two other people play this grey box for ten minutes and want another
go. If that fails, change the table, not the art plan.

## Known rough edges

- Flippers settle to their rest angle in the first tenth of a second rather
  than starting there.
- No tilt detection, so nudging is free.
- The ball can rest in the plunger lane after a failed launch, which is
  correct pinball but has no visual cue yet.
