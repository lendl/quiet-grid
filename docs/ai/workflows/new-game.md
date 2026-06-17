# New Game Workflow

1. Confirm the game fits Quiet Grid:

   - logic grid puzzle (or explicitly approved exception — note the exception before proceeding)
   - offline only
   - no bot requirement
   - no internet requirement

2. If AI already knows the puzzle, propose defaults but require user approval.

3. Capture explicit user-approved UX contract before coding (coding is blocked until approved):

   - interaction model (single-cell cycle vs explicit mode toggle vs selected-cell + input bar)
   - support-action model (for example notes, flags, marks) and whether those actions are optional or required
   - whether technique explanation must ignore support actions when canonical progress exists
   - helper behavior on invalid grids
   - whether technique explanation should focus/select target cell in live play
   - game-specific grid conventions (for example, Nonogram 5-cell separators)

4. Define canonical techniques.

5. Define support actions separately.

6. Define mistake policy.

7. Choose existing feedback effects.

8. If a required effect does not exist, split that effect into its own change before adding the game.

9. Define the full difficulty ladder:

   - easy
   - medium
   - hard
   - expert

10. Validate that the difficulty ladder is meaningful and separable.

11. Decide engine-backed vs non-engine.

12. If engine-backed, lock the engine generation contract before broad app wiring:

    - full size matrix, including approved rectangular variants and size caps
    - target puzzle counts per size/difficulty bucket
    - uniqueness/dedupe rule
    - allowed proof/classification model
    - catalog entry shape and round-trip expectations

13. If engine-backed, prove feasibility before full game integration:

    - generator/classifier spike first
    - bulk-generate target-like volume early
    - confirm buckets have enough supply before technique explanation/play wiring

14. Decide Learning Center scope:

    - load `docs/ai/context/learning-center.md`
    - treat it as the single source of truth for how to play, technique explanation (live), and technique explanation (post-game)
    - how to play static page
    - how to play interactive onboarding
    - technique explanation (live)
    - technique explanation (post-game)

15. For this repository, ship both Learning Center surfaces (how to play, technique explanation) unless user explicitly approves an exception.

16. Make the proof model explicit:

    - whether hypothetical branching is allowed
    - whether engine-backed teaching must stay strictly inside approved human/canonical techniques
    - whether elimination-only reasoning may appear as a standalone player-facing technique or only as evidence for an actionable technique

17. Choose technique explanation mode:

    - engine-backed: approved technique-path teaching, not hidden full-solution proof
    - non-engine: loss-state reflection

18. Enforce surface completeness before final validation:

    - how to play is wired: `content/i18n/en.ts` has all sections (goal, controls, wrongMove, rules, techniques, tips, scoring); `content/howToPlay.ts` calls the i18n function; game `definition.ts` has `supports.learning`; `howToPlayContent.ts` interface is satisfied
    - technique explanation (post-game) is wired (`ui/learning/analyzer/`, not content-only)
    - approved input model is implemented in gameplay actions and play UI

19. Apply the base scaffold.

20. Add engine add-on only if needed.

21. For engine-backed games, finish engine gates before broad app polish:

    - bulk generation succeeds
    - reclassification returns expected buckets
    - catalog ids/dedupe/parse round-trip works
    - no fallback difficulty/proof labels remain for techniques that are not actually implemented and teachable

22. Wire registries and persistence. Set `beta: true` in `definition.ts` — all new games ship as beta.

23. Keep all game-facing copy in `content/i18n/`. New games ship with `en` only; other languages (nl, de, fr, es) are added when the game is promoted out of beta.

24. Prefer canonical module paths (`gameplay/`, `ui/`, `content/`, `platform/`, `engine/`) even if older games still expose root-level compatibility shims.

25. In the adapter's `getState`, update state using `sessionRef.current + setSession` directly for correct taps. Only route through `runShellAction` for actions that require `handleEffects` (e.g. loss, win with side-effects). This is the pattern used by all working games — using the shell pipeline alone for correct taps is unreliable.

26. Run manual play checks before final validation:

    - play through at least one full correct round
    - trigger a wrong tap and confirm loss flow
    - navigate away and resume — confirm session restores to a playable state

27. Run manual Learning Center acceptance checks before final validation:

    - technique explanation (live) returns one actionable canonical technique
    - technique explanation (live) does not require support actions when canonical progress exists
    - technique explanation (post-game) teaches from puzzle state, not support-action history, unless explicitly approved

28. Validate with lint and typechecks.

29. Review against the new-game checklist.
