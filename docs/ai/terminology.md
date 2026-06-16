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

---

## Player-facing copy

These rules apply to i18n files, UI labels, tutorial text, how-to-play content, and loss screens.

### Preferred terms

| Concept | Use | Avoid |
| --- | --- | --- |
| One concrete board or challenge | `puzzle` | `game`, `level`, `board` |
| The category of game | `puzzle type` | `game type`, `game mode` |
| Player won | `puzzle solved` | `completed`, `finished`, `won` |
| Rule-based loss (e.g. Minesweeper mine) | `lost puzzle` | `failed`, `game over` |
| Player stopped without solving | `unfinished puzzle` | `ended puzzle`, `gave up`, `quit` |
| The play area | `grid` | `board`, `field` |
| A wrong input | `invalid` or omit entirely | `mistake`, `error`, `wrong` |

### Game titles

- In English copy use the English title: `Word Search`, `Minesweeper`, etc.
- On non-English surfaces use the localized title: `Woordzoeker`, `Wortsuche`, `Mots mêlés`, `Sopa de Letras`. Never use the English title as a fallback in translated copy.
- In labels where the game name already provides context, use the short form: `Binary`, `Minesweeper` rather than repeating the full puzzle-type phrase.

### Loss and abandonment screens

- Rule-failure loss title: `Puzzle lost`
- Abandoned/unfinished title: `Puzzle unfinished`
- `session` is acceptable in loss titles when the game name is already the subject (e.g. `Word Search session ended`), but prefer `puzzle` everywhere else.

### Tone

- Keep copy calm and direct — no blame.
- Avoid `mistake`, `failed`, `give up`, `wrong`.
- Do not over-explain; trust the player.
