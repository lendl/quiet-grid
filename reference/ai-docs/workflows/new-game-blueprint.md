# New Game Blueprint

## Package shape

```text
app/src/main/java/com/quietgrid/app/games/<id>/
  <Id>ViewModel.kt      # session state, persistence, finishAsWin()/finishAsLoss()
  <Id>Grid.kt            # the board composable
  <Id>PlayScreen.kt      # screen composition + chrome
  <Id>Logic.kt           # rules, validation, canonical moves, mistake checks
  <Id>Models.kt           # data classes / value types
  <Id>PuzzleBank.kt       # loads puzzles from app/src/main/assets/<id>_puzzles.json (engine-backed games)
  <Id>NextMove.kt         # optional in-play hint detection
```

Plus, if engine-backed:

```text
engine/src/main/kotlin/com/quietgrid/engine/<game>/
  ...                      # canonical rules, solving techniques, difficulty scoring

cli/src/main/kotlin/com/quietgrid/cli/<game>/
  <Game>Generator.kt        # offline puzzle generator, invoked via ./gradlew :cli:run
```

Not every game needs every file above — follow the closest existing game (e.g. `takuzu/` for an
engine-backed grid game, `minesweeper/` or `chimptest/` for a non-engine game) rather than inventing new
structure.

## Responsibilities

- `<Id>ViewModel.kt`
  - owns session state, the `*PersistedSession` shape, mistake/score tracking, win/loss transition
- `<Id>Logic.kt`
  - rules, canonical moves, board validation
- `<Id>Models.kt`
  - shared data classes for the game
- `<Id>Grid.kt` / `<Id>PlayScreen.kt`
  - board rendering and screen chrome (header row, hint toggle, `ZoomableBoardSurface`)
- `<Id>PuzzleBank.kt`
  - loads the committed puzzle JSON asset, when the game ships pre-generated puzzles
- `<Id>NextMove.kt`
  - optional; in-play hint detection, reusing `engine/` move logic when engine-backed

## Content rule

All game-facing copy belongs in `res/values*/strings.xml`, including:

- play labels
- How to Play text (rendered from `ui/screens/HowToPlayScreen.kt`, copy from `strings.xml`)
- hint copy
- loss/completion copy

## Player guidance rule

- Every new game ships a static How to Play composable (`<Id>HowToPlay()` added to
  `ui/screens/HowToPlayScreen.kt`, dispatched by `GameId`).
- Next-move hints are optional and, when present, must reuse the same move logic as `engine/` (if
  engine-backed) or `<Id>Logic.kt` (if not) — not a separately invented heuristic.
- Do not scaffold a tutorial screen or lesson-config file — that pattern is retired, see `learning-center.md`.
