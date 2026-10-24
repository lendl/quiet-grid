# Play screen standards

Every game's play screen follows these rules, including any game added later. They come from the quiet play screen redesign, and the design is on the [design canvas](https://claude.ai/artifact/TnpKeiwouK7Qysx8HePNVW).

Read this before building or changing any `games/<id>/*PlayScreen.kt`. If a game seems to need an exception, ask the user first instead of inventing a one-off layout.

## Build on the shared scaffold

- Use `GamePlayScaffold` (`ui/play/GamePlayScaffold.kt`) for every play screen. Never hand-build a header `Row`, timer line, hint `Card`, end-puzzle button or `EndPuzzleDialog` inside a game package. The scaffold owns all of them.
- The game passes only:
  - `timer`: `PlayTimer.Elapsed(seconds)` for normal play, `PlayTimer.Countdown(seconds)` for timed modes such as Challenger, or `null`.
  - `difficulty`: the difficulty of the loaded puzzle, read from the session, not from the route. The resume route always carries `EASY`.
  - `hint` (`PlayHintAction`) for the button and `hintMessage` (`PlayHintMessage(title, body)`) for the text, if the game has hints. The scaffold draws and places the card.
  - `info`: a `List<InfoItem>` for the strip.
  - `board`, and `dock` for controls. Leave `dock` out when the game has no controls. Sideways phones then have no right panel: the strip items and the hint share one column beside the board.
  - `boardAspectRatio` (width / height, default `1f`) and `dockHeight` (default `0.dp`) when the defaults don't fit.
  - `wideDock = true` when the dock needs width sideways (keyboards, long text panels), and `dockRole = DockRole.DragSource` when the player drags pieces out of it.
  - `phonePortraitOnly = true` for games with an on-screen keyboard: phones (smallest width under 600dp) stay upright while the game is open, and tablets still rotate. The previous orientation comes back when the player leaves. This wins over the player's rotation setting on phones, so list the game in `PHONE_PORTRAIT_ONLY_GAMES` (`core/GameCatalog.kt`) too.
  - `finishing` (the ViewModel's `finishing` flow, collected as state) so the board folds into its center when the puzzle ends and the result medallion grows out of that spot. Add `foldRows` and `foldCols` with the grid size when the board is a grid; other boards fold as a default grid. Challenger and Endless runs leave it out.
- `onEndPuzzle` is called after the player confirms the scaffold's End puzzle dialog, or right away when the player turned that confirmation off.
- Challenger and Endless variants use the same scaffold. Their hearts, tier or best score go in `info`, and their countdown goes in `timer`.
- Reuse the shared pieces before writing your own: `heartsInfo(...)` for lives, `puzzleLanguageInfo(...)` for the puzzle language flag, `PlaySegmentedButtons` for two-way mode switches, `PlayHintAction(enabled = ...)` when the hint is temporarily unavailable.
- Never special-case a game by name in shared code.

## Five slots, always in this order

1. **Top bar:** back on the left, the timer centered, then Hint, Rules (?) and the ⋮ menu on the right.
   - Never show the game name.
   - A game without hints leaves the hint icon out, and so does a game whose player turned hints off (app-wide or for that game). Rules and ⋮ keep their positions.
   - End puzzle lives in the ⋮ menu, not as its own icon.
   - When the timer is off (app-wide or for that game), the center stays empty and nothing shifts. A countdown always shows.
2. **Info strip:** one line of muted text with what the player needs to see (lives, theme, score / target, mines left, words found, puzzle language). It always keeps its height, even when empty. The strip adds the `·` separators itself, and only the first item marked `emphasized` is emphasized.
3. **Board:** centered on the whole page when it fits, otherwise as close to the middle as it can get without touching the strip or the dock. It stays put.
4. **Dock:** the game's tools and input (pad, keyboard, tray, word list), near the thumb. Its height is fixed per game and layout (`dockHeight = { sizeClass -> ... }`). Labelled tools (icon over a short label) sit in one row, toggles show their on state with the primary container tint.
5. **Overlays:** the hint card and the rules peek. They never push the layout, and only one shows at a time: opening one closes the other first (the hint card hides, its board highlight stays).

## Difficulty

The play screen never shows the difficulty when the player just picked it. It shows it only when the player didn't choose it: a Daily puzzle (`Daily · <date> · <difficulty>`) or a Mix draw (`<difficulty>`). The scaffold adds these items to the front of the strip from `LocalPlayContext`, which `AppNavHost` provides. Games only pass `difficulty`.

## Hints

- The hint card is a header line (lightbulb, title, close ✕) with the body under it. The ✕ hides only the card: the board highlight stays until the player's next move. The hint button then shows the card again, and tapping it while the card shows turns the hint off.
- The hint never covers the board or the dock unless the player asks for more:
  - **Upright:** it drops down from under the top bar, over the info strip and the empty space above the board. When the text doesn't fit (small phones), the description gets shorter first: it ends with "…" or disappears, with an expand arrow. Tapping the card expands it over the board, and tapping again collapses it.
- The title always shows in full and wraps to more lines when it needs to. It is never cut to make room for the description.
  - **Sideways phone:** it sits in a column between the board and the right panel. When a centered board would leave less than 160 for that column, the board sits next to the rail instead (it still never moves during play). If even that column is under 160 (small phones), the card also covers the panel. A game without a dock has no panel: the strip items stack at the top of the column beside the board, and the hint fills the space under them.
  - **Landscape tablet:** a fixed area at the top of the side panel, between the strip and the dock, with larger text.
- No animation on the hint card.

## Rules peek

- The ? button toggles a card that drops down under the top bar (beside the rail when sideways). The board and dock behind it dim. Any tap or Back closes it.
- The card shows the game's picture rules side by side: a small drawing of each rule with a short title, separated by thin dividers, then a "Full rules" button that opens the game's Rules page (`Routes.RULES`, the same content as the Rules tab, with a back arrow to the puzzle). It deliberately isn't the game page itself: its Play tab could start or resume a second copy of the running puzzle.
- Picture rules come from `QuickStartContent.rules` (`QuickRule(titleRes) { drawing }`). Draw each one with the game's own cell colors, about 40 high. Games without picture rules fall back to the quick-start text (goal, bullets, tip).
- Every new game must be registered in `quickStartFor` (`ui/components/QuickStartRegistry.kt`) and should get 2 or 3 picture rules.

## Dock types

A dock is one of two types. Pick the type from how the player uses the dock, never from which game it is.

| Type | How to recognize it | Examples | Where it sits |
| --- | --- | --- | --- |
| Controls (default) | The player taps it to act on the selected cell or the game: buttons, toggles, number pad, keyboard, word list. | Sudoku pad, Word Guess keyboard, Word Search list, Minesweeper Dig / Flag | Under the board when upright (phone and tablet), in the right panel when sideways and on landscape tablets. |
| Drag source | The player drags items **out of** the dock **onto** the board: pieces, tiles, letters, cards. | Block Fill tray. Any future game with draggable pieces, tiles or cards. | Always **directly next to the board**, so the drag stays short: under it when upright (phone and tablet), a column right beside it when sideways and on landscape tablets, never in a separate panel. The info strip sits above the board. Items are drawn at a size close to the board's cells. |

When a dock mixes both (draggable pieces plus a few buttons), it's a drag source, and the buttons go with the pieces in the same column.

Pass `dockRole = DockRole.DragSource` for a drag source. Block Fill (`BlockFillPlayLayout`) is the reference: it keeps the drag state above the scaffold and draws the floating piece on top of it.

## Hard rules

- **The grid never moves during play.** Nothing that appears or disappears (hints, messages, errors, toggles) may change the board's position or size. The scaffold enforces this: the board's box depends only on the window size, and dock content that doesn't fit is clipped.
- **The grid gets its full size first, after the dock.** Set `dockHeight` to what the essential controls need, and no more. When a list doesn't fit, cut it off and put the rest behind a button such as "All words". Never shrink the grid to fit optional content.
- **Lists show what's left first.** In any list of targets (words, items), unfound entries come first and found ones go last, crossed out and muted. Each group is sorted alphabetically (`wordsInPlayOrder`).
- **Lists never scroll in place.** A list shows what fits and fades out at the bottom. An "All words · n left" button opens a sheet with "To find" first and "Found" after.
- **Quiet look:** muted text in the strip, no cards, borders or banners around the header, no always-visible rules or tips (not even in the tablet panel), no decorative motion during play.

## Size classes

The scaffold measures the space it actually gets (after system bars and the app's own chrome) and picks the layout from that, so rotation, split screen and foldables switch automatically. The thresholds live in `ui/play/PlayLayout.kt`. Games must look right in all six layouts. The dock receives the `PlaySizeClass`, so it can arrange itself per layout.

| Layout | `PlaySizeClass` | When (dp, measured area) | What changes |
| --- | --- | --- | --- |
| Small phone, upright | `Compact` | not tablet, not sideways, height < 700 | Top bar 52, strip 32, 44dp icon buttons. Long lists shrink to one line plus a button that opens a sheet. The dock keeps only essential controls. |
| Normal phone, upright | `Medium` | not tablet, not sideways, height >= 700 | Top bar 56, strip 40, 48dp icon buttons. The dock sits under the board. Lists get a fixed box with the rest behind "All words". |
| Small phone, sideways | `PhoneLandscape` | height < 480 and width > height, width < 700 | Rail on the left (back, timer, hint, rules, ⋮). The board fills the full height. The right panel is 168 wide: strip items stacked at the top, controls in one compact column below. |
| Normal phone, sideways | `PhoneLandscape` | height < 480 and width > height, width >= 700 | Same rail. The right panel is 196 wide, with room for a 3x3 pad, a tray column, or a list in 2 to 4 columns. |
| Tablet, landscape | `TabletLandscape` | width >= 600, height >= 480, width > height | Top bar 80, 64dp icon buttons, 20sp timer, text about a third larger (hint title 20, body 18). The board sits on the left at full height, with a side panel on the right (36% of the width, at most 440) for the strip, the hint and the dock. |
| Tablet, upright | `TabletPortrait` | width >= 600, height >= 480, height >= width | Same larger sizes. The board takes the full width at the top, and the dock goes under it. |

Unfolded foldables count as tablets. A phone turned sideways never gets the tablet layout.

## Player preferences

Timer, hint button, screen rotation, keep screen awake, vibration, check lines as you go, reduce motion, left-handed layout, pinch to zoom and confirm before End puzzle each have an app-wide default in Settings and an optional per-game override on the game page's Settings tab (`GamePreferencesRepository`, `ui/screens/GamePreferencesSection.kt`). `AppNavHost` resolves them per game with `playPreferencesFor(gameId)` and hands them down through `ProvideGamePlayPreferences` on every play and result route (including Challenger and Endless), which also sets `LocalHapticsEnabled`, `LocalReduceMotion` and `LocalPinchZoomEnabled`. The scaffold applies the timer, hint, rotation, left-handed and End puzzle ones itself, and `BoardEntrance`, `FeedbackText`, `PuzzleBoardContainer` and `ConfettiBurst` follow the locals. Play screens read `LocalGamePlayPreferences` only for game-specific behavior, such as `showMistakes` in the games listed in `MISTAKE_CHECK_GAMES`.

- **Left-handed layout** mirrors the sideways layouts (sideways phone and tablet landscape): the rail or top-bar chrome and the panel swap sides, and the rules peek opens on the panel side. The scaffold does this by laying out in the opposite direction and restoring the original direction inside the board, dock, strip, rail and hint card, so board content never mirrors.
- **Reduce motion** covers decorative motion only: the board entrance, spins and shakes, confetti, the end-of-puzzle fold and the result screen entrance. Moves that show what happened (2048 tiles sliding, Arrow Escape arrows leaving) still animate. Use `rememberReduceMotion()`, which also honors the system animation setting, instead of reading `systemAnimationsDisabled` directly.
- **Check lines as you go** off skips line checking entirely: no spin or lock for a right line, no flag, shake or buzz for a wrong one, and a wrong line costs no score. Only the finished puzzle is checked. Hiding just the wrong-line flag would still give mistakes away, because a finished line that doesn't spin must be wrong. The ViewModel passes `showMistakes` into its finalize-validation step, which then checks no lines.

## Checklist for a new game

- [ ] Uses `GamePlayScaffold`, with no custom header, timer, end button or end dialog.
- [ ] Has `QuickStartContent` registered in `quickStartFor`, with 2 or 3 picture rules in `rules`.
- [ ] Passes `difficulty` from the session. The strip shows only game state, never the title.
- [ ] The hint, if the game has one, uses `hint` / `hintMessage`, and the game is listed in `HINT_CAPABLE_GAMES` (`core/GameCatalog.kt`) so its Settings tab offers the hint preference. Likewise list it in `ZOOMABLE_GAMES` if its board is zoomable and in `MISTAKE_CHECK_GAMES` if it flags wrong lines.
- [ ] The dock type fits how the player uses it.
- [ ] `dockHeight` covers the essential controls, and nothing in the dock grows past its box.
- [ ] Checked in all six layouts: small and normal phone each upright and sideways, and tablet in landscape and upright. `GamePlayScaffoldTest` shows how to cover them with Robolectric qualifiers.
- [ ] New strings are only in `values/strings.xml`, per the localization rule in `CLAUDE.md`.
