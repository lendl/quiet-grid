# Techniques

## User goal

- Give players a clear reasoning vocabulary for how the puzzle is solved.
- Let technique explanation, how to play, and engine solving speak the same language.

## Architecture goal

- Techniques are the source of truth for how to play, technique explanation (live), technique explanation (post-game), and engine solving/classification.
- Support actions must be defined separately so AI does not confuse optional play style with core logic.

## Techniques vs support actions

A technique directly advances the puzzle toward solved — it is a logical deduction that changes puzzle state (place a digit, reveal a cell, circle a word). A support action records information for later without advancing the puzzle (Minesweeper flag, Sudoku pencil note, Nonogram empty-cell mark).

The practical test: if a player skips the action entirely, can they still solve the puzzle? If yes, it is a support action. If no, it is a technique.

This distinction matters for technique explanation: the hint system should teach logical deductions, not bookkeeping choices. Flagging is not a technique; the deduction that forces the flag is. Notes are not a technique; the naked pair or hidden single that the notes reveal is.

## Rules

- Every game's techniques must be explicitly defined and approved. Do not invent or rename techniques during implementation.
- Each technique gets its own file under `gameplay/analysis/techniqueModules/`. Do not bundle multiple techniques into one file.
- Keep support actions separate from techniques.
- If AI knows the puzzle well, it may suggest technique vocabulary, but the user must approve it.
- Technique explanations should come from the same technique logic used by gameplay analysis.
- The suggested action from technique explanation (live or post-game) must always be a technique, never a support action. Support actions may appear as context or evidence in the explanation, but the player-facing step must resolve to a technique.
- Engine solving/classification must be based on techniques only. Support actions must not influence difficulty classification.
- If the game is engine-backed, engine solving/classification must use the same approved technique system as how to play, technique explanation (live), and technique explanation (post-game).
- Do not hide brute-force or full-solution proof behind a player-facing technique unless that search model is itself an explicitly approved technique.
- If hypothetical branching is allowed, document it as part of the technique system and require each branch to resolve through approved techniques.

## Examples

- Takuzu: find pairs, avoid trios, complete lines, eliminate filled lines, eliminate impossible combinations
- Nonogram: overlap fill is canonical; marking empty cells is a support action
- Minesweeper flagging is a support action, not a canonical technique

## File map

- `src/games/<id>/gameplay/analysis/techniqueModules/<techniqueName>.ts` — one file per technique (preferred pattern, as used in Sudoku)
- `src/games/<id>/gameplay/analysis/techniques.ts` — registers and orders all technique modules
- `src/games/<id>/gameplay/analysis/dispatcher.ts` — selects the next applicable technique from the ordered list
- `src/games/<id>/gameplay/analysis/` — shared helpers, candidate logic, difficulty classification
- `src/games/<id>/ui/learning/analyzer/`
- `src/games/<id>/engine/` when engine-backed

## Mistakes to avoid

- Do not let how to play, technique explanation (live), technique explanation (post-game), and engine use four different reasoning systems.
- Do not teach support actions as mandatory skill if they are optional style choices.
- Do not classify engine difficulty with a hidden proof model that player-facing teaching cannot explain.
