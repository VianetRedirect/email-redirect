# Pinball Roguelike

A pinball game with roguelike runs and a base you build between them.
Android first, portrait, pixel art. Built in Godot 4.4.

This is the Phase 1 grey-box skeleton. There is no art, no sound and no
progression yet. That is deliberate. The only job of this build is to answer
one question: is flipping a ball around this table fun for ten minutes?

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

```
project.godot            engine settings: resolution, physics rate, input map
scenes/main/             Main.tscn, the entry point. Later routes base <-> run
scenes/table/            everything on the play field
  Table.tscn             the grey-box table layout. Edit walls and positions here
  table.gd               plunger, touch input, ball spawning, drain handling
  Ball.tscn / ball.gd    the ball. Speed cap and hit reporting live here
  Flipper.tscn           one flipper, mirrored for the right side
  Bumper.tscn            pop bumper
  Slingshot.tscn         slingshot
  Target.tscn            placeholder enemy with hit points
  drain.gd               area under the flippers that eats the ball
  greybox_body.gd        draws collision shapes as flat colour so no art is needed
scenes/ui/               Hud.tscn: score, balls, run timer, run-over message
scenes/base/             Base.tscn placeholder for Phase 3
scripts/autoload/        Events (signal bus) and GameState (run state)
assets/                  empty for now. sprites, fonts, sfx, music
docs/                    design docs. Start with pitch-and-pillars.md
```

## How the pieces talk to each other

- Nothing holds a reference to the HUD or to GameState from the table.
  Table objects emit on `Events`, GameState listens and updates, the HUD
  listens to GameState.
- The ball calls `on_ball_hit(ball)` on anything it touches that has that
  method. Bumpers, slingshots and targets implement it. New table objects
  only need that one method.
- Physics layers: 1 is walls and flippers, 2 is the ball, 4 is hittable
  objects. The ball collides with 1 and 4.

## Tuning knobs

All in the inspector, no code needed:

- `project.godot`: gravity (420) and physics ticks (120). Keep ticks at 120
  or higher or the ball tunnels through flippers at speed.
- Ball: `max_speed`, `radius`, bounce and friction on the physics material.
- Flipper: `rest_degrees`, `active_degrees`, `angular_speed`, `length`.
- Table: `launch_min`, `launch_max`, `charge_time`.
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

`tools/export_android.gd` exports an unsigned APK headless using only the
export templates, then you sign it yourself:

```
godot --headless --editor -s res://tools/export_android.gd --path .
java -jar uber-apk-signer.jar --apks export/pinball-roguelike-unsigned.apk \
  --ks debug.keystore --ksAlias androiddebugkey --ksPass android --ksKeyPass android \
  --out export/
```

The APK is arm64 only. Install it with "install from unknown sources" enabled.

## Phase 1 exit gate

You and two other people play this grey box for ten minutes and want another
go. If that fails, change the table, not the art plan.

## Known rough edges

- Flippers settle to their rest angle in the first tenth of a second rather
  than starting there.
- No tilt detection, so nudging is free.
- The ball can rest in the plunger lane after a failed launch, which is
  correct pinball but has no visual cue yet.
