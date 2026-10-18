# Development plan

Decisions made: Godot 4, pixel art at 270x480 internal resolution, portrait.

| Phase | Goal | Weeks | Gate |
|-------|------|-------|------|
| 0 | Pitch and pillars | 1 | A stranger asks a follow-up question after the pitch |
| 1 | Grey-box physics prototype | 2-3 | Ten minutes of fun with no art, on a real phone |
| 2 | Run loop design | 3-4 | Three floors and five pick-ups playable end to end |
| 3 | Meta and base design | 2 | Progression spreadsheet covers the first two hours |
| 4 | Graphics design | 4-6 | Stranger finds ball, flippers, enemy and pick-up in 5 s at phone size |
| 5 | Vertical slice | 4 | Runs average 10-15 min, testers start a second run unprompted |

Sound and the wider feature list start after Phase 5.

## Phase 1 checklist

- [x] Project skeleton, portrait, 120 Hz physics
- [x] Flippers, plunger, bumpers, slingshots, drain
- [x] Touch input: halves for flippers, hold to launch
- [x] Placeholder targets with hit points
- [ ] Test on a real Android device, measure flipper response
- [ ] Tune gravity, flipper speed and bounce until shots feel intentional
- [ ] Add outlanes and decide whether they are too punishing at this size
- [ ] Nudge via swipe, with a tilt penalty
- [ ] Playtest with two other people for ten minutes each
