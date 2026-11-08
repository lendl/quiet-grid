# Arrow Escape Silhouette Guidelines

Rules for adding shapes to `cli/src/main/resources/arrowescape_silhouettes.json`. The offline generator uses these shapes to build Arrow Escape boards in the outline of an object, such as a heart, a cat or the app logo. Read this before you design, change or review a shape.

## Purpose

A shaped board is only worth having if a player can tell what it is from the outline alone. That must hold on a small Easy board and on a large Expert board alike. A shape must also be big enough to fill every difficulty tier.

## Content rules

- **No brands, logos or characters owned by someone else.** That means no mascots, app icons, franchise characters or trademarked products. Our own app logo (`logo`) is the only logo allowed.
- **No weapons, violence or adult themes.** Use the same test as the word lists: would a primary school teacher put it on a classroom worksheet?
- **No political, national or religious symbols.** That means no flags, crosses, crescent-and-star, party symbols or hand gestures.
- **Generic objects only.** A "rocket" is fine. A specific real rocket model is not.
- **The outline alone must identify the object.** The board has no colours and no inner lines. Eyes, stripes, windows and text never show up reliably.

## Game rule the generator must respect

An arrow's head may never point into its own body. A snake can't slide through itself, so such an arrow could never leave the board. The engine treats it as permanently blocked, the solver rejects the puzzle, and `ArrowEscapeBankVerificationTest` fails on it.

## File format

The file is a JSON array. Each entry looks like this:

```json
{
  "id": "heart",
  "name": "Heart",
  "width": 100,
  "height": 90,
  "minRows": 8,
  "parts": 1,
  "shapes": [
    { "type": "circle", "x": 27, "y": 27, "r": 25 },
    { "type": "circle", "x": 73, "y": 27, "r": 25 },
    { "type": "polygon", "points": [[3, 35], [97, 35], [50, 88]] }
  ]
}
```

