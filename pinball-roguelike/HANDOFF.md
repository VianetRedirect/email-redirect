# Handoff: Pinball Roguelike

Read this first, then the docs it points to. It tells you what the game
is, what has been decided, what exists, how to build and test it, and
what to build next.

## 1. The project in five lines

- Android pinball roguelike, portrait, 270x480 internal resolution, Godot 4.4.1, GDScript.
- Cyberpunk. Story: "The Climb". You are KITE, a courier drone carrying a stolen data core up the Halcyon arcology.
- A run is five vertical sectors of about 2.5 minutes each. Light three nodes to open a gate, take the gate to the next sector.
- A drain costs one shield and drops you one sector. A drain at zero shield ends the run.
- Three power-up families: ball (3 slots), flipper (2 slots), passive (4 slots). A hideout between runs spends scrap.

The owner is Ashley. He plays builds on his own Android phone and gives
feedback from that. He liked Ball x Pit: short runs plus a base.

## 2. Decisions that are locked

Do not reopen these without being asked.

| Decision | Choice |
|----------|--------|
| Engine | Godot 4.4.1, GDScript, GL Compatibility renderer |
| Screen | Portrait, 270x480, integer scaled, nearest filtering |
| Art | Pixel art, cyberpunk. Grey box until the sector plays well |
| Story | The Climb (docs/02) |
| Fall rule | Fall one sector, lose one shield. Gates stay open. Launch tube back up |
| Power-ups | Ball, flipper, passive. Tags: Shock, Burn, Hack, Magnet, Split (docs/03) |
| Order of work | Game and graphics design before sound and wider features |

## 3. Documents, in reading order

| File | What it holds |
|------|---------------|
| docs/01-research-and-direction.md | Comparable games, why each decision was made, free asset packs and licences |
| docs/02-run-rules-and-sector-one.md | Run rules, the five sectors, paper layout of the Sump, tuning targets |
| docs/03-power-ups.md | First 30 power-ups, fusions, example builds, balance rules |
| docs/00-plan.md | The original phase plan. Phases still apply, dates do not |
| README.md | Controls, folder map, tuning knobs |
| docs/pitch-and-pillars.md, art-bible.md, run-loop.md, playtest-log.md | Empty templates. Fill in as work reaches them |

## 4. What exists in code

A grey-box build of sector one, the Sump. No art, no sound, no power-ups,
no hideout.

```
scenes/main/Main.tscn          entry point: one Sector plus the Hud
scenes/sectors/Sump.tscn       sector one layout, 270x720, edit geometry here
scenes/sectors/sector.gd       ball, plunger, touch input, camera follow,
                               objective, drain rule, ball search
scenes/table/                  reusable pieces
  ball.gd / Ball.tscn          speed cap, reports hits via on_ball_hit(ball)
  flipper.gd / Flipper.tscn    AnimatableBody2D plus an explicit kick
  bumper, slingshot, valve, drone, gate, lift, spinner, drain
  greybox_body.gd              draws collision shapes as flat colour
scenes/ui/Hud.tscn             shield, scrap, valves, timer, off-screen hint
scenes/base/Base.tscn          empty placeholder for the hideout
scripts/autoload/events.gd     signal bus
scripts/autoload/game_state.gd shield, scrap, nodes lit, timer
tests/                         headless tests (section 6)
tools/                         build scripts (section 5)
```

How the pieces talk:

- The ball calls `on_ball_hit(ball)` on anything it touches that has the
  method. New table objects only need that method.
- Table objects emit on `Events`. `GameState` and the `Hud` listen. Nothing
  holds a reference to the HUD.
- Physics layers: 1 walls, flippers, gate. 2 ball. 4 hittable objects
  (bumpers, slingshots, valves, drones). The ball's mask is 1 + 4.
- Objective nodes join the group `objective_nodes`. Balls join `balls`.

What works, verified by the tests: launch, lane exit, both main flippers
and the mini flipper, three valves, gate opening, lift ending the run as a
win, drains costing shield, run over at zero shield, camera follow, HUD.

## 5. Build and test in a cloud container

```
cd pinball-roguelike
tools/setup_build_env.sh     # once per container. Downloads about 1.3 GB
tools/build_apk.sh           # tests, then export/pinball-roguelike-debug.apk
```

Then send the APK to Ashley with the file-sending tool. He installs it by
hand. It is arm64 only and signed with `tools/debug.keystore`, which is
committed on purpose so each build installs over the last one.

`build_apk.sh` has been run end to end. `setup_build_env.sh` was written
from the commands that worked but has not been run on a clean container.
If a step fails, the commands inside it are the ones that worked by hand.

Network facts for the cloud container:

- Reachable: github.com release downloads.
- Blocked: dl.google.com (Android SDK), api.adoptium.net, itch.io,
  opengameart.org. Asset packs cannot be downloaded from here. Ask Ashley
  to download them and add them to the repo.

