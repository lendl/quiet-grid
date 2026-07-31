# New Game Checklist

- [ ] Game fits Quiet Grid eligibility rules.
- [ ] User approved the important game-design choices.
- [ ] Canonical moves are defined.
- [ ] Support actions are separated from canonical moves.
- [ ] Mistake policy is defined explicitly in `<Id>ViewModel.kt` / `<Id>Logic.kt`.
- [ ] Loss condition is defined separately if applicable (`finishAsLoss()`, `Routes.LOSS`).
- [ ] Existing feedback effects (`ui/components/`) are selected.
- [ ] Missing reusable effects were split into a separate change.
- [ ] All four difficulty levels are playable:
  - [ ] easy
  - [ ] medium
  - [ ] hard
  - [ ] expert
- [ ] Difficulty separation is meaningful.
- [ ] Engine-backed vs non-engine mode is explicit.
- [ ] `core/GameCatalog.kt` is updated (the single registry — no separate engine registry to update).
- [ ] Engine module (`engine/` + `cli/`) is added if engine-backed.
- [ ] `*PersistedSession` shape is defined in the ViewModel and wired to `SessionRepository`.
- [ ] All game-facing copy is in `res/values*/strings.xml` (source-of-truth `values/strings.xml` plus every
  locale it should ship in).
- [ ] Static How to Play composable added to `ui/screens/HowToPlayScreen.kt` for the new `GameId`.
- [ ] Next-move hints, if implemented, reuse the same move logic used for difficulty/canonical moves.
- [ ] No tutorial screen or lesson-config file was added (retired pattern).
- [ ] `./gradlew compileDebugKotlin`
- [ ] `./gradlew assembleDebug`
- [ ] `./gradlew lint`
- [ ] `./gradlew testDebugUnitTest`
- [ ] `./gradlew :engine:test :cli:test` when engine-backed
