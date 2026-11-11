# Table elements: how real ones work and how ours copy them

Written for version 0.5.0, when the Sump was rebuilt with real pinball
parts. Each section says what the real mechanism does, what we built, and
which test proves it.

## Proportions

- A standard flipper bat is about 3 inches long. A search confirmed only
  that bats range from 1.5 to 3 inches, so treat 3 inches as the common
  size rather than a checked figure.
- Our bats were 28 px on a 270 px table, about 10% of the width. They are
  now 38 px, about 14%, which matches a 3-inch bat on a standard-width
  playfield. Upper flippers are 24 px, a mini-flipper size.
- The ball grew from 8 px to 10 px across so it stays readable on a phone.

## Flippers

**Real.** A solenoid swings the bat up when the button is pressed. Where
the ball sits on the bat sets the shot. A ball near the tip is struck
early in the swing and goes across the table. A ball near the base rides
the bat through the stroke and goes up its own side.

**Ours.** The bat is a moving physics body plus one explicit kick per
press. Kick speed rises from 520 px/s at the base to 900 px/s at the tip.
Direction blends from 2 degrees toward its own side at the base to 30
degrees across at the tip. The kick zone reaches slightly behind the
pivot, so a ball resting where the inlane meets the bat still gets a full
shot.

**Fixed on the way.** The slingshot's lower corner hung over the bat's
base. Base shots were hitting its underside and dying. The corner was
pulled back.

**Test.** `elements_test`: tip shot goes across, base shot goes up its
own side. `flipper_power_test`: every point on the bat would climb 450 to
560 px.

## Pop bumpers

**Real.** The ball presses a skirt around the bumper. That closes a
switch, which fires a coil that pulls a metal ring down onto the ball and
throws it away. The ball leaves much faster than it arrived, even from a
gentle touch. Usually three sit together, fed by the top lanes.

**Ours.** The skirt is a zone 2 px wider than the bumper. Any ball inside
it is thrown straight away from the centre at 560 px/s, with up to 8
degrees of scatter. The coil then rests for 0.1 s. The ring visibly
shrinks and flashes when it fires. Before 0.5.0 bumpers only set the
ball's speed on contact, so they felt like a bounce.

**Test.** `elements_test`: a ball dropped gently onto a bumper is thrown
more than 60 px in 0.2 s.

## Slingshots

**Real.** Rubber-wrapped triangles above each flipper. A hit on the rubber
face fires a kicker arm that pushes the ball out sideways.

**Ours.** Only the long face kicks. It sets a fixed exit speed with
scatter, so two facing slingshots cannot trap the ball in an endless
volley. The base triangle behind is a plain wall.

## Inlanes, outlanes and their saves

**Real.** Beside each flipper a divider post splits the side of the table
into an inlane and an outlane. The inlane feeds the ball onto the
flipper. The outlane drains it. Tables protect outlanes in several ways:

- A **kickback** fires an outlane ball back into play once, then goes
  dark until relit. Terminator 2 has one in the left outlane, relit by
  shooting targets.
- A **diverting gate** is a slanted barrier that turns an outlane ball
  into the inlane beside it.
- A **ball saver** returns a ball that drains in the first seconds of
  play.
- Black Knight used a magnet save on both outlanes.

**Ours.** Both outlanes have a diverting gate. Each gate closes for 20
seconds on every launch and again when the drop target bank is finished,
then blinks for 4 seconds and opens. The left outlane also has a
kickback, lit at the start of a sector and relit by the drop bank. These
are the "traps either side that stay closed until a certain point, or can
be closed as a bonus".

**Tests.** `elements_test`: closed gate feeds the inlane, open gate
drains, lit kickback saves, dark kickback drains.

## Drop targets

**Real.** A bank of targets that fall below the playfield when hit. When
the whole bank is down, a coil resets it and the game usually awards
something.

**Ours.** Three targets on the left wall. All three down pays 10 scrap,
recloses both outlane gates, relights the kickback, and resets the bank
after one second.

**Test.** `elements_test`: bank pays, rearms the saves, and resets.

## Ramp and wire rail

**Real.** A ramp is an inclined plastic track. A shot without enough speed
rolls back down it. A shot that makes it reaches the top and drops onto a
wire rail, a habitrail, which carries the ball by gravity somewhere else,
often to an inlane or an upper flipper.

**Ours.** The ramp is a walled lane with extra gravity pulling the ball
back down. A weak shot rolls back out. A ball that reaches the top while
still climbing is caught by the rail. It is carried above the table with
collisions off and dropped next to the upper right flipper. The ramp is
the main way up to the top half of the Sump.

