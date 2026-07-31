# Canonical Moves

## User goal

- Give players a clear reasoning vocabulary for how the puzzle is solved.
- Let next-move hints, How to Play copy, and difficulty scoring speak the same language.

## Architecture goal

- Canonical moves are the source of truth for difficulty classification (`engine/`), puzzle solving/generation
  (`engine/` + `cli/`), and any in-play next-move hints (`app/`).
- Support actions must be defined separately so AI does not confuse optional play style with core logic.

## Rules

- Define canonical moves early for each new game.
- Keep support actions separate from canonical moves.
- If AI knows the puzzle well, it may suggest move vocabulary, but the user must approve it.
- Next-move hint explanations (when a game has hints) should come from the same move-detection logic the
  engine uses for solving/difficulty, not a separately invented heuristic.

## Examples

- Takuzu: find pairs, avoid trios, complete lines (`engine/src/main/kotlin/com/quietgrid/engine/takuzu/`,
  ported to hints in `app/src/main/java/com/quietgrid/app/games/takuzu/TakuzuNextMove.kt`)
- Nonogram: overlap fill, forced empty, complete line
- Minesweeper flagging is a support action, not a canonical move

## File map

- `engine/src/main/kotlin/com/quietgrid/engine/<game>/` — canonical move/technique logic, difficulty scoring
- `app/src/main/java/com/quietgrid/app/games/<id>/<Id>NextMove.kt` — optional in-play hint surface built on
  the engine's moves
- `app/src/main/java/com/quietgrid/app/games/<id>/<Id>Logic.kt` — in-app rule/validation logic
- `cli/src/main/kotlin/com/quietgrid/cli/<game>/` — generator, consumes engine moves for difficulty targeting

## Mistakes to avoid

- Do not let hints, How to Play copy, and engine difficulty scoring use different reasoning systems for the
  same game.
- Do not teach support actions as mandatory skill if they are optional style choices.
- Do not duplicate move logic between `engine/` and `app/` — see [[project_shared_puzzle_engine]] for the
  known takuzu gap where this happened; new games should not repeat it.
