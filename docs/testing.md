# Testing strategy

## Commands

- All unit tests (app + engine + cli): `./gradlew test`
- App module only: `./gradlew :app:testDebugUnitTest`
- Engine/CLI only (plain JVM, no emulator): `./gradlew :engine:test :cli:test`
- Single class: `./gradlew :app:testDebugUnitTest --tests "com.quietgrid.app.games.sudoku.*"`
- Compile check only (fast): `./gradlew compileDebugKotlin`

## Current coverage

- `:engine` — canonical puzzle rules, solving techniques, and difficulty scoring for sudoku, takuzu,
  nonogram, and wordsearch. The deepest and most systematic coverage in the repo
  (`engine/src/test/kotlin/com/quietgrid/engine/`).
- `:cli` — offline puzzle generators, arg parsing, JSON writers (`cli/src/test/kotlin/com/quietgrid/cli/`).
- `:app` data layer — `SessionRepository`/`StatsRepository`/`SettingsRepository`
  (`app/src/test/java/com/quietgrid/app/data/`). These take an injected `DataStore<Preferences>`
  rather than a `Context`, so tests run against a real DataStore backed by a JUnit `TemporaryFolder`
  — no Robolectric or emulator needed.
- `:app` session layer — `PuzzleSessionController` (the shared timer/save/finish state machine every
  game ViewModel wraps) and all 8 `*PlayViewModel` classes
  (`app/src/test/java/com/quietgrid/app/games/<id>/<Id>PlayViewModelTest.kt`): session-start wiring,
  `endPuzzle` → abandoned loss, and — where a fixture makes it deterministic — a real win path.
- `:app` per-game pure logic — every game now has at least one `*LogicTest.kt`
  (`app/src/test/java/com/quietgrid/app/games/<id>/`); Minesweeper, Sudoku, and Takuzu additionally
  split out `*RulesTest`/`*ValidationTest` and `*NextMoveTest` files where those are meaningfully
  separate concerns from the core session logic. Next-move hint algorithms are tested lightly
  (guard clauses plus 1-2 hand-verified fixtures) rather than exhaustively per-technique, except
  where the technique isn't covered by `:engine` at all (Minesweeper's constraint-solving hint
  logic and Takuzu's recovery-hint detection are both app-layer-only, so those got more careful
  fixture work — see the `GUARANTEED_SAFE_TILE` test in `MinesweeperNextMoveTest` and the
  triple/balance/duplicate-mismatch tests in `TakuzuNextMoveTest` for the pattern to extend).

- `:app` Compose behavior tests — run on the JVM via Robolectric (no emulator), living in `src/test`
  per the project convention (`androidTest` is reserved for real instrumented/device tests, which
  this repo doesn't have yet). `BottomNavBarTest` (`app/src/test/java/com/quietgrid/app/ui/components/`)
  is the first one and the template to copy for more: `@RunWith(RobolectricTestRunner::class)`,
  `createAndroidComposeRule<ComponentActivity>()`, semantic matchers (`onNodeWithText`) sourced from
  real string resources via `composeRule.activity.getString(...)` rather than hardcoded copy.

- `:app` screenshot tests — Compose Preview Screenshot Testing tool (host-side, LayoutLib-based, no
  emulator). Live in the `screenshotTest` source set, e.g.
  `app/src/screenshotTest/kotlin/com/quietgrid/app/ui/components/BottomNavBarScreenshotTest.kt`:
  a set of `@PreviewTest`-annotated `@Preview` composables (Light/Dark/Pencil theme + one large-font
  variant), wrapped in the real `QuietGridTheme`. Reference images live in
  `app/src/screenshotTestDebug/reference/` (committed to the repo, not gitignored — they're the
  golden images tests diff against).
  - Generate/update references: `./gradlew :app:updateDebugScreenshotTest`
  - Validate against references: `./gradlew :app:validateDebugScreenshotTest`
  - HTML diff report on failure: `app/build/reports/screenshotTest/preview/debug/index.html`

- **Coverage (Jacoco)** — all three modules have the `jacoco` plugin applied.
  - `:engine` / `:cli`: `./gradlew :engine:jacocoTestReport :cli:jacocoTestReport` →
    `<module>/build/reports/jacoco/test/html/index.html`. Both modules' `jacocoTestReport` task has
    an explicit `dependsOn(tasks.named("test"))` — the community `jacoco` plugin does **not**
    auto-wire that dependency for plain `org.jetbrains.kotlin.jvm` modules the way it does for
    `java`/`java-library` projects, so running `jacocoTestReport` alone with no execution data present
    silently no-ops (shows as `SKIPPED`) rather than failing loudly. Don't remove that `dependsOn`.
  - `:app`: uses AGP's own coverage integration instead (`enableUnitTestCoverage = true` on the
    `debug` build type in `app/build.gradle.kts`), which *does* correctly wire its own test
    dependency. Run `./gradlew :app:createDebugUnitTestCoverageReport` →
    `app/build/reports/coverage/test/debug/index.html`.

