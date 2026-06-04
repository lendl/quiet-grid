# New Game Workflow

1. Confirm the game fits Quiet Grid:
   - logic grid puzzle
   - offline only
   - no bot requirement
   - no internet requirement
2. If AI already knows the puzzle, propose defaults but require user approval.
3. Capture explicit user-approved UX contract before coding (coding is blocked until approved):
   - tutorial required and expected scope
   - interaction model (single-cell cycle vs explicit mode toggle vs selected-cell + input bar)
   - support-action model (for example notes, flags, marks) and whether those actions are optional or required
   - whether tutorial may mention support actions
   - whether next move/analyzer must ignore support actions when canonical progress exists
   - helper behavior on invalid boards
   - whether next move should focus/select target cell in live play
   - tutorial chrome expectations (skip button, extra live-play info box, compact vs spacious layout)
   - game-specific board conventions (for example, Nonogram 5-cell separators)
4. Define canonical moves.
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
   - confirm buckets have enough supply before tutorial/analyzer/play wiring
14. Decide Learning Center scope:
   - load `docs/ai/context/learning-center.md`
   - treat it as the single source of truth for tutorial, next move, and analyzer
   - tutorial
   - next move
   - analyzer
15. For this repository, ship all three Learning Center surfaces (tutorial, next move, analyzer) unless user explicitly approves an exception.
16. Make the proof model explicit:
   - whether hypothetical branching is allowed
   - whether engine-backed teaching must stay strictly inside approved human/canonical moves
   - whether elimination-only reasoning may appear as a standalone player-facing move or only as evidence for an actionable move
17. Choose analyzer mode:
   - engine-backed: approved move-path teaching, not hidden full-solution proof
   - non-engine: loss-state reflection
18. Enforce surface completeness before final validation:
   - tutorial is wired (`supports.tutorial`, `screens.tutorial`, `ui/tutorial/`)
   - analyzer is wired (`ui/learning/analyzer/`, not content-only)
   - approved input model is implemented in gameplay actions and play UI
19. Apply the base scaffold.
20. Add engine add-on only if needed.
21. For engine-backed games, finish engine gates before broad app polish:
   - bulk generation succeeds
   - reclassification returns expected buckets
   - catalog ids/dedupe/parse round-trip works
   - no fallback difficulty/proof labels remain for techniques that are not actually implemented and teachable
22. Wire registries and persistence.
23. Keep all game-facing copy in `content/i18n/`.
24. Prefer canonical module paths (`gameplay/`, `ui/`, `content/`, `platform/`, `engine/`) even if older games still expose root-level compatibility shims.
25. Run manual Learning Center acceptance checks before final validation:
   - tutorial decision checkpoints do not reveal the answer before response
   - next move returns one actionable canonical move
   - next move does not require support actions when canonical progress exists
   - analyzer teaches from puzzle state, not support-action history, unless explicitly approved
   - tutorial/next move/analyzer use the approved support-action policy consistently
26. Validate with lint and typechecks.
27. Review against the new-game checklist.
