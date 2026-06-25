# Quiet Grid — Claude Instructions

## Commands

- Install dependencies: `npm install`
- Run app on Android: `npm run android`
- Start Expo dev server: `npm run start`
- Check Expo project health: `npx expo-doctor`
- Lint app code: `npm run lint`
- Lint one file: `npx eslint src/path/to/file.ts`
- Type-check app: `npx tsc --noEmit`
- Type-check engine CLI: `npm run typecheck:engine`
- Run Takuzu engine CLI: `npm run engine -- --game=takuzu`
- Run Nonogram engine CLI: `npm run engine -- --game=nonogram`

## After making changes

Always run `npx tsc --noEmit` after editing TypeScript files and fix any errors before finishing.

Always run `npm run test:regression` after any change and fix any failures before finishing.

## TypeScript file conventions

- Use straight ASCII quotes (`'` and `"`) in all `.ts` / `.tsx` files — never curly/smart quotes (`'` `'` `"` `"`). This applies especially to i18n locale files containing non-English text.

## Terminology

When writing player-facing copy, i18n strings, loss screens, or tutorial text, read `docs/ai/terminology.md` for canonical terms (puzzle, unfinished, lost, grid, etc.) and game title localization rules.

## AI documentation

For new games and reusable context, load the relevant files under `docs/ai/`:

- New game workflow: `docs/ai/new-game/README.md`
- New game blueprint + checklist: `docs/ai/workflows/`
- Game rules and Learning Center context: `docs/ai/context/`
- Starter scaffolds: `docs/ai/scaffolds/`

## Architecture

- `App.tsx` is a thin provider shell. App-wide behavior lives under `src/app/`, not in `App.tsx`.
- Navigation is centralized in `src/app/navigation/AppNavigator.tsx`. Shared screens route by `puzzleTypeId` and resolve the active game through the shell registry.
- Games are plugin-like modules under `src/games/<id>/`. Each game keeps `definition.ts` at package root as the shell entrypoint, with internal code split by: `gameplay/` (rules/session), `ui/` (React/adapter), `content/` (localized copy), `platform/` (storage/runtime), `engine/` (generator).
- The shell registry at `src/app/shell/games/gameRegistry.ts` is the source of truth for installed games: Takuzu, Minesweeper, Nonogram, Sudoku, Word Search.
- `usePuzzlePlayController()` drives loading, persistence, dialogs, completion/loss routing, and delegates game-specific behavior through the `PuzzlePlayAdapter` contract.
- Localization has two layers: `src/app/i18n/index.ts` (global chrome), and `src/games/<id>/content/i18n/` (per-game copy resolved via `resolveGameContent()`).
- The offline generator in `src/engine/` is separate from Expo runtime. Engine-capable games: Takuzu, Nonogram.

## Key conventions

- Keep puzzle-specific logic inside the owning game package.
- In `definition.ts`, expose user-facing strings through getters (`get title()`, `get tagline()`) so language changes always read fresh values.
- Treat `src/games/<id>/content/i18n/` as the single source of truth for all user-visible copy. This includes line labels, cell labels, rule names, and plural forms used inside gameplay and technique explanation — never switch on language in gameplay code to build strings.
- When changing persisted puzzle/session shapes, update `activeSessionStateStorage.ts` normalization together with the change.
- Engine code uses `src/engine/tsconfig.json` — keep it free of Expo/React Native runtime dependencies.
- New games are wired in two registries: `src/app/shell/games/gameRegistry.ts` (app) and `src/engine/gameRegistry.ts` (engine, only if the game has a generator).
