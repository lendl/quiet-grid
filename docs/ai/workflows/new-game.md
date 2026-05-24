# New Game Workflow

1. Confirm the game fits Quiet Grid:
   - logic grid puzzle
   - offline only
   - no bot requirement
   - no internet requirement
2. If AI already knows the puzzle, propose defaults but require user approval.
3. Capture explicit user-approved UX contract before coding (coding is blocked until approved):
   - tutorial required and expected scope
   - interaction model (single-cell cycle vs explicit mode toggle)
   - helper behavior on invalid boards
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
12. Decide Learning Center scope:
   - load `docs/ai/context/learning-center.md`
   - treat it as the single source of truth for tutorial, next move, and analyzer
   - tutorial
   - next move
   - analyzer
13. For this repository, ship all three Learning Center surfaces (tutorial, next move, analyzer) unless user explicitly approves an exception.
14. Make the proof model explicit:
   - whether hypothetical branching is allowed
   - whether engine-backed teaching must stay strictly inside approved human/canonical moves
15. Choose analyzer mode:
   - engine-backed: approved move-path teaching, not hidden full-solution proof
   - non-engine: loss-state reflection
16. Enforce surface completeness before final validation:
   - tutorial is wired (`supports.tutorial`, `screens.tutorial`, `ui/tutorial/`)
   - analyzer is wired (`ui/learning/analyzer/`, not content-only)
   - approved input model is implemented in gameplay actions and play UI
17. Apply the base scaffold.
18. Add engine add-on only if needed.
19. Wire registries and persistence.
20. Keep all game-facing copy in `content/i18n/`.
21. Validate with lint and typechecks.
22. Review against the new-game checklist.
