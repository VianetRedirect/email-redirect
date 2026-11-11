# Run rules and sector one

Decisions locked: cyberpunk, The Climb story, fall one sector.

## The Climb in one paragraph

You are KITE, a courier drone carrying a stolen data core up the Halcyon
arcology, from the flooded undercity to the executive spire. Halcyon
security wants the core back. Your crew of fixers waits in a hideout in the
Sump and rebuilds you after every failed climb.

## The five sectors

| # | Sector | Look | Security | Gate |
|---|--------|------|----------|------|
| 1 | The Sump | Flooded pipes, green sodium light, rust | Scrap drones | Freight lift |
| 2 | Night Market | Neon signs, stalls, pink and cyan | Gang turrets | Mag-rail tube |
| 3 | The Foundry | Molten orange, conveyor belts | Welder bots. Mini-boss: the Press | Blast door |
| 4 | Server Stack | Cold blue, glass, data streams | ICE barriers, hunter daemons | Firewall |
| 5 | The Spire | White and gold, clean, quiet | Executive guard. Boss: the Warden | Core vault |

Each sector is 270 px wide and 720 px tall, one and a half screens. The
camera follows the ball vertically and never scrolls sideways.

## Run rules

- **Shield.** You start with 3 shield charges. The HUD shows them as pips.
- **Drain.** A ball lost through a sector's flippers costs 1 shield and
  drops into the sector below, arriving in its catch lane near the top
  flippers. Momentum is reset so the landing is always controllable.
- **Sector one drain.** There is nothing below the Sump. A drain there
  costs 1 shield and relaunches from the plunger.
- **Run over.** Shield at zero and a drain. Not before.
- **Gates stay open.** A gate opened this run never closes. After a fall,
  the open gate below has a launch tube that returns the ball to the sector
  above in one shot. Target: back in the fight within 8 seconds.
- **Objective.** Every sector has 3 nodes. They are always visible when the
  camera is in that sector and are marked on the HUD. All 3 lit opens the
  gate.
- **Reward.** Passing a gate for the first time opens the upgrade screen:
  pick 1 of 3. One shield charge is restored, up to the maximum.
- **Scrap.** Enemies and bumpers drop scrap. Scrap carries home whether
  the run ends in a win or a loss, and pays for hideout upgrades.
- **Target length.** 2.5 minutes per sector, 12 to 13 minutes for a full
  climb.

## Sector one: The Sump

Teaching sector. One idea at a time, bottom to top. Coordinates are in
pixels, origin at the sector's top-left, y grows downward.

```
y=0    +-------------------------------+
       |        [ FREIGHT LIFT ]       |  gate, opens when 3 valves lit
 80    |   \                       /   |  funnel walls into the lift
       |                     (V3)      |  valve 3, behind a drone patrol
160    |  o       <~ drone ~>       o  |  two pop bumpers, one patrol drone
       |                               |
240    |=====\                 /=======|  upper ledges, mini flipper on left
       |      \_ mini flipper          |
320    |                  ||  spinner  |  spinner lane on the right
       |  (V1)            ||           |  valve 1, open shot from left flipper
400    |                               |
       |   o     <~ drone ~>     (V2)  |  bumper, drone, valve 2 on the right
480    |                               |
       |  /|                     |\    |  slingshots
560    | / |                     | \   |
       |/  \___              ___/   \| |  inlane guides, plunger lane far right
640    |      ===\        /===       | |  main flippers
       |           drain             |P|
720    +-------------------------------+
```

**Elements**

| Element | Count | Behaviour |
|---------|-------|-----------|
| Main flippers | 2 | At y=640. Same feel as the grey box |
| Mini flipper | 1 | Left ledge at y=260. Fires with the left side of the screen. Needed to reach valve 3 and the lift |
| Valves (nodes) | 3 | Lit by one hit each. V1 is a direct shot. V2 needs a bank off the right slingshot or a right-flipper cross shot. V3 sits behind the upper drone, right of centre, where a late flip from the mini flipper lands |
| Pop bumpers | 3 | Kick the ball, drop 1 scrap per hit |
| Spinner lane | 1 | Right side. A clean shot up it feeds the upper area and awards scrap per spin |
| Scrap drones | 2 | Patrol left to right. 3 hit points. Touching one does not hurt you, it deflects the ball downward. Killing one drops 5 scrap |
| Freight lift | 1 | Closed: a wall with three lamps. Open: a scoop that carries the ball to sector two |
| Plunger lane | 1 | Far right, full height of the lower screen |

**What the sector teaches, in order**

1. Launch and flip. Valve 1 is in the natural path of a left-flipper shot.
2. Aim. Valve 2 needs a deliberate shot.
3. Enemies get in the way. The lower drone blocks lazy shots at valve 2.
4. The climb. The spinner lane or the left ledge is the way up.
5. A second flipper. Valve 3 and the lift are only reachable from the mini
   flipper.

**Tuning targets**

- A new player opens the lift in under 4 minutes.
- A practised player does it in 90 seconds.
- No shot requires more than two bounces to be makeable.
- From any resting position the player can see at least one unlit valve or
  an arrow pointing to it.

## Open questions for the prototype

- Does a 720 px sector feel like a climb, or does it need 960?
- Is a downward deflection from drones annoying or fair?
- Should the upgrade screen pause the ball mid-lift or wait for it to land?
