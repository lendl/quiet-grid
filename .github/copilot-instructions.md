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

There is no automated test script or committed `*.test.*` / `*.spec.*` suite in this repository today, so validate changes with lint plus the relevant type-check command(s).

## High-level architecture

- `App.tsx` is a thin provider shell only. It mounts `LanguageProvider`, `ThemeProvider`, and `AppNavigator`; app-wide behavior usually lives under `src/app/`, not in `App.tsx`.
- Navigation is centralized in `src/app/navigation/AppNavigator.tsx`. Shared screens route by `puzzleTypeId`, then resolve the active game through the shell registry instead of importing game logic directly into screens.
- Puzzle games are plugin-like modules under `src/games/<id>/`. Each game exposes a `definition.ts` that registers title/tagline/content getters, supported features, and a `playAdapter`. The shell registry in `src/app/shell/games/gameRegistry.ts` is the source of truth for installed app games.
- Shared puzzle play flow lives in `src/app/shell/`. `usePuzzlePlayController()` drives loading, persistence, dialogs, completion/loss routing, and delegates game-specific behavior to the selected adapter through the generic `PuzzlePlayAdapter` contract in `src/app/shell/games/playAdapter.ts`.
- Localization has two layers:
  - `src/app/i18n/index.ts` contains global app chrome strings and current-language state.
  - Each game owns its own localized content under `src/games/<id>/i18n/`, resolved with `resolveGameContent()` from `src/app/i18n/gameContent.ts`.
- Active puzzle persistence is versioned and normalized in `src/app/utils/activePuzzleStateStorage.ts`. That file is responsible for shape validation, legacy migrations (`binary` -> `takuzu`), and save/load envelopes before anything reaches storage helpers.
- The offline generator in `src/engine/` is separate from Expo app runtime. `src/engine/index.ts` loads an `EngineGameDefinition` from `src/engine/gameRegistry.ts`, generates catalog entries, and deduplicates them through the local SQLite DB in `src/engine/puzzles.db`. Today only Takuzu is registered as an engine-capable game.

## Key conventions

- Keep puzzle-specific logic inside the owning game package. Shared app shell files should depend on `PuzzleDefinition` / `PuzzlePlayAdapter` contracts, not on Takuzu- or Minesweeper-specific internals.
- In game `definition.ts` files, user-facing strings are exposed through getters (`get title()`, `get tagline()`, `content` getters) so language changes always read fresh localized values. Follow that pattern instead of caching strings at module load.
- Treat game i18n files as the single source of truth for user-visible copy. Do not duplicate tutorial/how-to-play/loss text in content builders when it can be keyed back to `src/games/<id>/i18n/`.
- When changing persisted puzzle/session shapes, update `activePuzzleStateStorage.ts` normalization and guards together. Backward compatibility for stored local data is handled there, not ad hoc in screens.
- Engine work uses the dedicated `src/engine/tsconfig.json` and separate `npm run typecheck:engine`. Keep engine-safe code free of Expo/React Native runtime dependencies.
- New game support is wired in two registries:
  - App play flow: add the game definition to `src/app/shell/games/gameRegistry.ts`
  - Engine generation (only if the game has a generator): add the engine definition to `src/engine/gameRegistry.ts`