**Tests.** `elements_test`: a strong shot reaches the upper level, a weak
shot rolls back. `shot_map_test`: the left flipper can make it.

## Spinner

**Real.** A flat plate hung on a wire across a lane. A passing ball sets
it spinning, faster for a faster ball, and it scores on every turn until
friction stops it.

**Ours.** It sits across the gap between the upper flippers. Spin rate
comes from the ball's speed. It decays over time and pays 1 scrap per half
turn.

**Test.** `elements_test`: a fast pass pays several times.

## Saucer (kickout hole)

**Real.** A shallow cup that catches a slow ball, holds it while the game
scores or starts something, then kicks it out. A fast ball skips over.

**Ours.** Upper left, above the left ledge. It catches balls slower than
450 px/s, holds them 0.8 s, pays 5 scrap, and kicks them toward the upper
flippers.

**Test.** `elements_test`: holds, pays, kicks out. `shot_map_test`: the
upper left flipper can reach it.

## Upper flippers

**Real.** Many tables add a third or fourth flipper higher up. The Addams
Family has four flippers, and its left ramp can only be made from the
upper right flipper. Black Knight split the playfield into two levels.

**Ours.** The Sump is two levels joined by a ramp. The upper level has a
pair of mini flippers on the ledges, fed by the ramp rail and by the
plunger. They shoot at the bumpers, the saucer, valve 3 and the lift. The
spinner sits in the gap between them, so a ball falling back to the lower
level spins it.

## Layout references

- **The Addams Family (Pat Lawlor, 1992):** four flippers, a shot
  through the bumpers, a ramp made from an upper flipper. We copied the
  upper-flipper ramp idea: the ramp feeds an upper flipper, which makes
  the next shot.
- **Black Knight (1980):** the first split-level table, with ramps
  between levels and saves on both outlanes. We copied the two levels and
  the protected outlanes.
- **Terminator 2 (1991):** left outlane kickback relit by targets. We
  copied it exactly.
- **Medieval Madness and Attack From Mars:** two-flipper fan layouts with
  a centre toy. A model for later sectors, not this one.
- **Twilight Zone (1993):** two ramps, three pop bumpers, a small upper
  playfield reached by a ramp. A model for a later sector.

## Shot map

`shot_map_test` fires from ten points along each flipper and records what
each shot touches. Current reach:

| Target | Reached from |
|--------|--------------|
| Ramp | Left flipper |
| Drop bank | Right flipper |
| Valve 1 | Both lower flippers |
| Valve 2 | Left flipper |
| Valve 3 | Upper right flipper |
| Lift | Upper right flipper |
| Saucer | Upper left flipper |
| Pop bumpers | Both upper flippers |

Some targets can only be reached from one flipper. A second ramp on the
left, fed by the right flipper, would balance that. It is listed as a
next step in the handoff.

## Sources

- [Pop bumper rebuild guide, homepinballrepair.com](https://homepinballrepair.com/?p=4270)
- [Bumper removal and parts, flippers.be](https://www.flippers.be/basics/101_remove_bumpers.html)
- [Selby, Electromechanical Pinball Machines (JHU)](https://hemkerlab.jhu.edu/wp-content/uploads/2022/10/Selby_Electromechanical-Pinball-Machines-Dogbone.pdf)
- [Kickback, Pinball FX Wiki](https://pinballfx.fandom.com/wiki/Kickback)
- [Outlane protection, Pinball FX Wiki](https://pinballfx.fandom.com/wiki/Outlane_protection)
- [Pinball glossary, lysator.liu.se](https://www.lysator.liu.se/pinball/glossary/)
- [Pinball glossary, pinwiki.com](https://pinwiki.com/wiki/index.php/Glossary)
- [Helical monorail ramp patent US5358241](https://patents.google.com/patent/US5358241)
- [Habitrail exit speed discussion, Pinside](https://pinside.com/pinball/forum/topic/what-rampshabitrails-return-the-ball-at-full-speed)
- [The Addams Family (pinball), Wikipedia](https://en.wikipedia.com/wiki/Addams_Family_(pinball))
- [Twilight Zone (pinball), Wikipedia](https://Www.wikipedia.org/wiki/Twilight_Zone_(pinball))
- [Black Knight (pinball), Wikipedia](https://Www.wikipedia.org/wiki/Black_Knight_(pinball))
- [Terminator 2 pinball tutorial, Kineticist](https://www.kineticist.com/post/terminator-2-pinball-tutorial)
- [Flipper arm length range, designingidea.com](https://designingidea.com/pinball-machine-dimensions)
