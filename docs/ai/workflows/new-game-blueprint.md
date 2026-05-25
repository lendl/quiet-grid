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
  - play plus Learning Center rendering and wiring
  - playable grid should use zoom/pan support (for example `ZoomableBoardSurface`) when board size can exceed comfortable tap/read bounds
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

- Learning Center is the umbrella subsystem for tutorial, next move, and analyzer.
- Load `docs/ai/context/learning-center.md` and follow it as the single source of truth for Learning Center user goals, rules, and architecture guidance.
