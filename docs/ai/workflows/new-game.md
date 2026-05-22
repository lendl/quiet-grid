# New Game Workflow

1. Confirm the game fits Quiet Grid:
   - logic grid puzzle
   - offline only
   - no bot requirement
   - no internet requirement
2. If AI already knows the puzzle, propose defaults but require user approval.
3. Define canonical moves.
4. Define support actions separately.
5. Define mistake policy.
6. Choose existing feedback effects.
7. If a required effect does not exist, split that effect into its own change before adding the game.
8. Define the full difficulty ladder:
   - easy
   - medium
   - hard
   - expert
9. Validate that the difficulty ladder is meaningful and separable.
10. Decide engine-backed vs non-engine.
11. Decide Learning Center scope:
   - load `docs/ai/context/learning-center.md`
   - treat it as the single source of truth for tutorial, next move, and analyzer
   - tutorial
   - next move
   - analyzer
12. Make the proof model explicit:
   - whether hypothetical branching is allowed
   - whether engine-backed teaching must stay strictly inside approved human/canonical moves
13. Choose analyzer mode:
   - engine-backed: approved move-path teaching, not hidden full-solution proof
   - non-engine: loss-state reflection
14. Apply the base scaffold.
15. Add engine add-on only if needed.
16. Wire registries and persistence.
17. Keep all game-facing copy in `content/i18n/`.
18. Validate with lint and typechecks.
19. Review against the new-game checklist.
