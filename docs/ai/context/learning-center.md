# Learning Center

## User goal

- Help players learn each puzzle, improve over time, and become expert players.
- Keep teaching centered on puzzle interaction and canonical move language.
- Use tutorial for first-contact teaching, next move for live guidance, and analyzer for reflection.

## Architecture goal

- Tutorial flow belongs in `ui/tutorial/`.
- Next move belongs to gameplay move detection plus play UI.
- Analyzer belongs in `ui/learning/analyzer/` and should reuse game move logic.
- Difficulty guidance belongs in Learning Center, but not inside tutorial lessons.

## Rules

- Tutorial should explain how the user can interact with the puzzle.
- Tutorial should use a valid example grid for each lesson when tutorial exists.
- Tutorial should not compare difficulty levels.
- Next move should explain one valid move from the current puzzle state.
- Next move should present one stored move suggestion at a time, not a continuously recomputed helper stream.
- Analyzer mode depends on game type:
  - engine-backed games: teach full solve path
  - non-engine games: analyze loss-state decisions
- Teach canonical moves first. Support actions are optional style tools.
- Explain mistake policy only when it changes how the player should play.

## File map

- `src/games/<id>/ui/tutorial/`
- `src/games/<id>/ui/learning/analyzer/`
- `src/games/<id>/content/tutorialLessons.ts`
- `src/games/<id>/content/i18n/`
- shared layout helpers in `src/app/components/` when needed

## Mistakes to avoid

- Do not hardcode game-facing copy in screens or components.
- Do not use invalid or rule-breaking tutorial boards.
- Do not let tutorial drift into difficulty comparison.
- Do not invent analyzer logic separate from the game’s move logic.
