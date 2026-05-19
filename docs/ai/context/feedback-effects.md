# Feedback Effects

## User goal

- Keep puzzle feedback readable and consistent across games.

## Architecture goal

- New games should use pre-selected reusable feedback effects.
- Creating a new reusable effect is a separate change from adding a new game.

## Rules

- Pick from existing feedback effects first.
- If the desired effect does not exist, stop and split the work:
  1. add reusable effect
  2. land that change
  3. add the game using that effect
- Do not create a new effect and a new game in one go.

## Typical effects

- success feedback
- mistake feedback
- hint emphasis
- completion emphasis
- line or cell highlight effects
- board spin feedback for correct cells or completed lines
- board shake feedback for incorrect cells or completed lines

## File map

- shared feedback or component files under `src/app/`
- shared board feedback helpers under `src/app/shell/` such as `boardFeedback.ts` and `useBoardFeedbackAnimation.ts`
- game play UI under `src/games/<id>/ui/play/`
- Learning Center surfaces when they reuse existing effects

## Mistakes to avoid

- Do not invent one-off effects inside a game package when the effect is meant to be reusable.
