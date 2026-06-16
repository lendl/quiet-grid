# Learning Center

Learning Center is the umbrella teaching system for a game. It includes two teaching surfaces:

- How to Play
- Technique Explanation

## User goal

- Help players learn each puzzle, improve over time, and become expert players.
- Keep teaching centered on puzzle interaction and canonical technique language.
- Use how to play for first-contact teaching, technique explanation for live guidance and post-game reflection.

## Surface user goals

- How to Play user goal: help the player safely learn the rules, interaction model, and core technique language before or alongside real play.
- Technique Explanation (live) user goal: help the player during live play by suggesting one valid, approved next step without taking control away from the player.
- Technique Explanation (post-game) user goal: help the player reflect on a finished state, understand why moves were right or wrong, and improve future decision-making.

## Architecture goal

- Learning Center owns how to play and technique explanation as one connected subsystem.
- How to play static content belongs in `content/i18n/` (howToPlay section) and is rendered by the app shell.
- How to play interactive onboarding belongs in `ui/tutorial/` (code name) and `content/tutorialLessons.ts` (code name).
- Technique explanation (live) belongs to gameplay technique detection plus play UI.
- Technique explanation (post-game) belongs in `ui/learning/analyzer/` (code name) and should reuse game technique logic.
- Difficulty guidance belongs in Learning Center, but not inside how to play lessons.

## Rules

- Shared Learning Center rules apply to how to play and technique explanation unless a rule below names only one surface.
- Keep all Learning Center surfaces aligned with the same canonical technique language and puzzle semantics.
- How to play should introduce the player goal and win condition before or alongside technique teaching.
- How to play should explain how the user can interact with the puzzle.
- How to play should explain the real live-play control mapping, not only onboarding-specific controls.
- How to play should explicitly call out when onboarding controls differ from live puzzle controls.
- How to play chrome should stay minimal by default: no skip button and no extra live-play info box unless user explicitly approves them.
- How to play should include explicit decision checkpoints in key lessons (for example, asking filled vs empty) instead of only passive "continue" steps.
- How to play should use a valid example grid for each lesson when interactive onboarding exists.
- How to play example grids should be valid puzzle states that obey the game rules and the lesson claim being taught.
- How to play decision checkpoints must not reveal the answer before the player responds. Highlighting the target cell is fine; showing the answer value or other hidden resolution is not.
- How to play should not compare difficulty levels.
- How to play is required for game integrations in this repository unless user explicitly approves an exception.
- Technique explanation (live) should explain one valid technique from the current puzzle state.
- Technique explanation (live) should present one stored suggestion at a time, not a continuously recomputed helper stream.
- Technique explanation (live) should support the player, not replace the player's agency.
- Technique explanation (live) must check invalid-board state first and report that the grid must be corrected before suggesting any technique.
- Technique explanation (live) should teach canonical progress first and should not require optional support actions when canonical progress exists.
- Technique explanation (live) should end in one actionable canonical technique. Elimination-only reasoning may support that technique, but should not be exposed as the final player-facing step unless eliminations are explicitly approved as canonical actions.
- Technique explanation (live) should select or focus the target cell when that helps the player apply the technique in live play.
- Technique explanation mode depends on game type:
  - engine-backed games: teach an approved solve path built from the same canonical technique system used for classification
  - non-engine games: analyze loss-state decisions
- Technique explanation (post-game) should explain decisions in a way that helps the player improve future runs, not just describe the past state.
- Technique explanation (post-game) is not complete until analysis logic and analyzer UI are both wired, not only localized copy.
- Technique explanation (post-game) may group multiple independent proofs for the same canonical technique into one teaching step when they all support the same target action.
- Technique explanation (post-game) for engine-backed games should teach from the puzzle state itself. It may reference visible support actions (flags, notes) as context, but the suggested action must always be a technique.
- Technique explanation (post-game) for engine-backed games should prefer placement-first teaching steps. Internal elimination logic may be used as evidence, but the player-facing step should still resolve to one approved target action unless eliminations are explicitly canonical.
- Teach canonical techniques. Support actions may appear as context in explanations but must never be the suggested player action.
- How to play may mention support actions to explain player controls.
- Explain mistake policy only when it changes how the player should play.
- Do not use hidden brute-force or full-solution search for player-facing technique explanation unless that proof model is explicitly part of the approved technique language.
- If an engine-backed game allows hypothetical branches, teach that branch model explicitly and keep branch resolution inside approved technique logic.

## Technique Explanation (live) policy

- Technique explanation (live) suggests one approved canonical technique from current puzzle state.
- If puzzle state is invalid, technique explanation does not suggest a technique and must state the grid must be corrected first.
- Technique explanation (live) must suggest a technique as the player's next step. It may reference visible support actions as context, but must not suggest a support action as the output.
- Technique explanation (live) does not expose elimination-only proof steps as standalone techniques unless eliminations are explicitly approved canonical actions.
- Technique explanation (live) should be deterministic for same stored puzzle/session state and explanation context.

## File map

- Learning Center spans how to play onboarding, technique explanation play integration, analyzer UI, and their shared content.
- `src/games/<id>/ui/tutorial/` (code name for how to play onboarding)
- technique explanation logic in `src/games/<id>/gameplay/` and play adapter wiring in `src/games/<id>/ui/play/`
- `src/games/<id>/ui/learning/analyzer/` (code name for technique explanation post-game)
- `src/games/<id>/content/tutorialLessons.ts` (code name for how to play lessons)
- `src/games/<id>/content/i18n/` (how to play static content in howToPlay section)
- shared layout helpers in `src/app/components/` when needed

## Mistakes to avoid

- Do not hardcode game-facing copy in screens or components.
- Do not use invalid or rule-breaking how to play grids.
- Do not use onboarding-specific controls without explaining how they map to real play.
- Do not leak how to play answers before decision checkpoints resolve.
- Do not let how to play drift into difficulty comparison.
- Do not invent technique explanation logic separate from the game's technique logic.
- Do not let engine-backed teaching explain a technique with evidence the player could never derive from the approved technique set.
- Do not suggest a support action as the player's next step in technique explanation. The output must always be a technique. Support actions may appear as evidence or context, not as the conclusion.
- Do not let engine difficulty labels depend on fallback proof families that are not actually implemented and teachable in technique explanation.
