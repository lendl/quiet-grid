# Quiet Grid Copilot Instructions

## Commands

- Install dependencies: `npm install`
- Run app on Android: `npm run android`
- Start Expo dev server: `npm run start`
- Lint app code: `npm run lint`
- Lint one file: `npx eslint App.tsx` or `npx eslint src\path\to\file.ts`
- Type-check Expo app: `npm run typecheck:app`
- Type-check engine CLI: `npm run typecheck:engine`
- Run Takuzu engine CLI: `npm run engine -- --game=takuzu`
- Generate one Takuzu size bucket: `npm run engine -- --game=takuzu --size=8`
- Run Nonogram engine CLI: `npm run engine -- --game=nonogram`
- Generate one Nonogram bucket: `npm run engine -- --game=nonogram --size=5 --difficulty=easy 25`

There is no automated test script or committed `*.test.*` / `*.spec.*` suite in this repository today, so validate changes with lint plus the relevant type-check command(s).

## AI task entrypoints

- Start at `docs/ai/README.md` for the split AI doc system.
- For new games, load:
  - `docs/ai/workflows/new-game.md`
  - `docs/ai/workflows/new-game-blueprint.md`
  - `docs/ai/workflows/new-game-checklist.md`
- For reusable game rules and Learning Center context, load the relevant files under `docs/ai/context/`.
- For starter code shape, use `docs/ai/scaffolds/base-game/` and add `docs/ai/scaffolds/engine-addon/` only when the game uses the engine.
- Keep these concepts explicit when adding or changing games:
  - canonical moves
  - support actions
  - mistake policy
  - loss condition
  - feedback effects
- If a new reusable feedback effect is needed, create that effect in a separate change before adding the game.
- All game-facing copy belongs in `src/games/<id>/content/i18n/`.

## High-level architecture

- `App.tsx` is a thin provider shell only. It mounts `LanguageProvider`, `ThemeProvider`, and `AppNavigator`; app-wide behavior usually lives under `src/app/`, not in `App.tsx`.
- Navigation is centralized in `src/app/navigation/AppNavigator.tsx`. Shared screens route by `puzzleTypeId`, then resolve the active game through the shell registry instead of importing game logic directly into screens.
- Puzzle games are plugin-like modules under `src/games/<id>/`. Each game keeps `definition.ts` at package root as the shell entrypoint, while internal code is split by responsibility: `gameplay/` for rules and session logic, `ui/` for React rendering and adapter glue, `content/` for localized copy, `platform/` for storage/runtime integration, and `engine/` for generator support. The shell registry in `src/app/shell/games/gameRegistry.ts` is the source of truth for installed app games, and it currently includes Takuzu, Minesweeper, and Nonogram.
- Shared puzzle play flow lives in `src/app/shell/`. `usePuzzlePlayController()` drives loading, persistence, dialogs, completion/loss routing, and delegates game-specific behavior to the selected adapter through the generic `PuzzlePlayAdapter` contract in `src/app/shell/games/playAdapter.ts`.
- Localization has two layers:
  - `src/app/i18n/index.ts` contains global app chrome strings and current-language state.
  - Each game owns its own localized content under `src/games/<id>/content/i18n/`, resolved with `resolveGameContent()` from `src/app/i18n/gameContent.ts`.
- Active puzzle persistence is versioned and normalized in `src/app/utils/activePuzzleStateStorage.ts`. That file is responsible for shape validation, legacy migrations (`binary` -> `takuzu`), and save/load envelopes before anything reaches storage helpers.
- The offline generator in `src/engine/` is separate from Expo app runtime. `src/engine/index.ts` loads an `EngineGameDefinition` from `src/engine/gameRegistry.ts`, generates catalog entries, and deduplicates them through the local SQLite DB in `src/engine/puzzles.db`. Takuzu and Nonogram are both registered as engine-capable games.

## Key conventions

- Keep puzzle-specific logic inside the owning game package. Shared app shell files should depend on `PuzzleDefinition` / `PuzzlePlayAdapter` contracts and root game entrypoints, not on deep Takuzu- or Minesweeper-specific internals.
- In game `definition.ts` files, user-facing strings are exposed through getters (`get title()`, `get tagline()`, `content` getters) so language changes always read fresh localized values. Keep `definition.ts` thin and push rules, UI, content, and platform glue into their subfolders instead of growing root files.
- Treat game i18n files as the single source of truth for user-visible copy. Game locale files now live under `src/games/<id>/content/i18n/`. Do not duplicate tutorial/how-to-play/loss text in content builders when it can be keyed back there.
- When changing persisted puzzle/session shapes, update `activePuzzleStateStorage.ts` normalization and guards together, and keep game-specific persisted payload shapes in `src/games/<id>/gameplay/activePuzzle.ts`. Backward compatibility for stored local data is handled in app storage normalization, not ad hoc in screens.
- Engine work uses the dedicated `src/engine/tsconfig.json` and separate `npm run typecheck:engine`. Keep engine-safe code free of Expo/React Native runtime dependencies, and isolate game generator/catalog code under `src/games/<id>/engine/`.
- New game support is wired in two registries:
  - App play flow: add the game definition to `src/app/shell/games/gameRegistry.ts`
  - Engine generation (only if the game has a generator): add the engine definition to `src/engine/gameRegistry.ts`
- For new games, default placement is: rules/session logic in `gameplay/`, React components and adapter in `ui/`, localized copy in `content/`, persistence/runtime glue in `platform/`, and only keep `definition.ts` at package root.
