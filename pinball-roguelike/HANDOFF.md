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

The owner is Ashley. They play builds on their Android phone and liked
Ball x Pit: short runs plus a base. Their pronouns have not been stated,
so use they/them.

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

A grey-box run loop. The Sump is the only sector, so it repeats as
sector 2, 3 and onward until Night Market exists. Six power-ups work. No
art, no sound, no hideout.

```
scenes/main/Main.tscn          run manager: Sector, Hud, UpgradeScreen
scenes/main/main.gd            lift -> pause and offer upgrades -> pick ->
                               restore 1 shield -> load next sector.
                               Run over -> tap restarts
scenes/sectors/Sump.tscn       sector one layout, 270x720, edit geometry here
scenes/sectors/sector.gd       ball, plunger, touch input, camera follow,
                               objective, drain rule, ball search, escape net
scenes/table/                  reusable pieces
  ball.gd / Ball.tscn          speed cap, hit reporting, Arc Ball, Splitter
  flipper.gd / Flipper.tscn    AnimatableBody2D plus explicit kick.
                               Overclock, Mag Grip
  drone.gd / Drone.tscn        patrol enemy, take_damage(), hit cooldown
  bumper, slingshot, valve, gate, lift, spinner, drain, arc_flash
  greybox_body.gd              draws collision shapes as flat colour
scenes/ui/Hud.tscn             shield, scrap, valves, sector, timer, items,
                               off-screen hint, toasts
scenes/ui/UpgradeScreen.tscn   three item cards, coloured by family
scenes/base/Base.tscn          empty placeholder for the hideout
scripts/autoload/events.gd     signal bus
scripts/autoload/game_state.gd shield, scrap, items, slots, depth, passives
scripts/items/item_data.gd     ItemData resource: id, family, tags, text
scripts/items/item_db.gd       list of every offerable item
items/*.tres                   one file per item
tests/                         headless tests (section 6)
tools/                         build scripts (section 5)
```

How the pieces talk:

- The ball calls `on_ball_hit(ball)` on anything it touches that has the
  method. New table objects only need that method.
- Enemies join the group `enemies` and expose `take_damage(amount) -> bool`.
  Anything that hurts enemies (Arc Ball, future Burn effects) calls that.
- Table objects emit on `Events`. `GameState` and the `Hud` listen. Nothing
  holds a reference to the HUD.
- Physics layers: 1 walls, flippers, gate. 2 ball. 4 hittable objects
  (bumpers, slingshots, valves, drones). The ball's mask is 1 + 4, so
  balls never hit each other.
- Groups: `objective_nodes`, `balls`, `enemies`, `bumpers`, `flippers`.

How items work:

- Data is a `.tres` per item in `items/`, listed in `scripts/items/item_db.gd`.
- Behaviour lives where the family hooks in: ball mods in `ball.gd`,
  flipper mods in `flipper.gd`, passives in `game_state.gd`. Code asks
  `GameState.has_item(&"id")`.
- Slots: 3 ball, 2 flipper, 4 passive. `GameState.roll_offers(3)` offers
  items the player can still take.
- Splitter clones have `is_clone = true`. They never cost shield, vanish
  in the lift, and expire after 6 seconds.
- Overclock lets that one shot exceed the normal speed cap (950) up to
  1150 for 0.6 seconds. No escapes seen in bot runs, but watch for it.
- Scrap Magnet is +25% only. The "scrap drifts to the ball" half of its
  design waits for scrap pickups, which do not exist yet.

## 5. Build and test in a cloud container

```
cd pinball-roguelike
tools/setup_build_env.sh     # once per container. Downloads about 1.3 GB
tools/build_apk.sh           # tests, then export/pinball-roguelike-debug.apk
```

