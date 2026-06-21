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
  - prefer imports from canonical subfolders (`ui/`, `gameplay/`, `content/`, `platform/`) instead of legacy root shims
- `types.ts`
  - shared game types
- `gameplay/`
  - rules, actions, active puzzle shape, play contract, canonical techniques
- `ui/`
  - play plus Learning Center rendering and wiring
  - playable grid wires zoom/pan support (for example `ZoomableBoardSurface`) only when `gestureProfile.mode === 'zoom'`; `viewport: required` always mounts it, `viewport: optional` lets the shell decide by screen size. `swipe` and `tap` modes never mount it — zoom and swipe cannot coexist in the same game
- `content/`
  - game-facing copy, how to play lesson configs, localized content
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
4. only then wire broad app surfaces (play, how to play, technique explanation)

Do not treat full game wiring as proof that engine work is done. Engine feasibility is its own gate.

## Content rule

All game-facing copy belongs in `content/i18n/`, including:

- play labels
- how to play copy
- technique explanation copy
- how to play static text
- loss-related text

Older packages may still carry compatibility shims like root `playAdapter.tsx`, `playContract.ts`, or `activePuzzle.ts`, plus legacy folders such as `screens/` or `learningCenter/`. Treat those as migration aids only; new code and refactors should point at the canonical locations above.

## Learning Center rule

- Learning Center is the umbrella subsystem for how to play (static + onboarding) and technique explanation (live + post-game).
- Load `docs/ai/context/learning-center.md` and follow it as the single source of truth for Learning Center user goals, rules, and architecture guidance.
