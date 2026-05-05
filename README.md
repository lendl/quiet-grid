# Quiet Grid

Logic Games
A lightweight, privacy-first logic puzzle game for **Android**, built with React Native (Expo) and TypeScript.

## What is Quiet Grid?

Quiet Grid currently includes two offline puzzle types:

1. **Binary** — fill a grid with 0s and 1s using logic
2. **Minesweeper** — clear the grid without opening a mine

## Features

- Binary offers 3 puzzle sizes across 4 difficulty levels:
  - Easy: 6x6 and 8x8
  - Medium: 8x8 and 10x10
  - Hard: 8x8 and 10x10
  - Expert: 10x10
- Minesweeper offers 4 difficulty levels with board profiles tuned per level
- Real-time line feedback (with 800ms grace period after each tap)
- Per-row and per-column live status indicators
- Undo history, 3 hints per puzzle, reset
- Local statistics with win streaks and best times
- Dark and light themes (persisted locally)
- Themed dialogs — all popups match the current app theme
- Engine-generated puzzles reveal between 15% and 25% of cells, rounded down
- **Fully offline** — no internet connection required, ever
- **No ads. No payments. No accounts. No tracking. No data ever leaves your device.**

## Project Structure

```
src/
  app/                      # React Native / Expo app shell
    components/             #   Shared UI components
    context/                #   ThemeContext
    navigation/             #   Stack navigator
    shell/                  #   Registry, shared hooks, shared storage helpers
    screens/                #   Home, Difficulty, shared shell screens
    theme/                  #   Color tokens (dark + light)
    types.ts                #   App-wide compatibility types
    utils/                  #   Shared helpers and transitional utilities
  games/                    # Puzzle-type modules
    shared/                 #   Engine-safe puzzle primitives used by game packages
    binary/                 #   Binary puzzle package
      core/                 #     Engine-safe binary types, decoding, validation
      engine/               #     Binary engine plugin for shared CLI runner
    minesweeper/            #   Minesweeper puzzle package
      core/                 #     Engine-safe minesweeper types and rules
  engine/                   # Offline puzzle generator CLI (Node.js)
    gameDefinition.ts       #   Shared contract for engine-capable games
    gameRegistry.ts         #   Registry of installed engine plugins
    encoding.ts             #   Shared hex encode/decode utilities
    generator.ts            #   Binary grid generator (legacy Binary-specific helper)
    validator.ts            #   Binary validation helper
    solver.ts               #   Binary unique-solution counter
    mask.ts                 #   Binary mask generator
    db.ts                   #   SQLite deduplication tracking
    writer.ts               #   Shared catalog append/reset helpers
    index.ts                #   CLI entry point
    tsconfig.json           #   Standalone Node.js tsconfig (does not extend Expo)
```

## Getting Started

### Requirements

- [Node.js](https://nodejs.org/) 18+
- **Android emulator** via [Android Studio](https://developer.android.com/studio) (required), or a physical Android device with [Expo Go](https://expo.dev/client) installed

> This is an Android-only app. Running on iOS or web is not supported.

### Android Studio setup (emulator)

1. Download and install [Android Studio](https://developer.android.com/studio)
2. Open **Device Manager** → **+** → Create Virtual Device (e.g. Pixel 6, API 34)
3. Start the emulator

### Install & run

```bash
npm install
npm run android
```

The app will launch automatically in the running emulator. On a physical device, scan the QR code shown in the terminal with Expo Go.

## Generating binary puzzles

The engine uses a shared CLI runner in `src/engine/` plus game-owned engine plugins in `src/games/<id>/engine/`. Binary is the first engine-capable game today, and its generator appends puzzles to `src/games/binary/puzzles/all.ts`. Everything runs entirely offline — no network access, no external services.

```bash
npm run engine -- --game=binary
npm run engine -- --game=binary --size=8
```

Each run uses the selected game's engine plugin. Previously seen dedupe keys are tracked in `src/engine/puzzles.db` (local SQLite, gitignored) so duplicate generated puzzles are not written.

## Privacy

All game data (statistics, theme preference) is stored exclusively on-device using `AsyncStorage`. The app is fully offline — no ads, no analytics, no telemetry, no network requests of any kind are made.

Read the full [Privacy Policy](PRIVACY.md).

## Template direction

Quiet Grid is evolving into a reusable shell for offline grid-based puzzle types. Shared navigation, storage envelopes, and shell hooks live in `src/app/shell/`, while puzzle-specific rules, codecs, tutorials, and metadata live in `src/games/<id>/`.

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
