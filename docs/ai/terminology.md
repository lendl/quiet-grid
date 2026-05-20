# Terminology

Follow these terms consistently in code, file names, directories, docs, and AI-generated changes.

## Canonical terms

- **Game**: top-level playable module and shell identity. Use for registries, definitions, overview navigation, and game packages like `src/games/<id>/`.
- **Puzzle**: one concrete board or challenge payload. Keep `.puzzle` fields and board-specific concepts as `puzzle`.
- **Session**: live or persisted attempt on a puzzle. Use explicit names like `ActiveSession` and `PersistedSessionEnvelope` for persisted state.
- **Result**: terminal app-level outcome model for a session.
- **Solved / Failed**: canonical internal result statuses.
- **Failure reason**: specific unsuccessful end cause.

## Required boundaries

### Use `game` for module identity

Examples:

- `GameId`
- `GameDefinition`
- `gameRegistry`
- `Game` route
- `GameTabs`

### Use `puzzle` for board payloads

Examples:

- `.puzzle` object fields
- puzzle generation
- puzzle solutions and masks
- `PuzzlePlayScreen` when the screen is about playing one puzzle

### Use `session` for attempts and persistence

Examples:

- `ActiveSession`
- `PersistedSessionEnvelope`
- active-session storage helpers

Do not collapse persisted session names into bare `Session` when runtime play-session types already exist.

### Use `result` for terminal models

Examples:

- `SessionResult`
- `createResult()`
- `SolvedResultVariant`
- `FailureReason`

## UX wording

Player-facing copy may still say **win / loss** when that tone fits the game. Internal models should still use `solved / failed` and `failure reason`.

## AI instruction

When adding or editing code:

1. Reuse these terms instead of inventing synonyms.
2. Prefer matching existing file and symbol names to these boundaries.
3. Do not reintroduce mixed naming like `PuzzleDefinition`, `puzzleRegistry`, `CompletionVariant`, or `LossReason`.
4. Do not rename `.puzzle` payload fields just to force `game` wording.
