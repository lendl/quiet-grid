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
4. **Growth.** The board starts at `minRows` and grows one row at a time until it reaches the tier's cell target. It stops at **60 rows or 45 columns**, whichever comes first.

| Tier | Target active cells |
|---|---|
| Easy | 100-144 |
| Medium | 324-576 |
| Hard | 625-900 |
| Expert | 961-1369 |

If the cap stops the growth before the target, the board stays smaller than its tier asks for. Avoid shapes that do this, because their Hard and Expert boards end up the same size.

## Design rules

- **Square or tall.** Keep `width / height` at or below about 1.1. Wide shapes hit the 45-column cap early. A 100x62 turtle tops out at 573 cells, which is too small for both Hard and Expert.
- **Fill at least 45% of the box.** At 45x45 a shape needs about 47% fill to reach the 961-cell Expert minimum. Rings, thin letters and stick figures fall short. Thicken them, or accept that they can't be Expert.
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
4. Generate 4 puzzles per tier. The bank only holds shaped boards, and every shape has 4 puzzles in each tier:
   `./gradlew :cli:run --args="generate --game arrowescape --difficulty <tier> --count 4 --shape <id> --out app/src/main/assets"`
   Run the four tiers one after another, not in parallel, because they all write to the same bank file.
5. Run the bank check:
   `./gradlew :cli:test --tests "*ArrowEscapeBankVerificationTest*"`

## Shapes that work and shapes that don't

Good candidates are chunky objects that read by outline: house, tree, pine, apple, pear, owl, penguin, bear head, chess rook, chess pawn, crown, bell, lightbulb, teacup, hot-air balloon, padlock, hourglass, pumpkin, ghost.

Avoid these:

| Shape | Problem |
|---|---|
| Whale, cloud, bat, car, train | Too wide; they hit the 45-column cap. |
| Key, snowflake, music note, spider | Too thin; they break into specks on Easy boards. |
| Faces, clocks, anything defined by inner detail | The detail never shows. |
| Letters or text, except a deliberate multi-part logo | Too thin, and they read as text, not as a shape. |
