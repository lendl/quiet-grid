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
6. Choose existing feedback effects (`ui/components/`).
7. If a required effect does not exist, split that effect into its own change before adding the game.
8. Define the full difficulty ladder:
   - easy
   - medium
   - hard
   - expert
9. Validate that the difficulty ladder is meaningful and separable.
10. Decide engine-backed (`engine/` + `cli/`) vs non-engine (logic lives entirely in the game's `app/`
    package).
11. Decide whether the game needs a next-move hint surface (`<Id>NextMove.kt`) in addition to the mandatory
    static How to Play page — see `learning-center.md`. There is no tutorial or analyzer to build; that
    system was retired.
12. Apply the base scaffold (`scaffolds/base-game/`).
13. Add the engine/cli scaffold only if engine-backed (`scaffolds/engine-addon/`).
14. Register the game in `core/GameCatalog.kt` (single registry — no separate engine registry) and add
    persistence (`*PersistedSession` via `SessionRepository`).
15. Keep all game-facing copy in `res/values*/strings.xml`.
16. Validate with:
    - `./gradlew compileDebugKotlin`
    - `./gradlew assembleDebug`
    - `./gradlew lint`
    - `./gradlew testDebugUnitTest` (and `:engine:test :cli:test` if engine-backed)
17. Review against `new-game-checklist.md`.
