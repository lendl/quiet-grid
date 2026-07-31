# Player Guidance

> Formerly "Learning Center" (tutorial + next move + analyzer) under the old React Native app. The interactive
> tutorial and post-loss analyzer were dropped when the app moved to Kotlin/Compose — see
> [[feedback_how_to_play]] equivalent: new games ship a static How to Play page only, plus optional in-play
> next-move hints. There is no tutorial screen and no analyzer/loss-review surface today.

## User goal

- Teach the player the rules well enough to start, mostly through the static How to Play page.
- Give players who are stuck a nudge via next-move hints, without solving the puzzle for them.
- Keep teaching centered on canonical move language so hints and rules text agree.

## Architecture goal

- How to Play content lives centrally in `ui/screens/HowToPlayScreen.kt` as one `@Composable` function per
  game (e.g. `TakuzuHowToPlay()`), dispatched by `GameId` — not scattered per-game files.
- Next-move hint logic (when a game has it) lives in the game's own package as `<Id>NextMove.kt`, e.g.
  `games/takuzu/TakuzuNextMove.kt`. It inspects the live board and engine move techniques
  (`engine/src/main/kotlin/com/quietgrid/engine/<game>/`) and returns a sealed hint type.
- Hints surface via the hint toggle in the Play screen's header row (see `*PlayScreen.kt`), not a separate
  screen.
- There is no dedicated Analyzer surface. Post-game reflection is limited to whatever `CompletionScreen.kt` /
  `LossScreen.kt` show via `CompletionExtras` (see [[project_completion_screen_overhaul]]) — per-game flavor
  like a word list or solved-picture preview, not move-by-move teaching.

## Rules

- How to Play should explain how to interact with the puzzle and its rules; it does not compare difficulty
  levels.
- Next-move hints, where implemented, should point to one real valid move from the game's own move-detection
  logic (mirroring the engine's canonical moves) — not a separately invented "helper" heuristic.
- Only add next-move hints for a new game if the game's canonical moves are already defined; do not invent
  hint logic that diverges from the move vocabulary used for difficulty scoring.
- Do not build a tutorial screen or lesson-config file for a new game — that pattern is retired.
- Do not build an analyzer/loss-review screen unless explicitly requested; it does not exist as a pattern to
  copy from.

## File map

- `app/src/main/java/com/quietgrid/app/ui/screens/HowToPlayScreen.kt` — static rules content, all games
- `app/src/main/java/com/quietgrid/app/games/<id>/<Id>NextMove.kt` — optional, in-play hint detection
- `app/src/main/java/com/quietgrid/app/games/<id>/<Id>PlayScreen.kt` — hint toggle wiring
- `engine/src/main/kotlin/com/quietgrid/engine/<game>/` — canonical move/technique logic hints should reuse
- `res/values*/strings.xml` — all How to Play and hint copy (never hardcoded in Composables)

## Mistakes to avoid

- Do not hardcode How to Play or hint copy in Composables — it belongs in `strings.xml`.
- Do not scaffold `ui/tutorial/` or a lesson-config file for a new game.
- Do not invent hint logic that uses different reasoning than the engine's move techniques.
- Do not build a new analyzer surface without confirming with the user first — it is not an expected part of
  a new game.
