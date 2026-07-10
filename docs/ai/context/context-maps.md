# Context Maps

Use these packets when AI is editing one subsystem and should not load the whole repository mentally at once.

## Game definition

### User goal
- Expose the game to the app shell with the correct title, difficulty list, and supported surfaces.

### Architecture goal
- Keep `definition.ts` thin and use getters for user-facing strings.
- Inside `definition.ts`, prefer canonical imports from `ui/`, `gameplay/`, and `content/` over legacy compatibility shims.

### File map
- `src/games/<id>/definition.ts`
- `src/app/shell/games/gameRegistry.ts` — must add an entry here or the game is not discoverable
- `src/engine/gameRegistry.ts` — must add an entry here if the game has an engine

### Mistakes to avoid
- Do not grow `definition.ts` into a behavior dump.
- Do not forget to add the game to `src/app/shell/games/gameRegistry.ts` — routing silently fails without it.

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
- Teach interaction, techniques, and improvement via two surfaces: how to play and technique explanation.

### Architecture goal
- Learning Center is the umbrella subsystem for how to play (static + onboarding) and technique explanation (live + post-game).
- Load `docs/ai/context/learning-center.md` for the canonical Learning Center rules and surface-specific guidance.

### File map
- `src/games/<id>/ui/tutorial/` (how to play onboarding, code name)
- `src/games/<id>/ui/learning/analyzer/` (technique explanation post-game, code name)
- `src/games/<id>/content/tutorialLessons.ts` (how to play lessons, code name)
- `src/games/<id>/content/i18n/` (how to play static content in howToPlay section)

### Mistakes to avoid
- Do not treat this summary as the rule source when `docs/ai/context/learning-center.md` exists.

## Content and i18n

### User goal
- Keep all game-facing copy consistent and translatable. Adding a new language must only require adding one locale file per game — no gameplay code changes.

### Architecture goal
- Put all game-facing copy in `content/i18n/`, including how to play copy.
- Content files (`howToPlay.ts`, `loss.ts`, etc.) are thin wrappers that call the i18n function — they do not contain strings directly. All strings live in `content/i18n/en.ts` and the other locale files.
- This includes labels for lines, cells, rule names, plural forms, and any other language-specific string used in gameplay or technique explanation output.
- Never switch on language or locale in gameplay, analysis, or content code. If a string varies by language, it belongs in the locale file — not in a `switch (language)` block inside a `.ts` file.
- If an older package still has a root `i18n/` shim, treat it as compatibility only and import `content/i18n/` in new edits.

### File map
- `src/games/<id>/content/i18n/index.ts`
- `src/games/<id>/content/i18n/en.ts`
- `src/games/<id>/content/i18n/nl.ts`
- `src/games/<id>/content/i18n/de.ts`
- `src/games/<id>/content/i18n/fr.ts`
- `src/games/<id>/content/i18n/es.ts`

### Mistakes to avoid
- Do not hardcode game-facing copy in screens, components, or content files. Strings belong in `content/i18n/`.
- Do not switch on language in gameplay or analysis code to build strings. That makes adding a language require code changes outside locale files.

## Persistence and platform

### User goal
- Resume sessions safely and consistently.

### Architecture goal
- Keep game-specific payload shapes in `gameplay/activePuzzle.ts` and app normalization in storage helpers.
- Every new game must add validate and normalize functions to `activeSessionStateStorage.ts`. Without this, sessions silently corrupt on load after an app update.

### File map
- `src/games/<id>/platform/`
- `src/app/utils/activeSessionStateStorage.ts` — add `is<Game>ActiveSession()` and `normalize<Game>ActiveSession()` here

### Mistakes to avoid
- Do not scatter persistence rules across screens.
- Do not ship a new game without updating `activeSessionStateStorage.ts` — missing normalization causes silent session corruption, not a typecheck error.

## Board visual accessibility

### User goal
- Grid tap targets, contrast, and edge padding stay readable for players with low vision or motor/memory impairment.

### Architecture goal
- Load `docs/ai/context/board-visual-accessibility.md` for the baseline recipe (border width/alpha, given-vs-entered color coding, tablet `maxCellSize`, edge padding, selected-cell emphasis) and which games have an established look and feel that overrides it.

### File map
- `src/app/shell/boardLayout.ts`, `src/app/shell/renderTokens.ts`
- `src/games/<id>/ui/play/components/*Grid*.tsx`, `src/games/<id>/ui/play/adapter.tsx`

### Mistakes to avoid
- Do not treat this summary as the rule source when `docs/ai/context/board-visual-accessibility.md` exists.
- Do not blanket-apply the recipe to a game with an established visual identity without checking first (see minesweeper in that doc).

## Engine generation

### User goal
- Produce puzzles with a real solve path and valid difficulty ladder.

### Architecture goal
- Keep generator support inside `engine/` and `puzzles/`.
- Treat engine feasibility as a separate gate before broad app surface work.
- Load `docs/ai/context/engine.md` for full generation pipeline, difficulty balancing guidance, and catalog rules.

### File map
- `src/games/<id>/engine/`
- `src/games/<id>/puzzles/`
- `src/engine/gameRegistry.ts`

### Mistakes to avoid
- Do not lock app/how to play/technique explanation work before the generator, classifier, dedupe, and catalog round-trip are proven.
- Do not ship engine-backed puzzles without verifying bucket supply across all sizes and difficulties.