- **Dependency injection (Hilt).** `AppContainer` (the old manual singleton locator) is gone;
  `QuietGridApplication` is `@HiltAndroidApp`, `MainActivity` is `@AndroidEntryPoint`.
  - The 3 repositories (`SessionRepository`/`StatsRepository`/`SettingsRepository`) are
    `@Singleton class ... @Inject constructor(dataStore: DataStore<Preferences>)`; the DataStore
    itself comes from `AppModule` (`@Provides @Singleton`). `SessionStore`/`StatsStore` (the
    interfaces `PuzzleSessionController` and the ViewModels actually depend on) are bound to their
    impls via `RepositoryBindingsModule` (`@Binds`).
  - Screens that just read/write settings/stats/session state without their own ViewModel (e.g.
    `StatsScreen`, `SettingsScreen`, `GamesScreen`, `AppNavHost`) call
    `hiltViewModel<RepositoriesViewModel>()` — a thin `@HiltViewModel` that just exposes the 3
    repositories, the direct architectural replacement for what `AppContainer.x` used to do. Safe to
    call from multiple independent Compose scopes (e.g. `AppNavHost`'s Activity scope vs a tab
    screen's nav-entry scope) since the repositories underneath are `@Singleton` regardless of how
    many `RepositoriesViewModel` instances exist.
  - All 8 `*PlayViewModel`s use **assisted injection** (`@AssistedInject`/`@AssistedFactory`), since
    `requestedDifficulty`/`resume` are per-navigation values Hilt can't provide on its own; the 5
    that read a bundled puzzle asset take `@ApplicationContext appContext: Context` directly instead
    of a screen passing `LocalContext.current`. Call sites use
    `hiltViewModel<XxxPlayViewModel, XxxPlayViewModel.Factory>(creationCallback = { it.create(difficulty, resume) })`
    (needs `androidx.hilt:hilt-navigation-compose` 1.2.0+ for the `creationCallback` param).
  - **Real gotcha hit and fixed**: Hilt/Dagger 2.52's bundled annotation processor uses a jarjarred
    `kotlinx-metadata-jvm` that only parses Kotlin metadata up to version 2.1.0 — Kotlin 2.2.10 (this
    project's compiler) emits metadata version 2.2.0, so `kaptDebugKotlin` failed outright
    (`Provided Metadata instance has version 2.2.0, while maximum supported version is 2.1.0`) before
    generating anything. Fixed by bumping to Hilt 2.60.1 (check Maven Central for whatever's current
    if this recurs after a future Kotlin bump — don't assume the pinned version here stays compatible
    forever).

## Not yet covered

Instrumented (`androidTest`) tests, end-to-end tests. Only one component has a behavior test
(`BottomNavBar`) and one has a screenshot test (`BottomNavBar` again) — everything else is
untested at those layers.

## Robolectric SDK pin

Robolectric 4.14 only supports up to API 35, but the app's `compileSdk`/`targetSdk` is 36 — without
a pin, any Robolectric test fails to even initialize (`Package targetSdkVersion=36 > maxSdkVersion=35`).
`app/src/test/resources/robolectric.properties` sets `sdk=35` globally so individual test classes
don't need a per-class `@Config(sdk = [35])`. Bump this file (or remove it) once Robolectric ships
API 36 support.

## Test infrastructure specifics

- **Fakes over mocks by default.** `app/src/test/java/com/quietgrid/app/testutil/FakeStores.kt` holds
  shared `FakeSessionStore`/`FakeStatsStore` fakes of the `SessionStore`/`StatsStore` interfaces,
  reused across `PuzzleSessionControllerTest` and every game's ViewModel test.
- **MockK is used only where a fake isn't possible.** Five of the eight games' ViewModels take a real
  `android.content.Context` (to read a bundled puzzle asset via `*PuzzleBank.randomPuzzle(...)`), which
  can't be hand-faked. The pattern: `mockkObject(XxxPuzzleBank)` + `coEvery { XxxPuzzleBank.randomPuzzle(...) } returns <fixture>`,
  with `Context` itself passed as `mockk<Context>(relaxed = true)` since it's never actually invoked.
- **Never wrap a `*PlayViewModel` test in `runTest { }` / use `backgroundScope`.** Every game ViewModel
  wires `PuzzleSessionController` into `viewModelScope`, and `PuzzleSessionController.runTicker()` is a
  deliberate `while(true) { delay(1000); ... }` loop that runs for as long as a puzzle is open — that's
  correct production behavior, not a bug. `kotlinx-coroutines-test`'s `runTest` unconditionally drains
  `Dispatchers.Main` at the end of every test (regardless of what dispatcher you pass it as context), so
  if `Dispatchers.Main` is set to a `TestDispatcher` and a ViewModel's ticker is running on it, that final
  drain never terminates — the whole test JVM spins at ~100% CPU forever. Confirmed via a `jstack` thread
  dump stuck in `TestCoroutineScheduler.advanceUntilIdleOr` → `PuzzleSessionController.runTicker`.

  Instead, every `*PlayViewModelTest` uses a plain (non-suspend) `@Test fun`, installs
  `Dispatchers.Main` via `MainDispatcherRule` (`app/src/test/java/com/quietgrid/app/MainDispatcherRule.kt`,
  built on `UnconfinedTestDispatcher()`), collects `SharedFlow`s on `CoroutineScope(mainDispatcherRule.dispatcher)`
  instead of `backgroundScope`, and advances time by calling `mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(x)`
  / `.runCurrent()` directly on the `TestCoroutineScheduler` — never the ambient `TestScope` extensions,
  and never `advanceUntilIdle()` in any form.
