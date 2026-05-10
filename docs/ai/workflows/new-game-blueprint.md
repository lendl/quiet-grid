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
- Unless explicitly scoped narrower, Learning Center guidance applies to all three surfaces.
- Tutorial user goal: teach the player the rules, interaction model, and core moves safely.
- Next move user goal: give the player one valid live suggestion while keeping the player in control.
- Analyzer user goal: help the player reflect on outcomes and improve future decisions.
- Tutorial teaches interaction and core moves.
- Next move explains one valid stored move suggestion at a time.
- Analyzer reflects either full solution teaching or loss-state analysis, based on engine mode.