| Field | Meaning |
|---|---|
| `id` | Unique lowercase ASCII id. Puzzle ids use it (`ae-<id>-<rows>x<cols>-...`), and the CLI `--shape` flag selects by it. Never rename an id once it has puzzles in the bank. |
| `name` | English display name. It is not shown in the app yet. |
| `width`, `height` | The size of the drawing box in your own units. Only the ratio matters. A board has `round(rows * width / height)` columns. |
| `minRows` | The smallest board height at which the shape still reads. Boards grow from here until they reach the tier's cell target. |
| `parts` | Optional, default 1. Set it only for a shape made of separate pieces on purpose, such as the 4 squares of `logo`. |
| `season` | Optional. Marks a seasonal shape, such as `"halloween"`. See [Seasonal shapes](#seasonal-shapes). |
| `shapes` | The drawing, listed in order. See below. |

Coordinates run from `(0, 0)` at the top-left to `(width, height)` at the bottom-right. `y` grows downward.

### Shape types

| `type` | Fields | Notes |
|---|---|---|
| `circle` | `x`, `y`, `r` | |
| `ellipse` | `x`, `y`, `rx`, `ry` | Axis-aligned only. |
| `capsule` | `points`, `width` | A thick line through the points with round ends. Good for legs, tails, stems and handles. A single point gives a dot. |
| `polygon` | `points` | Even-odd fill. Keep it simple and non-self-intersecting. For rounded corners, add arc points yourself (see `logo`). |

Any shape can take `"cut": true`.

### Drawing order

The shapes are applied in order:
- A normal shape adds its area.
- A cut shape removes its area from what was drawn **before** it.
- A normal shape listed after a cut adds its area back.

So put cuts after the body they carve, and put parts that must stay solid (knobs, handles) after the cuts.

## How a shape becomes a board

Knowing these steps explains most design rules below. The code is in `cli/src/main/kotlin/com/quietgrid/cli/arrowescape/ArrowEscapeSilhouette.kt`.

1. **Sampling.** Each cell is sampled at 4x4 points. The cell is active when more than half the samples fall inside the shape.
2. **Pinhole fill.** An empty pocket of 1-2 cells that doesn't touch the board edge gets filled.
3. **Speck removal.** Any group of cells smaller than `max(4, 2% of active cells)` is removed. So is any cell with no active neighbour.
4. **Growth.** The board starts at `minRows` and grows one row at a time until it reaches the tier's cell target. It stops at the tier's size cap (`ARROW_ESCAPE_SILHOUETTE_BOARD_LIMIT_BY_DIFFICULTY`), whichever comes first.

| Tier | Target active cells | Size cap |
|---|---|---|
| Easy | 100-144 | 60 rows x 45 columns |
| Medium | 324-900 | 60 rows x 45 columns |
| Hard | 961-1369 | 60 rows x 45 columns |
| Expert | 1500-2100 | 80 rows x 60 columns |

If the cap stops the growth before the target, the board stays smaller than its tier asks for. Avoid shapes that do this, because their boards end up smaller than the rest of the tier. A square shape tops out at about 1750 cells at the 60x60 Expert cap; only tall shapes reach the top of the Expert range.

Every Hard puzzle must hold at least 100 arrows (`ARROW_ESCAPE_SILHOUETTE_HARD_MIN_PIECES`) and every Expert puzzle at least 140 (`ARROW_ESCAPE_SILHOUETTE_EXPERT_MIN_PIECES`). The generator throws away fills with fewer arrows, and `ArrowEscapeBankVerificationTest` checks the bank for it. A shape that is too small for 140 arrows at the cap can't be Expert: generating it just reports 0 puzzles. Wide shapes are the usual case: the turtle (991 cells at the cap) has no Expert puzzles, and fish (1172) and runner (1285) only reach 140 arrows on some fills.

Expert also has to pass two gates in `classifyArrowEscapeDifficulty`, on top of the free-share floors: free share below `ARROW_ESCAPE_EXPERT_FREE_SHARE_CEILING` (0.15) and trail share at most `ARROW_ESCAPE_EXPERT_TRAIL_CEILING` (0.25). Trail share is how often a player who always takes the arrow next to the one just removed finds such an arrow. The generator lowers it with `reduceArrowEscapeTrail` after the fill. About a quarter of Expert fills pass, and the share depends on the shape: in calibration, rocket, octopus and elephant passed much less often than turtle, pine or runner.

## Design rules

- **Square or tall.** Keep `width / height` at or below about 1.1. Wide shapes hit the column cap early. A 100x62 turtle tops out at 573 cells at Hard's 45-column cap and 991 cells at Expert's 60-column cap, which is too small for both tiers.
- **Fill at least 45% of the box.** At 60x60 a shape needs about 42% fill to reach the 1500-cell Expert minimum, and at 60x45 about 36% to reach Hard's 961. Rings, thin letters and stick figures fall short. Thicken them, or accept that they can't be Expert.
- **No thin parts.** At `minRows`, every leg, stem, ear, handle and neck must be at least 2 cells thick. Thinner parts break off and are removed as specks, or they connect only through a corner. As a rule of thumb, nothing should be thinner than `height / minRows * 2` units.
- **Holes must be big.** A cut hole needs to be at least 3 cells across at `minRows`. Smaller holes get filled, or show up only at some board sizes.
- **No detail that depends on size.** The same shape is drawn at 12 rows and at 50 rows. If a feature shows only on big boards, the shape will look different between tiers. Leave it out.
- **Multi-part shapes need clear gaps.** With `parts > 1`, keep each gap at least 1 cell wide at `minRows`, and make each part much bigger than the speck limit. Arrows can cross a gap, so the parts still play as one puzzle.
- **Pick `minRows` honestly.** Use the smallest height where the shape reads and passes the fragmentation test. Easy boards are often a few rows taller than this anyway, because they grow to reach 100 cells.

## Workflow

1. Add the entry to `arrowescape_silhouettes.json`. The file is pretty-printed with 2-space indents. Keep that format so diffs stay small.
2. Preview the shape before you generate anything. Print `rasterizeSilhouette(silhouette, rows).toMaskRows()` from a scratch test at `minRows`, around 26 rows and at the size where Expert stops. Check that it reads at every size.
3. Run the shape tests:
   `./gradlew :cli:test --tests "*ArrowEscapeSilhouette*"`
   They check unique ids, parts and fragmentation at `minRows`, and they generate a Medium puzzle for every shape.
4. Generate 4 puzzles per tier. The bank only holds shaped boards, and every shape that can reach a tier has 4 puzzles in it:
   `./gradlew :cli:run --args="generate --game arrowescape --difficulty <tier> --count 4 --shape <id> --out app/src/main/assets"`
   Each tier writes its own bank file, `arrowescape_puzzles_<tier>.json`, but run the four tiers one after another, not in parallel, because they share the generation state file.
5. Run the bank check:
   `./gradlew :cli:test --tests "*ArrowEscapeBankVerificationTest*"`

## Seasonal shapes

A shape with a `season` never goes into the main bank. `--shape all` skips it, and generating it by id writes to `arrowescape_<season>_puzzles.json` instead of the per-tier main bank `arrowescape_puzzles_<tier>.json`. The app serves that bank only on its day: the Halloween bank on October 31, while "Seasonal touches" is on in Settings, as the first new puzzle of each tier that the player hasn't played that day.

The Halloween bank holds at least one puzzle per tier: `witchhat` (Easy), `bat` and `jackolantern` (Medium), and `cauldron` (one Hard board and one bigger Expert board). The bat is too wide for Hard or Expert, so keep it on Medium or below. The jack-o'-lantern reads by its cut-out eyes and mouth, which is how it avoids the plain pumpkin's `apple` problem. Regenerate a tier with:
`./gradlew :cli:run --args="generate --game arrowescape --difficulty <tier> --count 1 --shape <id> --out app/src/main/assets"`

## Shapes that work and shapes that don't

Good candidates are chunky objects that read by outline. The bank already uses house, tree, pine, apple, pear, owl, bear, rook, pawn, crown, bell, mug, hot-air balloon, padlock, hourglass, acorn, cactus, gift, duck, ghost, ice cream, boot, bottle, teapot, umbrella, chess knight, trophy, gem, star, gear, T-shirt, mitten, snail, squirrel, leaf, tulip, paw print, top hat, octopus, elephant and lamp.

A new shape must also look different from every shape already in the file. Compare the previews side by side at `minRows` and at 26 rows:
- A pumpkin was dropped because its lobes don't show in the outline, so it reads as `apple`.
- A lightbulb was dropped because a round top on a narrow base reads as `balloon` or `icecream`.
- A tent was dropped because a triangle with a door notch reads as the letter A, and it is too thin for Expert.
- A lighthouse was dropped because its gallery and lantern read as a chess piece.
- A pineapple was dropped because at mid sizes the oval with a leaf tuft reads as a grenade.
- A snowman was dropped because the pinches between its balls vanish on Easy boards, so it reads as a vase.
- A penguin, a cupcake and a strawberry were dropped because on Easy boards they blur into blobs that read as `pine`, `acorn` and `apple`.

Avoid these:

| Shape | Problem |
|---|---|
| Whale, cloud, bat, car, train | Too wide; they hit the column cap. |
| Key, snowflake, music note, spider | Too thin; they break into specks on Easy boards. |
| Faces, clocks, anything defined by inner detail | The detail never shows. |
| Letters or text, except a deliberate multi-part logo | Too thin, and they read as text, not as a shape. |
