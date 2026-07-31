# Difficulty Ladder

## User goal

- Help players pick a level that matches their current skill.
- Show a clear path from easy to expert.

## Architecture goal

- Every new game should ship with playable `easy`, `medium`, `hard`, and `expert` levels.
- Difficulty should reflect real differences in move complexity and learning demand, not cosmetic labels.

## Rules

- Validate that all four levels are meaningful and separable.
- Reject models where one tier is practically unreachable or two tiers behave the same.
- If engine-backed, difficulty classification lives in `engine/` and should support re-evaluation when
  heuristics change (regenerate/reclassify via `cli/` — see [[project_shared_puzzle_engine]] for known gaps
  in the reclassification/audit path).
- If not engine-backed (puzzle bank is hand-authored or generated outside the engine), difficulty is whatever
  `<Id>PuzzleBank.kt` tags each puzzle with — still validate the four tiers are meaningfully different.
- How to Play may explain current and next difficulty expectations if useful, but should not turn into a
  tier-by-tier comparison lesson.

## Validation questions

- Does each level make sense for this puzzle?
- Are bucket distributions reasonable?
- Can the engine/generator still produce enough puzzles for each tier?
- Does progression reflect harder move combinations rather than arbitrary size changes alone?

## File map

- `app/src/main/java/com/quietgrid/app/core/GameCatalog.kt` — game registration (title/tagline, not per-
  difficulty data)
- `app/src/main/java/com/quietgrid/app/games/<id>/<Id>PuzzleBank.kt` — loads puzzles per difficulty from
  `app/src/main/assets/<id>_puzzles.json`
- `engine/src/main/kotlin/com/quietgrid/engine/<game>/` — difficulty classification, when engine-backed
- `cli/src/main/kotlin/com/quietgrid/cli/<game>/` — generation targeting a difficulty tier
- `res/values*/strings.xml` — difficulty labels/descriptions shown in the picker

## Mistakes to avoid

- Do not define four levels on paper if they collapse in practice.
- Do not teach difficulty progression only through UI labels with no move-based meaning.
