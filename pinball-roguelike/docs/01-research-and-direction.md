# Research and design direction

Written after the first grey-box build. The grey box proved the physics
works on a phone. It did not have a concept. This document sets one.

## 1. Comparable games and what each teaches

| Game | What it is | Take | Avoid |
|------|-----------|------|-------|
| Sonic Spinball (1993) | Tall pinball levels, collect emeralds to open the boss room | Vertical levels where only the bottom is lethal. Objectives that gate progress | Reviews then and now call the level design confusing and tedious, the physics floaty. Players did not know where to go |
| Pinball Spire (2024) | "Pinballvania". A ball climbs a tower, three unlockable abilities powered by mana | Tower climb works as a pinball structure. Abilities cost a resource gathered by playing | Reviewers said having no game over removed the urgency of fighting gravity |
| Yoku's Island Express (2018) | Pinball metroidvania, open world | Barely any fail state: you lose some currency or return to the plunger. Each challenge and its solution fit on one screen | Slow backtracking between tables |
| Ball x Pit (2025) | Brick-breaker roguelite with a base | Runs under 20 minutes. Fusion (merge two balls, free a slot) and evolution (two balls make a new one) create over a thousand combinations from 17 base balls. Ending a run early still pays out | Reviewers found the base poorly introduced and the metagame slow |
| Peglin (2022) | Pachinko roguelike | Orbs plus relics. Map of nodes between fights | Passive: you watch the ball fall |
| Ballionaire (2024) | Pachinko board builder | Building the board is the build | Leans on chance. Enjoyment swings run to run |

Lessons that recur across roguelite design writing:

- Synergy matters more than item count. Items should combine into more than
  their sum.
- Balance combinations, not single items. Test pairs and triples.
- Give several routes to the same effect so a build does not depend on one
  lucky drop.
- Let the player feel overpowered late in a run. Scale enemy health, not
  just numbers.

## 2. Assessment of the proposed direction

| Idea | Confidence | Why |
|------|-----------|-----|
| Cyberpunk aesthetic | High | Neon on dark makes the ball and hazards readable. Pinball already speaks in lights and circuits. Free CC0 art exists |
| Climb the board, unlock a gate to reach the next part | Medium-high | Proven by Spinball, Pinball Spire and Yoku. Fails when the player cannot see the objective, so every gate needs its goal visible on screen |
| Fall all the way back to the start | Low | A 10-15 minute run cannot absorb a full reset. It is the most criticised feeling in Spinball. Yoku and Pinball Spire both softened it |
| Three power-up families: passive, flipper, ball | High | Three clear slots give builds an identity and keep choice screens readable. Matches the Ball x Pit lesson that a small set with combinations beats a big flat list |

### Options for the fall rule, ranked

1. **Fall one sector, land in a catch lane.** High. Each cleared gate is a
   checkpoint. A drain drops the ball to the sector below and costs one
   charge of shield. Zero shield at the bottom sector ends the run. Keeps
   gravity threatening without erasing progress.
2. **Fall to the start, but unlocked gates stay open and launch tubes carry
   you back up fast.** Medium. Closest to the original idea. Works only if
   the return trip takes under ten seconds.
3. **Fall to the start with everything reset.** Low. Fits a 45-minute
   console game, not a phone run.

## 3. Proposed structure

**Run.** Five sectors, about two and a half minutes each. A sector is one
and a half screens tall. Each has three objective nodes that are always
visible. Hit all three and the gate opens. Sector three has a mini-boss,
sector five the boss. After each gate, choose one of three upgrades.

**Power-ups.** Limited slots force choices: 3 ball mods, 2 flipper mods,
4 passives. Every item carries one or two tags so families combine.

Tags: Shock, Burn, Hack, Magnet, Split.

| Family | Changes | Examples |
|--------|---------|----------|
| Ball | What the ball is and does on contact | Arc Ball (Shock): chains to the nearest enemy. Splitter (Split): splits in two on bumper hits. Thermite Core (Burn): leaves a burning trail. Ghost Packet (Hack): passes through the first wall it touches |
| Flipper | What your input does | Overclock: a held flipper charges a stronger shot. Mag Grip (Magnet): hold to catch the ball on the flipper. Tesla Tips (Shock): a flip electrifies the ball for three seconds. Twin Link: an extra mini flipper higher up |
| Passive | Rules of the run | Insurance: first drain per sector is free. Daemon (Hack): objective nodes need one less hit. Feedback Loop: each bumper hit adds a little damage until a drain. Scrap Magnet (Magnet): pickups drift toward the ball |

Cross-family example: Tesla Tips plus Arc Ball plus a passive that makes
shocked enemies explode. Three slots, one build.

Borrow from Ball x Pit: two ball mods with a shared tag can fuse into one
slot, which frees a slot and makes a stronger named ball.

## 4. Story options, ranked

1. **The Climb.** High. You are a rogue courier drone carrying a stolen
   data core up a megacorp arcology, from the flooded undercity to the
   executive spire. Each sector is a district with its own security. A
   drain is a security purge flushing you down a level. The base is a
   hideout in the undercity where a crew of fixers install your upgrades.
   The fiction explains the climb, the gates, the fall and the base.
2. **The Machine.** Medium. You are a ball inside a sentient pinball
   cabinet in a derelict arcade, fighting its AI. Charming and cheap to
   draw, but it does not explain a base.
3. **The Net.** Medium. You are a packet of code climbing through network
   layers. Abstract art is the cheapest of all, but characters and a base
   are hard to care about.

Characters are crew members: each is a starting loadout that changes how a
shot feels, for example a heavy slow ball or short fast flippers.

## 5. Free cyberpunk art

Licences below come from search listings. itch.io and OpenGameArt could
not be opened from the build environment, so confirm the licence on each
page before shipping.

| Pack | Licence as listed | Use |
|------|-------------------|-----|
| Warped City, ansimuz | CC0, confirmed on itch.io and OpenGameArt listings. Music has a separate licence | Three-layer parallax city background, props, palette anchor |
| CHROME DISTRICT, booliebuilds | CC0 | 20 characters, top-down. Crew portraits and base screen |
| Cyber City, OpenGameArt | CC0. Contents not checked | Tiles |
| Cyber Inventory Mega Pack, OpenGameArt | CC0. Verify on page | 158 neon icons. Power-up icons |
| Purple City, Svor | Free for commercial use, no credit required. Custom licence | City tiles |
| Pixxxelpunkkk toolkit, venoxxx | CC BY 4.0, credit required | Robots, platformer parts |
| Cyberpunk Backgrounds Pixel Art, OpenGameArt | OGA-BY 3.0, credit required | Eight backgrounds at 576x324 |
| Warped City 2, ansimuz | Free. Licence not confirmed | More city tiles |
| Free packs from free-game-assets (CraftPix) | Licence not confirmed | Backgrounds, icons, townspeople |
| PixelEdge City free sample | Non-commercial only | Do not use |

Limits to be honest about:

- These packs are side-view platformer art. A pinball table is seen from
  above. They cover backgrounds, palette, icons and characters. Flippers,
  bumpers, rails and gates need original art.
- Free "concept art" with a usable licence barely exists. Use the packs as
  the style anchor and collect reference boards for mood only.

## 6. Next steps

1. Confirm the fall rule and the story.
2. Paper-design sector one: three nodes, one gate, one enemy type.
3. Download Warped City and Cyber Inventory, build a palette and a
   one-screen mock-up at 270x480.
4. Write the first 30 power-ups, ten per family, tagged.
5. Rebuild the prototype around one sector with a scrolling camera.
