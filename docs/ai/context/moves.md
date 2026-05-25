# Canonical Moves

## User goal

- Give players a clear reasoning vocabulary for how the puzzle is solved.
- Let hints, analysis, and tutorials speak the same language.

## Architecture goal

- Canonical moves are the source of truth for tutorial, next move, analyzer, and engine solving/classification.
- Support actions must be defined separately so AI does not confuse optional play style with core logic.

## Rules

- Define canonical moves early for each new game.
- Keep support actions separate from canonical moves.
- If AI knows the puzzle well, it may suggest move vocabulary, but the user must approve it.
- Move explanations should come from the same move logic used by gameplay analysis.
- If the game is engine-backed, engine solving/classification must use the same approved move system as tutorial, next move, and analyzer.
- Do not hide brute-force or full-solution proof behind a player-facing move unless that search model is itself an explicitly approved move.
- If hypothetical branching is allowed, document it as part of the move system and require each branch to resolve through approved moves.

## Examples

- Takuzu: find pairs, avoid trios, complete lines, eliminate filled lines, eliminate impossible combinations
- Nonogram: overlap fill is canonical; marking empty cells is a support action
- Minesweeper flagging is a support action, not a canonical move

## File map

- `src/games/<id>/gameplay/moves.ts` or equivalent move-definition module
- `src/games/<id>/gameplay/analysis/`
- `src/games/<id>/ui/learning/analyzer/`
- `src/games/<id>/engine/` when engine-backed

## Mistakes to avoid

- Do not let tutorial, next move, analyzer, and engine use four different reasoning systems.
- Do not teach support actions as mandatory skill if they are optional style choices.
- Do not classify engine difficulty with a hidden proof model that player-facing teaching cannot explain.
