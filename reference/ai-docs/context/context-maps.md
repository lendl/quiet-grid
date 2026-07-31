# Context Maps

Use these packets when AI is editing one subsystem and should not load the whole repository mentally at once.

## Game registration

### User goal
- Expose the game to the app shell with the correct title, tagline, and beta flag.

### Architecture goal
- `core/GameCatalog.kt` is the single source of truth for installed games — there is no separate engine
  registry in this app (unlike the old RN app's split app/engine registries).

### File map
- `app/src/main/java/com/quietgrid/app/core/GameCatalog.kt`
- `app/src/main/java/com/quietgrid/app/core/GameId` (enum in the same file)
- `res/values*/strings.xml` — title/tagline string resources referenced by `GameMeta`

### Mistakes to avoid
- Do not create a second registry for engine-backed games — one catalog covers all games.

## Gameplay core

### User goal
- Make puzzle rules truthful and consistent.

### Architecture goal
- Session state and persistence live in `<Id>ViewModel.kt`. Board validation and rule logic live in
  `<Id>Logic.kt`. Data shapes live in `<Id>Models.kt`.

### File map
- `app/src/main/java/com/quietgrid/app/games/<id>/<Id>ViewModel.kt`
- `app/src/main/java/com/quietgrid/app/games/<id>/<Id>Logic.kt`
- `app/src/main/java/com/quietgrid/app/games/<id>/<Id>Models.kt`
- `engine/src/main/kotlin/com/quietgrid/engine/<game>/` — canonical rules, when the game has shipped a shared
  engine module (sudoku, takuzu, nonogram, wordsearch as of 2026-07-29)

### Mistakes to avoid
- Do not move truth-bearing puzzle rules into a Composable.
- Do not duplicate rule logic that already lives in `engine/` — check there first for the four ported games.

## Canonical moves

See [[project moves|moves.md]].

## Support actions

Support actions (optional, non-canonical player actions like Minesweeper flagging) are documented alongside
canonical moves; see `moves.md`. They live in the same `<Id>Logic.kt` / `<Id>ViewModel.kt` files as canonical
moves — there is no separate file split for them.

## Mistake policy

See `mistake-policy.md`.

## Feedback effects

See `feedback-effects.md`.

## Difficulty system

See `difficulties.md`.

## Play screen and board

### User goal
- Give the player a consistent play surface: header controls, optional hint card, zoomable board.

### Architecture goal
- Every game's Play screen composes: header row (back button, difficulty/progress, hint toggle, end-puzzle)
  → optional hint card → `ZoomableBoardSurface { GameGrid(...) }` inside `Box(Modifier.weight(1f))`.

### File map
- `app/src/main/java/com/quietgrid/app/games/<id>/<Id>PlayScreen.kt` — screen composition + chrome
- `app/src/main/java/com/quietgrid/app/games/<id>/<Id>Grid.kt` — the board composable
- `app/src/main/java/com/quietgrid/app/ui/components/ZoomableBoardSurface.kt`
- `app/src/main/java/com/quietgrid/app/ui/components/GameBackButton.kt`
- `app/src/main/java/com/quietgrid/app/ui/components/AppTopBar.kt`

### Mistakes to avoid
- Do not deviate from the header-row-then-weighted-board layout without a reason — it is used by every game.

## Engine vs non-engine mode

### User goal
- Produce puzzles with a real solve path and valid difficulty ladder where possible.

### Architecture goal
- Engine-backed games (sudoku, takuzu, nonogram, wordsearch) keep canonical rules/techniques/difficulty
  scoring in `engine/` (pure Kotlin/JVM, no Android deps) and generation in `cli/`.
- Non-engine games (e.g. minesweeper, chimptest) keep all logic in the `app/` game package; puzzles/rounds
  are generated on-device or hand-authored, not via `cli/`.

### File map
- `engine/src/main/kotlin/com/quietgrid/engine/<game>/`
- `cli/src/main/kotlin/com/quietgrid/cli/<game>/`
- `app/src/main/assets/<id>_puzzles.json` — generated output, committed, loaded by `<Id>PuzzleBank.kt`
- `docs/superpowers/specs/2026-07-24-shared-puzzle-engine-design.md` — design rationale

### Mistakes to avoid
- Do not ship engine-backed puzzles without uniqueness, difficulty, and reclassification rules — see
  [[project_shared_puzzle_engine]] for known gaps (dedupe seeding, audit logging) to be aware of, not to
  silently repeat.
- Do not write generator code that could run on-device — `cli/` never ships in the APK.

## Player guidance (How to Play / next-move hints)

See `learning-center.md` — the old tutorial/analyzer system is retired.

## Content and copy

### User goal
- Keep all game-facing copy consistent, translatable, and terminology-consistent with existing locales.

### Architecture goal
- All player-visible copy lives in `res/values*/strings.xml` — never hardcoded in a Composable.

### File map
- `app/src/main/res/values/strings.xml` (English, source of truth for new keys)
- `app/src/main/res/values-*/strings.xml` (other locales)

### Mistakes to avoid
- Do not hardcode game-facing copy in screens or components.
- Do not introduce new terminology inconsistent with existing entries — match established terms (puzzle,
  unfinished, lost, grid, etc).
- Never use curly/smart quotes in Kotlin or string resources — straight ASCII quotes only.

## Persistence and platform

### User goal
- Resume an in-progress session safely and consistently; only one game can have an in-progress puzzle at a
  time.

### Architecture goal
- Each game's ViewModel serializes a `*PersistedSession` data class (kotlinx.serialization) into
  `SessionRepository`'s single active-session slot (DataStore-backed).

### File map
- `app/src/main/java/com/quietgrid/app/games/<id>/<Id>ViewModel.kt` — `*PersistedSession` shape
- `app/src/main/java/com/quietgrid/app/data/SessionRepository.kt`
- `app/src/main/java/com/quietgrid/app/data/AppDataStore.kt`
- `app/src/main/java/com/quietgrid/app/data/StatsRepository.kt` — per game/difficulty stats
- `app/src/main/java/com/quietgrid/app/data/SettingsRepository.kt` — theme/language/timer/beta-games settings

### Mistakes to avoid
- Do not scatter persistence rules across screens — it belongs in the ViewModel + repository layer.
- Remember: changing a `*PersistedSession` shape degrades gracefully (decode failure -> start fresh) but
  silently drops any in-progress session across the change — that is accepted, not a bug to fix.

## Engine generation

### User goal
- Produce puzzles with a real solve path and valid difficulty ladder.

### Architecture goal
- Generation is offline-only, invoked manually, and writes directly into committed asset JSON — it never
  runs on-device.

### File map
- `cli/src/main/kotlin/com/quietgrid/cli/<game>/`
- Command: `./gradlew :cli:run --args="generate --game <id> --difficulty <easy|medium|hard|expert> --count <n> --out app/src/main/assets"`
- `app/src/main/assets/<id>_puzzles.json`

### Mistakes to avoid
- Do not ship engine-backed puzzles without uniqueness, difficulty, and reclassification rules.