## 6. Tests

```
godot --headless --path . --import      # always first, see gotchas
godot --headless --fixed-fps 60 --path . -s res://tests/flipper_power_test.gd
godot --headless --fixed-fps 60 --path . -s res://tests/sump_bot_test.gd
```

- `flipper_power_test` rests a ball at four points on the left flipper,
  flips, and converts launch speed to the height it would climb. It must
  be at least 430 px. Current result: 358 to 499 px, PASS. The first build
  managed 148 px, which is the "flippers are weak" complaint.
- `sump_bot_test` plays the sector with a bot that flips whenever the ball
  is over a flipper. It prints when each valve was lit, whether the lift
  was reached, drains and scrap. It is a smoke test. The bot is bad at
  aiming, so it clears the sector in roughly one run in four within ten
  simulated minutes. Set `TRACE` to true in the file to print ball
  positions and find jams.

## 7. Gotchas already paid for

- **Class cache.** `Ball` is a global class name. Scripts that mention it
  fail to parse until the project has been imported once. Run `--import`
  before any `-s` script on a fresh checkout.
- **Autoloads in tests.** Scripts run with `-s` cannot use `Events` or
  `GameState` as identifiers. Fetch them with `root.get_node("Events")`.
  Nodes added in `_initialize` have not had `_ready` called yet, so act in
  `_process`.
- **Android export.** `--export-debug "Android"` fails with an empty
  "configuration errors" message in this environment. Use
  `tools/export_android.gd`, which calls the export platform directly.
- **Flipper strength.** An AnimatableBody2D flipper only passes on its
  surface speed, which is weak. `flipper.gd` adds one explicit kick per
  press along the flipper normal, scaled from `kick_base` to `kick_tip`.
  Tune those two numbers, not `angular_speed`.
- **Flat surfaces trap the ball.** Every ledge and wall top must slope.
  The ball once sat forever on a 4 px flat divider top. `sector.gd` now
  also has a ball search that kicks a ball that is still for 4 seconds.
- **Wall meets flipper.** Inlane guides must end at the flipper's top rear
  edge, 6 px above the pivot, or the ball wedges there.
- **Slingshots.** A kick that adds an impulse makes two facing slingshots
  volley forever. They set a fixed exit speed with angular scatter, and
  only a thin strip on the upper part of each slingshot kicks.
- **Launch strength.** The ball needs about 600 px/s to clear the lane.
  If gravity changes, retune `launch_min` and `launch_max`.
- **Flippers start at angle zero** for the first tenth of a second.
  Cosmetic, unfixed.

## 8. What to build next, in order

1. **Get Ashley's phone feedback on this build** before adding anything.
   Ask about flipper strength, whether the climb reads, and whether drones
   deflecting the ball feels fair. The three open questions are at the
   end of docs/02.
2. **Find resting spots.** In bot runs the ball sits idle for long
   stretches until ball search frees it. Turn on `TRACE`, find where, and
   slope or reshape those surfaces.
3. **Upgrade screen.** On reaching the lift, pause and offer one of three
   power-ups. Start with six items, two per family, chosen from docs/03 to
   be as different as possible: Arc Ball, Splitter, Overclock, Mag Grip,
   Insurance, Scrap Magnet. This needs an item resource type, slot limits
   in GameState, and hooks in ball.gd and flipper.gd.
4. **Sector two, Night Market**, and the real fall rule: a drain in sector
   two drops the ball into the Sump's catch lane, and the open lift
   becomes a launch tube back up. `sector.gd` already isolates the drain
   rule in `_on_ball_drained`. Main must become a run manager that owns
   several sectors.
5. **Art pass on the Sump** once layout stops changing. Palette first.
   Warped City (CC0) is the style anchor. Table furniture needs original
   art because the free packs are side-view.
6. **Hideout.** Scrap carries home. Two buildings to start.

Sound stays out until Ashley asks for it.

## 9. Working rules

- Work on branch `ccr-ededa791-96q9tf` unless told otherwise. Commit and
  push when a piece of work is done. Do not open a pull request unless
  asked.
- The game lives in `pinball-roguelike/` inside the `email-redirect`
  repository because that is where the first session ran. It should move
  to its own repository. Ashley has been told. Ask before moving it.
- Ashley's organisation requires this format for decisions: offer options,
  give each a confidence of high, medium or low, rank them, say plainly
  when his own idea is not high confidence and why, then do it his way
  once he decides.
- His preferences mention about-me, writing-rules and memory files. They
  were not present in the cloud container. Say so once if they are still
  missing and carry on.
- Keep prose plain. Short sentences, no jargon he has not used.
- Test before sending a build. A build that fails on his phone costs him
  an install cycle.
