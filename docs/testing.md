# Testing strategy

## Commands

- All unit tests (app + engine + cli): `./gradlew test`
- App module only: `./gradlew :app:testDebugUnitTest`
- Engine/CLI only (plain JVM, no emulator): `./gradlew :engine:test :cli:test`
- Puzzle data checks: `./gradlew :cli:bankTest` (40 to 60 minutes). The tests in `cli/src/bankTest` check
  every committed puzzle bank, Journey file and word list. Run it after generating, regrading or editing
  a bank, and after engine changes to solving or grading. CI runs it in the `Puzzle bank checks` workflow
  only when the banks, `cli/`, `engine/` or the Gradle build files change, or when started by hand.
- Shared module (Android host): `./gradlew :shared:testAndroidHostTest`
- Single class: `./gradlew :app:testDebugUnitTest --tests "com.quietgrid.app.games.sudoku.*"`
- Single engine class: `./gradlew :engine:jvmTest --tests "com.quietgrid.engine.sudoku.*"`. `:engine:test`
  is only an alias for `:engine:jvmTest` and rejects `--tests`.
- Compile check only (fast): `./gradlew compileDebugKotlin`

## Current coverage

- `:engine` — canonical puzzle rules, solving techniques, and difficulty scoring for sudoku, takuzu,
  nonogram, and wordsearch. The deepest and most systematic coverage in the repo
  (`engine/src/commonTest/kotlin/com/quietgrid/engine/`, shared by the JVM and iOS simulator runs).
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
    `<module>/build/reports/jacoco/test/html/index.html`. `:cli`'s `jacocoTestReport` has an explicit
    `dependsOn(tasks.named("test"))` — the community `jacoco` plugin does **not** auto-wire that
    dependency for plain `org.jetbrains.kotlin.jvm` modules the way it does for `java`/`java-library`
    projects, so running `jacocoTestReport` alone with no execution data present silently no-ops
    (shows as `SKIPPED`) rather than failing loudly. `:engine` is a Kotlin Multiplatform module, so
    its `jacocoTestReport` is registered by hand in `engine/build.gradle.kts`: it depends on
    `jvmTest`, reads `build/jacoco/jvmTest.exec` and the classes in `build/classes/kotlin/jvm/main`.
    Don't remove either module's `dependsOn`.
  - `:app`: uses AGP's own coverage integration instead (`enableUnitTestCoverage = true` on the
    `debug` build type in `app/build.gradle.kts`), which *does* correctly wire its own test
    dependency. Run `./gradlew :app:createDebugUnitTestCoverageReport` →
    `app/build/reports/coverage/test/debug/index.html`.

- **Dependency injection (plain constructors).** There is no Hilt or Dagger.
  - `AppGraph` (`app/src/main/java/com/quietgrid/app/AppGraph.kt`) builds every app-wide instance
    once, as a synchronized `by lazy` property: the DataStore, the Room database and its DAOs, the
    repositories, `AppForeground`, `DailyReminderScheduler`, `QuickPlaySource` and
    `QuickPlayWidgetUpdater`. `QuietGridApplication.graph` holds it.
  - Android entry points (the Activity, the worker, the widget) read it with `context.appGraph`.
    Composables read `LocalAppGraph.current`, which `MainActivity` provides around its content.
  - Screens create ViewModels with `appViewModel { graph -> XxxPlayViewModel(graph.sessionRepository,
    ..., difficulty, resume) }`, a thin wrapper over lifecycle's `viewModel { }`. It uses the current
    `ViewModelStoreOwner` (the navigation entry) and keys the ViewModel by class name, and the lambda
    runs only when the ViewModel is first created. Per-screen values such as `difficulty` and `resume`
    are plain constructor arguments. A ViewModel that needs saved state or navigation arguments takes
    `createSavedStateHandle()` from the lambda's `CreationExtras` receiver, never `SavedStateHandle()`.
  - `QuietGridWorkerFactory` builds `DailyReminderWorker` for WorkManager. WorkManager looks the
    worker up by its stored class name, so reminders scheduled by older builds keep firing.
  - Tests build ViewModels and repositories directly with their constructors and fakes or `mockk`; no
    DI framework is involved. A Robolectric test that needs the real wiring reads
    `ApplicationProvider.getApplicationContext<Context>().appGraph`. Composables under test that call
    `appViewModel` or read `LocalAppGraph` need
    `CompositionLocalProvider(LocalAppGraph provides context.appGraph)` (see
    `TabletStoreArtCaptureTest`), or the `setContentWithGraph` helper in
    `app/src/test/java/com/quietgrid/app/ui/TestGraph.kt`. `AppGraphTest` and
    `QuietGridWorkerFactoryTest` cover the wiring.
  - `AppArchitectureTest` fails in these cases:
    - an `:app` file imports `dagger.*`, `javax.inject.*` or `androidx.hilt.*`;
    - production code builds an empty `SavedStateHandle()`;
    - a ViewModel takes a `Context`;
    - a file in the portable set (see CLAUDE.md) imports `android.*`, `java.*`, `javax.*` or an
      `androidx.*` package outside the allowlist;
    - a file in the portable set calls a JVM-only API that needs no import: `System.`, `Charsets.`,
      `Math.`, `Thread.`, `synchronized(`, `::class.java`, `.javaClass`, `String.format`, or
      `@Volatile` without `import kotlin.concurrent.Volatile`.

- **Storage and platform services** (the iOS port's step 5+6):
  - **Migrations.** `QuietGridMigrationTest` builds each older database version with raw SQL
    through `AndroidSQLiteDriver` and opens it with the current Room setup from `:shared`, so every
    migration runs on real data. It uses `@Config(application = Application::class)` so the real
    app's startup does not open the same database file.
  - **Storage files.** `QuietGridStorageTest` checks that the DataStore keeps the file the old
    `preferencesDataStore` delegate used, and reads a value written there.
  - **Saved formats.** `SavedDateFormatTest` pins every stored date and time to a literal copied
    from what the app writes: the session envelope, badge dates, quiet tips, the reminder time,
    history and daily keys. If one of these breaks, users' saved data would decode differently.
  - **Small checks.**
    - `TodayTest`: `today()` across time zones.
    - `BankStreamingTest`: streamed banks match whole-file reads.
    - `SupportLinksTest`: `encodeUrlComponent` and the support link URLs.
    - `AndroidPlatformTest`: the Android platform implementations.

- **Store screenshots** — `TabletStoreArtCaptureTest`
  (`app/src/test/java/com/quietgrid/app/storeart/TabletStoreArtCaptureTest.kt`) is a Roborazzi capture
  class that seeds demo data and Robolectric-renders the real app at 7" and 10" tablet sizes into
  `tools/store-art/captures/<device>/*.png` for framing into store listing images. It's skipped in
  normal `./gradlew test` runs; run it explicitly with
  `./gradlew :app:recordRoborazziDebug --tests "com.quietgrid.app.storeart.TabletStoreArtCaptureTest"`.
  See [`tools/store-art/README.md`](../tools/store-art/README.md) for the full capture-to-store-art
  pipeline.

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
