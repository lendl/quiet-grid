# Quiet Grid — Claude Instructions

Quiet Grid is a native Android app (Kotlin, Jetpack Compose). The repo root is the Gradle project root.

## Commands

- Build debug APK: `./gradlew assembleDebug`
- Install + run on a connected device/emulator: `./gradlew installDebug`
- Type/compile check (fast, no packaging): `./gradlew compileDebugKotlin`
- Lint: `./gradlew lint`
- Static analysis (report-only, never fails the build): `./gradlew detekt` (config `config/detekt/detekt.yml`, reports in `<module>/build/reports/detekt/`)
- Unit tests: `./gradlew testDebugUnitTest`

## Shared puzzle engine

- `engine/` — Kotlin Multiplatform module (JVM + iOS targets, no Android deps; shared code in `src/commonMain`, the few platform pieces in `src/jvmMain`/`src/iosMain`) holding canonical puzzle rules, solving techniques, and difficulty scoring for sudoku, takuzu, nonogram, and wordsearch. `app/` depends on it for live hints/validation; `cli/` depends on it for offline generation. See `docs/superpowers/specs/2026-07-24-shared-puzzle-engine-design.md`.
- `cli/` — offline puzzle generator (`./gradlew :cli:run --args="generate --game <sudoku|takuzu|nonogram|wordsearch> --difficulty <easy|medium|hard|expert> --count <n> --out shared/src/commonMain/composeResources/files"`). Arrow Escape also takes `--shape <id|all>` to build silhouette boards from `cli/src/main/resources/arrowescape_silhouettes.json` (shape authoring rules: `docs/arrowescape-silhouette-guidelines.md`). Writes/merges generated puzzles directly into the committed `*_puzzles.json` asset files `*PuzzleBank.kt` already loads — never ships in the APK, never runs on-device.
- `shared/` — Compose Multiplatform module (Android target always, iOS targets only with `-Pquietgrid.ios=true`) that app code moves into during the iOS port; `iosApp/` holds the XcodeGen spec (`project.yml`, the `.xcodeproj` is generated on CI) and the SwiftUI host. `compileDebugKotlin` does not cover `:shared`: run `./gradlew :shared:testAndroidHostTest`. See `docs/ios-port.md`.
- Seasonal (Halloween, Oct 31 only, gated by `core/Seasonal.kt` + the "Seasonal touches" setting) puzzles live in their own banks: `arrowescape_halloween_puzzles.json` (silhouettes with `"season": "halloween"`) and `wordsearch_halloween_puzzles_<tier>.json` (`--season halloween`, words in `cli/src/main/resources/seasonal_themes_halloween.json`). Never merge them into the main banks.
- Fast unit tests for rules/techniques/generators: `./gradlew :engine:test :cli:test` (plain JVM, no emulator needed). Checks over the committed data (every bank, Journey file and word list: the `*BankVerificationTest` classes and the like) live in `cli/src/bankTest`, not `cli/src/test`, and run with `./gradlew :cli:bankTest` (40 to 60 minutes). Run it after generating, regrading or editing a bank, and after engine changes to solving or grading. CI runs it in the `Puzzle bank checks` workflow (`.github/workflows/bank-checks.yml`), only when banks, `cli/`, `engine/` or Gradle build files change. To filter engine tests use `./gradlew :engine:jvmTest --tests "..."`: `:engine:test` is only an alias and rejects `--tests`. Engine tests live in `engine/src/commonTest` and use `kotlin.test`, so they also run on the iOS simulator (`./gradlew -Pquietgrid.ios=true :engine:iosSimulatorArm64Test`, macOS only; CI runs it). iOS targets are only declared when `-Pquietgrid.ios=true` is passed, so default builds and F-Droid never download Kotlin/Native (quote it in PowerShell: `'-Pquietgrid.ios=true'`); see `docs/ios-port.md`. Engine code must stay platform-neutral: no `java.*` APIs in `commonMain`, put any platform call behind `expect`/`actual`.
- New games' move-finding, solving-technique, and validity logic belongs in `:engine` first
  (`engine/src/commonMain/kotlin/com/quietgrid/engine/<game>/`), with the `:app` layer delegating to it
  rather than re-deriving board-scanning or validity logic locally in `games/<id>/`. Takuzu's
  `:app` hint code drifted into exactly this (see `TakuzuNextMove.kt`'s history) before being
  brought back in line — treat that as the canonical example of what not to repeat. This
  convention is enforced at review time, not by a test: cross-module semantic duplication isn't
  something static analysis can detect. (What *is* mechanically enforced: `:engine` has zero
  Android dependencies, checked by `EngineArchitectureTest` in `:engine`'s `jvmTest` source set; JVM-only APIs in shared engine code fail the iOS compile.)

## After making changes

Always run `./gradlew compileDebugKotlin` after editing Kotlin files and fix any errors before finishing. Follow with `./gradlew assembleDebug` to confirm the full build (resources, manifest, packaging) still succeeds.

The user tests builds on their own device — don't run `adb install`/screenshot/logcat verification loops unless explicitly asked to check something on-device (e.g. diagnosing a reported crash).

## Kotlin file conventions

- Use straight ASCII quotes (`'` and `"`) — never curly/smart quotes. This applies especially to locale string resources containing non-English text.
- **No code documentation.** Don't write KDoc or explanatory comments in Kotlin source — not even for non-obvious WHY-level rationale. A durable finding worth keeping (e.g. a diagnostic result, a design tradeoff, a root-cause) belongs in project memory instead, not inline in code. Code should read as plain, self-explanatory logic with well-named identifiers carrying the intent.

## Localization during development

New games and new features stay English-only while unstable: add strings only to the default `shared/src/commonMain/composeResources/values/strings.xml`, skip every other locale's `values-*/strings.xml` there entirely until the feature is ready to ship. Android falls back to the default string for any key missing in a locale, so this never crashes or shows a blank string. Once the feature is ready to ship, do one batch translation pass covering all its new keys across all locales — don't translate per-session while the feature is still in flux.

## Terminology

When writing player-facing copy, string resources, loss screens, or how-to-play text, keep the established terms (puzzle, unfinished, lost, grid, etc.) and game title localization consistent with existing `values-*/strings.xml` entries.

## Architecture

- `app/src/main/java/com/quietgrid/app/` is the single module (`applicationId` / `namespace` = `com.quietgrid.app`).
- No Hilt or Dagger. `AppGraph.kt` holds every app-wide instance (DataStore, Room, repositories, services) as a lazy property; Android entry points read `context.appGraph`, composables `LocalAppGraph.current`. Screens create ViewModels with `appViewModel { graph -> X(graph.sessionRepository, ..., difficulty, resume) }`, and `QuietGridWorkerFactory` builds WorkManager workers.
- Platform services are interfaces in `:shared` `com.quietgrid.app.platform` (`BundledFiles`, `AppLanguage`, `MotionSettings`, `Haptics`, `ShareSheet`, `AppInfo`), with Android implementations in `:app` `platform/` and iOS ones in `:shared` iosMain; `AppGraph` holds one of each. The Room database, its DAOs and the DataStore setup live in `:shared` (`data/`). Puzzle banks read through `BundledFiles`, never `Context`.
- Portable set: `data/`, `core/`, `session/`, each game's `*ViewModel`/`*PuzzleBank`/`*Logic`/`*Models` files and every `ui/**/*ViewModel` must not import `android.*`, `java.*` or `javax.*` (only an `androidx.*` allowlist), because they move to `:shared` for iOS. Use kotlinx-datetime, `kotlin.time`, okio and the platform interfaces instead. `AppArchitectureTest` enforces the imports, catches common JVM-only calls that need no import (`System.`, `Charsets.`, `Math.`, `@Volatile` without `kotlin.concurrent.Volatile`, and a few more), and checks that no ViewModel takes a `Context`. The iOS compile in step 7 catches anything it misses.
- Navigation uses Navigation Compose with type-safe `@Serializable` routes in `nav/Destinations.kt` (enums used as route args need `@Keep` for R8). `nav/AppNavHost.kt` holds the shell (drawer, top/bottom bars, tabs) and wires the area graphs: `ShellGraphs.kt` (game page, journey, mixes, account), `PlayGraph.kt` (play, completion, loss, analyzer) and `ModeGraphs.kt` (Challenger, Endless). Shared navigation actions live in `nav/AppNavigator.kt`. Game-specific result details travel as a `CompletionHighlight`, never as extra route args.
- Games are plugin-like modules under `games/<id>/` (e.g. `games/takuzu/`), each with its own `*ViewModel.kt` (session state, persistence), `*Grid.kt` (the board composable), `*PlayScreen.kt` (screen composition + chrome), and `*Strings.kt`/`*Logic.kt`/`*Models.kt`/`*PuzzleBank.kt` as needed.
- The shell registry at `core/GameCatalog.kt` is the source of truth for installed games (id, title, tagline, beta flag).
- Each game also has a `games/<id>/<Name>Definition.kt` object implementing `GameDefinition` (`games/GameDefinition.kt`): capabilities, quick start, icon, difficulty labels, play screen, loss copy overrides, and an optional `ChallengerSpec`. They are listed in `games/GameRegistry.kt`; shared screens call `GameRegistry.of(gameId)` instead of switching on `GameId` or keeping `Set<GameId>` lists. `GameRegistryTest` fails if a `GameId` has no definition.
- Shared chrome lives in `ui/components/`: `GlobalMenu.kt` (top brand bar), `BottomNavBar.kt` (bottom tab bar), `AppTopBar.kt` (back-mode bar for sub-pages), `ZoomableBoardSurface.kt` (pinch-zoom + pan wrapper for boards), `GameBackButton.kt`, `BoardEntrance.kt`, `FeedbackText.kt` (per-cell correct/incorrect spin-shake).
- Each game's Play screen composes `GamePlayScaffold` (`ui/play/`): top bar (back, centered timer, hint, rules peek, ⋮ menu with End puzzle) → info strip → board (`PuzzleBoardContainer { GameGrid(...) }`) → dock, with the hint card and rules peek as overlays. See `docs/play-screen-standards.md`; every game uses it.
- The per-game page (`ui/screens/PuzzlePickerScreen.kt`) hosts four tabs — Play (difficulty picker), Rules (`HowToPlayScreen.kt`), Stats (`StatsOverview.kt` scoped to one game), Settings (`GameSettingsTab.kt`: per-game play preferences, plus theme selection for themed games) — matching the shared `GlobalMenu` + `BottomNavBar` chrome used on the main tabs.
- Persisted session state: each game's ViewModel serializes a `*PersistedSession` data class (kotlinx.serialization) into `SessionRepository`'s single active-session slot (DataStore-backed). Only one game can have an in-progress puzzle at a time.
- Stats are tracked per game/difficulty in `StatsRepository`, aggregated across games for the main Stats tab and scoped to one game for the per-game Stats tab (`ui/screens/StatsOverview.kt`).
- Settings (theme mode, language, timer visibility, beta games toggle) live in `SettingsRepository`, also DataStore-backed. Play preferences (timer, hint button, rotation, keep screen awake, vibration, check lines as you go, reduce motion, left-handed layout, pinch to zoom, confirm End puzzle) have an app-wide default there plus optional per-game overrides in `GamePreferencesRepository`, shown on the per-game Settings tab; see "Player preferences" in `docs/play-screen-standards.md`.
- Theming: `ui/theme/Theme.kt` defines Dark/Light/Pencil (colorblind-friendly grayscale) color schemes plus the bundled Plus Jakarta Sans font for headings. `LocalIsPencilTheme` lets game boards branch to grayscale variants where hardcoded color would otherwise bypass the pencil theme.
- The offline puzzle generator that used to live in the React Native app's `src/engine/` has no equivalent here yet — puzzle banks are bundled as static resources per game (`*PuzzleBank.kt`), stored in `shared/src/commonMain/composeResources/files/` (Compose resources, shared with iOS); `:app` packages that folder as its assets. A build check fails if anything is put back in `app/src/main/assets/` or `app/src/main/res/values*/strings.xml`.

## Key conventions

- Keep puzzle-specific logic inside the owning game package (`games/<id>/`).
- Treat `shared/src/commonMain/composeResources/values*/strings.xml` as the single source of truth for all user-visible copy — never hardcode strings in Composables. Write apostrophes and double quotes there plain (`'`, `"`), never Android-style `\'` or `\"`: iOS shows the backslash. `:app`'s `syncSharedStrings` task adds the Android escapes and fails the build on a backslash-escaped quote.
- When changing a persisted session's data class shape, existing saved sessions may fail to decode; `SessionRepository`/ViewModel `restoreOrCreate()` paths already treat decode failures as "start fresh," so this degrades gracefully rather than crashing — just be aware resuming an in-progress puzzle across the change will silently drop it.
- New games register in `core/GameCatalog.kt` (meta) and `games/GameRegistry.kt` (definition). Challenger is generic: a game supplies a `ChallengerSpec` (config, play screen, copy overrides) and a `ChallengerSession<P>` typealias; shared copy uses `challenger_*`, `difficulty_*` and `loss_abandoned_*` keys, with per-game keys only where wording differs.
- Games must not import other game packages; `AppArchitectureTest` holds the list of existing exceptions and fails on new ones.
- Every play screen (new or changed) must follow `docs/play-screen-standards.md`: built on `GamePlayScaffold`, same five slots (top bar, info strip, board, dock, overlays), the grid never moves and is never shrunk for optional content, and it works in all six layouts (small/normal phone upright and sideways, tablet landscape and upright). Read it before touching any `*PlayScreen.kt`.
- Permissions are deny-by-default. The only allowed permissions are listed in `allowedPermissions` in `app/build.gradle.kts`; `checkReleasePermissions` (run by every `assemble` and in CI) fails on anything else, including permissions merged in from libraries. Strip unneeded library permissions with `tools:node="remove"` in `AndroidManifest.xml`. Adding a permission to the allowlist needs a stated reason in the PR.
- This app was ported from a React Native/Expo version of the same game (now removed from this repo) — when in doubt about intended behavior, wording, or UX for a feature, prefer matching what's described in past session context/commit messages over inventing new behavior.
