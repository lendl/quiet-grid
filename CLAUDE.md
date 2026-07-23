# Quiet Grid — Claude Instructions

Quiet Grid is a native Android app (Kotlin, Jetpack Compose). The repo root is the Gradle project root.

## Commands

- Build debug APK: `./gradlew assembleDebug`
- Install + run on a connected device/emulator: `./gradlew installDebug`
- Type/compile check (fast, no packaging): `./gradlew compileDebugKotlin`
- Lint: `./gradlew lint`
- Unit tests: `./gradlew testDebugUnitTest`

## After making changes

Always run `./gradlew compileDebugKotlin` after editing Kotlin files and fix any errors before finishing. Follow with `./gradlew assembleDebug` to confirm the full build (resources, manifest, packaging) still succeeds.

The user tests builds on their own device — don't run `adb install`/screenshot/logcat verification loops unless explicitly asked to check something on-device (e.g. diagnosing a reported crash).

## Kotlin file conventions

- Use straight ASCII quotes (`'` and `"`) — never curly/smart quotes. This applies especially to locale string resources containing non-English text.

## Terminology

When writing player-facing copy, string resources, loss screens, or how-to-play text, keep the established terms (puzzle, unfinished, lost, grid, etc.) and game title localization consistent with existing `values-*/strings.xml` entries.

## Architecture

- `app/src/main/java/com/quietgrid/app/` is the single module (`applicationId` / `namespace` = `com.quietgrid.app`).
- Navigation is centralized in `nav/AppNavHost.kt`, using Navigation Compose. Routes are defined in `nav/Routes.kt`.
- Games are plugin-like modules under `games/<id>/` (e.g. `games/takuzu/`), each with its own `*ViewModel.kt` (session state, persistence), `*Grid.kt` (the board composable), `*PlayScreen.kt` (screen composition + chrome), and `*Strings.kt`/`*Logic.kt`/`*Models.kt`/`*PuzzleBank.kt` as needed.
- The shell registry at `core/GameCatalog.kt` is the source of truth for installed games (id, title, tagline, beta flag).
- Shared chrome lives in `ui/components/`: `GlobalMenu.kt` (top brand bar), `BottomNavBar.kt` (bottom tab bar), `AppTopBar.kt` (back-mode bar for sub-pages), `ZoomableBoardSurface.kt` (pinch-zoom + pan wrapper for boards), `GameBackButton.kt`, `BoardEntrance.kt`, `FeedbackText.kt` (per-cell correct/incorrect spin-shake).
- Each game's Play screen composes: header row (back button, difficulty/progress, hint toggle, end-puzzle) → optional hint card → `ZoomableBoardSurface { GameGrid(...) }` in a `Box(Modifier.weight(1f))` for centering.
- The per-game page (`ui/screens/PuzzlePickerScreen.kt`) hosts three tabs — Play (difficulty picker), Rules (`HowToPlayScreen.kt`), Stats (`StatsOverview.kt` scoped to one game) — matching the shared `GlobalMenu` + `BottomNavBar` chrome used on the main tabs.
- Persisted session state: each game's ViewModel serializes a `*PersistedSession` data class (kotlinx.serialization) into `SessionRepository`'s single active-session slot (DataStore-backed). Only one game can have an in-progress puzzle at a time.
- Stats are tracked per game/difficulty in `StatsRepository`, aggregated across games for the main Stats tab and scoped to one game for the per-game Stats tab (`ui/screens/StatsOverview.kt`).
- Settings (theme mode, language, timer visibility, beta games toggle) live in `SettingsRepository`, also DataStore-backed.
- Theming: `ui/theme/Theme.kt` defines Dark/Light/Pencil (colorblind-friendly grayscale) color schemes plus the bundled Plus Jakarta Sans font for headings. `LocalIsPencilTheme` lets game boards branch to grayscale variants where hardcoded color would otherwise bypass the pencil theme.
- The offline puzzle generator that used to live in the React Native app's `src/engine/` has no equivalent here yet — puzzle banks are bundled as static resources per game (`*PuzzleBank.kt`).

## Key conventions

- Keep puzzle-specific logic inside the owning game package (`games/<id>/`).
- Treat `res/values*/strings.xml` as the single source of truth for all user-visible copy — never hardcode strings in Composables.
- When changing a persisted session's data class shape, existing saved sessions may fail to decode; `SessionRepository`/ViewModel `restoreOrCreate()` paths already treat decode failures as "start fresh," so this degrades gracefully rather than crashing — just be aware resuming an in-progress puzzle across the change will silently drop it.
- New games register in `core/GameCatalog.kt` (app-wide catalog) — no separate engine registry exists in this app.
- This app was ported from a React Native/Expo version of the same game (now removed from this repo) — when in doubt about intended behavior, wording, or UX for a feature, prefer matching what's described in past session context/commit messages over inventing new behavior.
