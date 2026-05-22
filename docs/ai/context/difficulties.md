# Difficulty Ladder

## User goal

- Help players pick a level that matches their current skill.
- Show a clear path from easy to expert.

## Architecture goal

- Every new game should ship with playable `easy`, `medium`, `hard`, and `expert` levels.
- Difficulty should reflect real differences in move complexity and learning demand, not cosmetic labels.

## Rules

- Validate that all four levels are meaningful and separable.
- Reject models where one tier is practically unreachable or two tiers behave the same.
- If engine-backed, the engine should classify difficulty and support re-evaluation when heuristics change.
- If engine heuristics become stricter, reclassification may drop or downgrade existing catalog entries; treat that as real fallout to audit, not as noise to hide.
- Learning Center may explain current and next difficulty expectations, but tutorial should not compare difficulties.

## Validation questions

- Does each level make sense for this puzzle?
- Are bucket distributions reasonable?
- Can the engine still produce enough puzzles for each tier?
- After heuristic changes, do reclassified buckets still have enough puzzles to ship?
- Does progression reflect harder move combinations rather than arbitrary size changes alone?

## File map

- `src/games/<id>/definition.ts`
- `src/games/<id>/content/i18n/`
- `src/games/<id>/engine/difficulty.ts`
- `src/games/<id>/engine/definition.ts`

## Mistakes to avoid

- Do not define four levels on paper if they collapse in practice.
- Do not teach difficulty progression only through UI labels with no move-based meaning.
- Do not preserve old difficulty labels when the current move model no longer supports them.
