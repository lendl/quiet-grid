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
  - generator, proof/classification logic, engine definition
  - dedupe strategy and catalog formatting/round-trip safety
- `puzzles/`
  - generated puzzle catalog for engine-backed games

## Engine-backed delivery order

For engine-backed games, prefer this order:

1. lock size/difficulty matrix and generation contract
2. build generator + classifier + dedupe + catalog round-trip spike
3. bulk-generate enough puzzles to prove bucket supply
4. only then wire broad app surfaces (play, tutorial, next move, analyzer)

Do not treat full game wiring as proof that engine work is done. Engine feasibility is its own gate.

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
