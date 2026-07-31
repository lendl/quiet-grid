# Mistake Policy

## User goal

- Make it clear whether the game notices mistakes, when it notices them, and what that means for feedback or
  score.

## Architecture goal

- Mistake policy should be defined explicitly in the game's `<Id>ViewModel.kt` (session/score state) and
  `<Id>Logic.kt` (validation), not left implicit.
- Loss condition is separate from mistake policy and belongs to the loss-transition path
  (`finishAsLoss()` in the ViewModel, `Routes.LOSS`) — see [[project_completion_screen_overhaul]].

## Rules

- Define whether mistakes exist.
- Define when mistake checks happen.
- Define what feedback the player gets — typically the shared `FeedbackText` spin/shake in
  `ui/components/FeedbackText.kt`.
- Define whether score is affected (`StatsRepository`-tracked accuracy/score fields).
- Define whether mistakes only give feedback or temporarily block progress.
- Do not treat mistakes as loss conditions.

## Examples

- Sudoku tracks mistakes live in `SudokuViewModel.kt` and factors them into accuracy/score.
- Minesweeper can have a loss condition when a mine is triggered, which is not the same thing as mistake
  policy.

## File map

- `app/src/main/java/com/quietgrid/app/games/<id>/<Id>ViewModel.kt` — mistake/score state, `finishAsWin()` /
  `finishAsLoss()`
- `app/src/main/java/com/quietgrid/app/games/<id>/<Id>Logic.kt` — move/board validation
- `app/src/main/java/com/quietgrid/app/ui/components/FeedbackText.kt` — per-cell correct/incorrect feedback
- `app/src/main/java/com/quietgrid/app/data/StatsRepository.kt` — persisted score/accuracy per game/difficulty
- `res/values*/strings.xml` — mistake-related copy

## Mistakes to avoid

- Do not mix mistake policy and loss condition into one vague rule.
- Do not hide score penalties inside Composable-only code — score state belongs in the ViewModel.
