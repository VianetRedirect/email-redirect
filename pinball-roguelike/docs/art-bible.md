# Art bible

Phase 4. Decide the technical frame before drawing a single sprite.

## Technical frame

- Internal resolution: 270 x 480, integer scaled
- Orientation: portrait
- Safe zones: keep HUD 24 px from top and bottom edges for notches and gesture bars
- Texture filter: nearest, set in project settings
- Tool: Aseprite

## Palette

32 to 48 colours. Paste the swatch here and name each colour's job.

| Colour | Hex | Used for |
|--------|-----|----------|
| | | Ball highlight |
| | | Ball body |
| | | Flippers |
| | | Danger / enemies |
| | | Pick-ups |
| | | Table furniture |
| | | Background |

## Readability rules

1. The ball is the highest-contrast object on screen, always.
2. Enemies are brighter than table furniture.
3. Pick-ups glow or pulse.
4. Test every sprite at real phone size at arm's length before approving it.

## Production order

1. Ball and flippers
2. One table set: walls, bumpers, lanes
3. Three enemies
4. UI frame and font
5. Base screen (last, it changes most)

## Visual effects list

| Effect | Trigger | Rule |
|--------|---------|------|
| Hit spark | Any bumper or enemy hit | 4 frames, palette colour only |
| Ball trail | Speed above 60% of max | Fades over 6 frames |
| Screen shake | Enemy death, boss hit | Never more than 2 px |
| Damage number | Enemy hit | Rises 12 px and fades |

## Mood board

Link or paste three concept paintings of the table at different floors.
