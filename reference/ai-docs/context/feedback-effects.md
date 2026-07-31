# Feedback Effects

## User goal

- Keep puzzle feedback readable and consistent across games.

## Architecture goal

- New games should reuse the shared chrome components under `ui/components/`.
- Creating a new reusable effect is a separate change from adding a new game.
- Animations should settle once, not loop — see [[feedback_stability_over_animation]].

## Rules

- Pick from existing feedback effects first.
- If the desired effect does not exist, stop and split the work:
  1. add the reusable component under `ui/components/`
  2. land that change
  3. add the game using that component
- Do not create a new effect and a new game in one go.

## Typical effects

- `ui/components/FeedbackText.kt` — per-cell correct/incorrect spin-shake
- `ui/components/ZoomableBoardSurface.kt` — pinch-zoom + pan wrapper for boards
- `ui/components/BoardEntrance.kt` — board entrance animation
- `ui/screens/CompletionScreen.kt` / `LossScreen.kt` — one-shot settle (fade + slide + scale) plus
  `ConfettiBurst` on win; per-game highlight data via `ui/screens/CompletionExtras.kt`
  (see [[project_completion_screen_overhaul]])

## File map

- `app/src/main/java/com/quietgrid/app/ui/components/` — shared reusable effects
- `app/src/main/java/com/quietgrid/app/games/<id>/<Id>PlayScreen.kt` — where a game wires effects in
- `app/src/main/java/com/quietgrid/app/ui/screens/CompletionScreen.kt`,
  `app/src/main/java/com/quietgrid/app/ui/screens/CompletionExtras.kt` — win-screen per-game highlight

## Mistakes to avoid

- Do not invent one-off effects inside a game package when the effect is meant to be reusable.
- Do not add `infiniteRepeatable` looping animations — settle-once is the established pattern.
- Do not grow `Routes.COMPLETION`'s URL args for per-game data; use `CompletionExtras` instead.
