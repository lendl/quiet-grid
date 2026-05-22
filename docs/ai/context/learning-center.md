# Learning Center

Learning Center is the umbrella teaching system for a game. It includes all three teaching surfaces:

- Tutorial
- Next Move
- Analyzer

## User goal

- Help players learn each puzzle, improve over time, and become expert players.
- Keep teaching centered on puzzle interaction and canonical move language.
- Use tutorial for first-contact teaching, next move for live guidance, and analyzer for reflection.

## Surface user goals

- Tutorial user goal: help the player safely learn the rules, interaction model, and core move language before or alongside real play.
- Next Move user goal: help the player during live play by suggesting one valid, approved next step without taking control away from the player.
- Analyzer user goal: help the player reflect on a finished state, understand why moves were right or wrong, and improve future decision-making.

## Architecture goal

- Learning Center owns tutorial, next move, and analyzer as one connected subsystem.
- Tutorial flow belongs in `ui/tutorial/`.
- Next move belongs to gameplay move detection plus play UI.
- Analyzer belongs in `ui/learning/analyzer/` and should reuse game move logic.
- Difficulty guidance belongs in Learning Center, but not inside tutorial lessons.

## Rules

- Shared Learning Center rules apply to tutorial, next move, and analyzer unless a rule below names only one surface.
- Keep all Learning Center surfaces aligned with the same canonical move language and puzzle semantics.
- Tutorial should introduce the player goal and win condition before or alongside move teaching.
- Tutorial should explain how the user can interact with the puzzle.
- Tutorial should explain the real live-play control mapping, not only tutorial-specific controls.
- Tutorial should explicitly call out when tutorial controls differ from live puzzle controls.
- Tutorial should use a valid example grid for each lesson when tutorial exists.
- Tutorial example grids should be valid puzzle states that obey the game rules and the lesson claim being taught.
- Tutorial should not compare difficulty levels.
- Next move should explain one valid move from the current puzzle state.
- Next move should present one stored move suggestion at a time, not a continuously recomputed helper stream.
- Next move should support the player, not replace the player’s agency.
- Analyzer mode depends on game type:
  - engine-backed games: teach an approved solve path built from the same canonical move system used for classification
  - non-engine games: analyze loss-state decisions
- Analyzer should explain decisions in a way that helps the player improve future runs, not just describe the past state.
- Teach canonical moves first. Support actions are optional style tools.
- Explain mistake policy only when it changes how the player should play.
- Do not use hidden brute-force or full-solution search for player-facing next-move or analyzer explanations unless that proof model is explicitly part of the approved move language.
- If an engine-backed game allows hypothetical branches, teach that branch model explicitly and keep branch resolution inside approved move logic.

## File map

- Learning Center spans tutorial UI, next move play integration, analyzer UI, and their shared content.
- `src/games/<id>/ui/tutorial/`
- next move logic in `src/games/<id>/gameplay/` and play adapter wiring in `src/games/<id>/ui/play/`
- `src/games/<id>/ui/learning/analyzer/`
- `src/games/<id>/content/tutorialLessons.ts`
- `src/games/<id>/content/i18n/`
- shared layout helpers in `src/app/components/` when needed

## Mistakes to avoid

- Do not hardcode game-facing copy in screens or components.
- Do not use invalid or rule-breaking tutorial boards.
- Do not use tutorial-only controls without explaining how they map to real play.
- Do not let tutorial drift into difficulty comparison.
- Do not invent analyzer logic separate from the game’s move logic.
- Do not let engine-backed teaching explain a move with evidence the player could never derive from the approved move set.