Then send the APK to Ashley with the file-sending tool. They install it
by hand. It is arm64 only and signed with `tools/debug.keystore`, which is
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
godot --headless --fixed-fps 60 --path . -s res://tests/<name>.gd
```

| Test | What it proves | Last result |
|------|----------------|-------------|
| `flipper_power_test` | A flip from four points on the flipper would climb at least 430 px | 358 to 499 px, PASS |
| `mini_flipper_reach_test` | At least 3 of 12 flip timings on the mini flipper touch valve 3 | 5, PASS |
| `drone_top_test` | A ball landing on a drone costs it at most 1 hit point and slides off | 1 each, PASS. The old drone lost all 3 |
| `items_test` | Each item's effect, slot limits, offers, and the lift-to-next-sector flow | 19 of 19, PASS |
| `sump_bot_test` | A full run with a bot that flips blindly and picks random upgrades, up to 3 sectors | See below |

`sump_bot_test` prints sectors cleared and when, items picked, escapes,
drains, shield, clones seen and resting spots. It is not deterministic, so
run several in parallel. Last 12 runs: 2 cleared all 3 sectors, 8 cleared
2, 1 cleared 1, 1 died in sector one. 0 escapes, 0 resting spots, 0
errors. The bot cannot aim, so it is far slower than a person.

**Screenshots without a phone.** The container has a virtual display:

```
xvfb-run -a godot --rendering-driver opengl3 --path . -s res://tests/screenshot.gd -- OUT_DIR
```

It writes `play.png`, `top.png` and `upgrade.png` at 2x scale. Read them
to check layout and readability. Extend the script for new screens.

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
- **Moving bodies.** Do not set `sync_to_physics` on a body you move by
  writing `position` in `_physics_process`. The drone did this and its
  collision body desynced from its visible position, firing the ball
  through the walls at max speed in a third of bot runs. `sector.gd` now
  respawns any ball that leaves the sector, with no shield lost, and logs
  a warning. The flippers use `sync_to_physics` with `rotation` and are
  fine.
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
- **Pausing.** The upgrade screen pauses the tree. Timers that must
  freeze during it need `create_timer(t, false)`. UI that must work
  while paused needs `process_mode = ALWAYS`.
- **Swapping sectors.** `main.gd` removes the old sector from the tree
  before freeing it, so its signal handlers cannot double-count events
  from the new one. Tests must look the sector up by name each time,
  because the node changes.
- **Flippers start at angle zero** for the first tenth of a second.
  Cosmetic, unfixed.

## 8. What to build next, in order

1. **Sector two, Night Market, and the real fall rule.** A drain in
   sector two drops the ball into the Sump's catch lane, and the open
   lift becomes a launch tube back up (docs/02). `main.gd` already loads
   sectors by depth from `SECTOR_SCENES`. The drain rule is isolated in
   `sector.gd` `_on_ball_drained`. Night Market's enemy is gang turrets,
   its gate a mag-rail tube.
2. **More items.** Grow from 6 toward the 30 in docs/03, a few per
   family at a time, keeping every tag present in every family. Add
   fusions once two ball mods share a tag.
3. **Art pass on the Sump** once layout stops changing. Palette first.
   Warped City (CC0) is the style anchor. Table furniture needs original
   art because the free packs are side-view.
4. **Hideout.** Scrap carries home. Two buildings to start.

Sound stays out until Ashley asks for it.

## 8a. Session log

- 0.2.0: Sump grey box, flipper kick, handoff written.
- 0.3.0: Fixed ball escaping through walls (drone `sync_to_physics`).
  Mini flipper kick raised to 470/680. Valve 3 moved from (80, 100) to
  (188, 100), where late flips from the mini flipper land. Valves
  enlarged from radius 9 to 11. Added escape safety net and
  `mini_flipper_reach_test`.
- Feedback on 0.3.0 from Ashley's phone: main flippers reach the top with
  a good hit near the tip. The next valve is hard to see, but the arrow
  is fine for now. Drones knocking the ball down feels fair. A drone's
  top was "squishy": a ball landing on it took all 3 hits at once.
  One and a half screens per sector is enough for now.
- 0.4.0: Drone hit cooldown (0.35 s) and slide-off for balls landing on
  top. Upgrade screen and run manager: lift pauses and offers 3 items,
  pick restores 1 shield and loads the next sector (the Sump again).
  Six items: Arc Ball, Splitter, Overclock, Mag Grip, Insurance, Scrap
  Magnet. HUD shows sector, items and an Insurance toast. Unlit valves
  now pulse brightly and lit ones go dim. Added `drone_top_test`,
  `items_test`, `screenshot.gd`, and a multi-sector bot.

## 9. Working rules

- Work on branch `ccr-ededa791-96q9tf` unless told otherwise. Commit and
  push when a piece of work is done. Do not open a pull request unless
  asked.
- The game lives in `pinball-roguelike/` inside the `email-redirect`
  repository because that is where the first session ran. It should move
  to its own repository. Ashley has been told. Ask before moving it.
- Ashley's organisation requires this format for decisions: offer options,
  give each a confidence of high, medium or low, rank them, say plainly
  when their own idea is not high confidence and why, then do it their
  way once they decide.
- Their preferences mention about-me, writing-rules and memory files. They
  were not present in the cloud container. Say so once if they are still
  missing and carry on.
- Keep prose plain. Short sentences, no jargon they have not used.
- **Testing rule, set by Ashley.** Test headless by default: the test
  suite, bot runs and screenshots. Do not stop work to wait for phone
  feedback. When a question can only be answered by playing (feel,
  fairness, difficulty), ask Ashley to try it on a laptop: open the
  project in Godot 4.4.1 and press F5. Mouse clicks act as touches, and
  the keys are in README.md. Send an APK too when a build is worth
  trying on the phone.
- Test before sending a build. A build that fails on their phone costs
  them an install cycle.
