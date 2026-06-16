# New Game Checklist

## Design

- [ ] UX contract approved before coding: interaction model, support-action model, technique explanation behavior, invalid-grid helper, grid visual conventions.
- [ ] Canonical techniques defined and approved.
- [ ] Mistake and loss policy defined.
- [ ] Feedback effects chosen; any new effects landed as a separate change first.
- [ ] All four difficulty levels defined and separation is meaningful.
- [ ] For engine-backed games: size matrix, bucket targets, dedupe rule, proof model, and catalog shape approved up front.

## Wiring

- [ ] Game added to `src/app/shell/games/gameRegistry.ts`.
- [ ] Game added to `src/engine/gameRegistry.ts` if engine-backed.
- [ ] `src/app/utils/activeSessionStateStorage.ts` has `is<Game>ActiveSession()` and `normalize<Game>ActiveSession()` for this game's session type.
- [ ] `definition.ts` has `supports.learning: true` and no `supports.tutorial` or `screens.tutorial`.

## Engine (engine-backed games only)

- [ ] Generator/classifier feasibility proven before broad app wiring.
- [ ] Bulk generation succeeds and bucket supply is sufficient across all sizes and difficulties (aim for at least ~20 puzzles per size/difficulty bucket).
- [ ] Catalog round-trip works: entries parse, ids increment, dedupe behaves correctly.
- [ ] Difficulty labels only reference techniques that are actually implemented and teachable.
- [ ] Score weights validated against actual generation distribution; no bucket is starved.
- [ ] `npm run typecheck:engine`

## Content

- [ ] How to play content in `content/i18n/` for all 5 languages (en, nl, de, fr, es): goal, controls, wrongMove, rules, techniques, tips.
- [ ] `content/howToPlay.ts` delegates to i18n — no strings hardcoded in the content file.
- [ ] All game-facing copy in `content/i18n/` — nothing hardcoded in screens or components.

## Learning Center

- [ ] How to play interactive onboarding wired (`ui/tutorial/`, `content/tutorialLessons.ts`).
- [ ] Technique explanation (post-game) wired under `ui/learning/analyzer/` — not only i18n strings.
- [ ] Technique explanation (live) blocks on invalid grid state before suggesting anything.
- [ ] Technique explanation (live) suggests one canonical technique; does not require support actions.
- [ ] Technique explanation (post-game) teaches from puzzle state, not support-action history.
- [ ] Engine-backed teaching uses the canonical technique system, not hidden brute-force proof.

## Play

- [ ] Input model implemented in gameplay actions and play UI.
- [ ] Playable grid supports pinch-zoom and pan.
- [ ] Grid visual conventions implemented (e.g. Nonogram 5-cell separators).

## Final

- [ ] `npm run lint`
- [ ] `npm run typecheck:app`
