# Context Maps

Use these packets when AI is editing one subsystem and should not load the whole repository mentally at once.

## Game definition

### User goal
- Expose the game to the app shell with the correct title, difficulty list, and supported surfaces.

### Architecture goal
- Keep `definition.ts` thin and use getters for user-facing strings.

### File map
- `src/games/<id>/definition.ts`
- `src/app/shell/games/gameRegistry.ts`
- `src/engine/gameRegistry.ts` when engine-backed

### Mistakes to avoid
- Do not grow `definition.ts` into a behavior dump.

## Gameplay core

### User goal
- Make puzzle rules truthful and consistent.

### Architecture goal
- Own session logic, rule logic, and action reduction inside `gameplay/`.

### File map
- `src/games/<id>/gameplay/activePuzzle.ts`
- `src/games/<id>/gameplay/actions.ts`
- `src/games/<id>/gameplay/playContract.ts`
- `src/games/<id>/gameplay/rules/`

### Mistakes to avoid
- Do not move truth-bearing puzzle rules into UI.

## Learning Center

### User goal
- Teach interaction, moves, and improvement.

### Architecture goal
- Learning Center is the umbrella subsystem for tutorial, next move, and analyzer.
- Load `docs/ai/context/learning-center.md` for the canonical Learning Center rules and surface-specific guidance.

### File map
- `src/games/<id>/ui/tutorial/`
- `src/games/<id>/ui/learning/analyzer/`
- `src/games/<id>/content/tutorialLessons.ts`
- `src/games/<id>/content/i18n/`

### Mistakes to avoid
- Do not treat this summary as the rule source when `docs/ai/context/learning-center.md` exists.

## Content and i18n

### User goal
- Keep all game-facing copy consistent and translatable.

### Architecture goal
- Put all game-facing copy in `content/i18n/`, including tutorial copy.

### File map
- `src/games/<id>/content/i18n/index.ts`
- `src/games/<id>/content/i18n/en.ts`
- `src/games/<id>/content/i18n/nl.ts`

### Mistakes to avoid
- Do not hardcode game-facing copy in screens or components.

## Persistence and platform

### User goal
- Resume sessions safely and consistently.

### Architecture goal
- Keep game-specific payload shapes in `gameplay/activePuzzle.ts` and app normalization in storage helpers.

### File map
- `src/games/<id>/platform/`
- `src/app/utils/activeSessionStateStorage.ts`

### Mistakes to avoid
- Do not scatter persistence rules across screens.

## Engine generation

### User goal
- Produce puzzles with a real solve path and valid difficulty ladder.

### Architecture goal
- Keep generator support inside `engine/` and `puzzles/`.

### File map
- `src/games/<id>/engine/`
- `src/games/<id>/puzzles/`
- `src/engine/gameRegistry.ts`

### Mistakes to avoid
- Do not ship engine-backed puzzles without uniqueness, difficulty, and reclassification rules.
