# New Game Blueprint

## Package shape

```text
src/games/<id>/
  definition.ts
  types.ts
  gameplay/
  ui/
  content/
  platform/
  engine/        # optional
  puzzles/       # optional, engine-backed only
```

## Responsibilities

- `definition.ts`
  - thin shell entrypoint
  - getters for user-facing strings
- `types.ts`
  - shared game types
- `gameplay/`
  - rules, actions, active puzzle shape, play contract, canonical moves
- `ui/`
  - play, tutorial, analyzer rendering
- `content/`
  - game-facing copy, tutorial lesson configs, localized content
- `platform/`
  - runtime loading, codecs, persistence helpers local to the game
- `engine/`
  - generator, difficulty logic, engine definition
- `puzzles/`
  - generated puzzle catalog for engine-backed games

## Content rule

All game-facing copy belongs in `content/i18n/`, including:

- play labels
- tutorial copy
- analyzer copy
- how-to-play text
- loss-related text

## Learning Center rule

- Tutorial teaches interaction and core moves.
- Next move explains one valid move.
- Analyzer reflects either full solution teaching or loss-state analysis, based on engine mode.
