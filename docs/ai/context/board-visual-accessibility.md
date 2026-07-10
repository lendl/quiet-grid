# Board Visual Accessibility

Conventions applied across games to make grid tap targets easier to see for players with low vision or motor/memory impairment (e.g. Alzheimer's), without assuming every game's visual identity should change the same way.

## User goal

A player should be able to tell, at a glance, which cells are tappable, which cell is selected/locked, and where the grid's edges are relative to the screen bezel — without relying on subtle shadow or 1px-border differences.

## Architecture goal

Baseline recipe for games built on `createBoundedGridLayout` / `createSharedBoardRenderTokens` (`src/app/shell/boardLayout.ts`, `src/app/shell/renderTokens.ts`):

- **Cell border**: 2px baseline (not 1px). Resting-state border alpha raised to ~0.85-0.95 (was ~0.6-0.84) so ungridded cells don't fade into the board.
- **Given vs. player-entered value**: color-code the digit/letter itself — givens/locked stay `theme.text`, player-entered values render in `theme.primary` (or `theme.primaryLight` in transient animation overlays). Key this off the semantic "given" flag, not the "locked" flag, so a value doesn't revert to neutral color just because a row/box later locks in.
- **Tablet scaling**: games that cap `maxCellSize` (sudoku, wordsearch) had it raised from ~44-56px to 90px so the grid actually grows on tablets. Games that already pass `Number.MAX_SAFE_INTEGER` (takuzu, minesweeper) were left alone — that's the correct pattern, not a bug.
- **Edge padding**: board container horizontal padding raised to ≥24px (previously as low as 8-12px) so the grid doesn't hug the screen bezel. Check the adapter's `boardArea`/`gridArea`/`mainArea` style, not just the grid component — padding often lives one level up.
- **Selected-cell emphasis** (select-then-input flow, e.g. sudoku): face tint alpha raised to ~0.35-0.4 (was ~0.12-0.2), border bumped +1px over the new baseline, so the active cell is unambiguous before typing a digit.
- **Notes/pencil-marks**: font floor raised to ~11px (was 8px).

## Per-game identity caveat

Not every game should get the full recipe automatically — ask before applying if a game has an established look and feel.

- **Minesweeper kept its classic identity**: only took the padding fix, the revealed-cell border alpha bump, and a `theme.primary`-tinted border on hidden cells (color only, at the original 1px width). A first pass that also bumped border width to 2-3px and rescaled the flag glyph was explicitly reverted — those changed the game's visual weight in a way that didn't fit "minesweeper's certain look and feel."
- **Wordsearch had no such constraint** — got the full recipe (padding, border, tablet cap) plus removal of a near-invisible zebra-stripe row tint that wasn't worth the complexity.
- **Chimptest** isn't built on the shared `boardLayout`/`renderTokens` helpers, so it got a bespoke version of the same idea: 3px `theme.primary` border, `theme.surfaceElevated` fill, stronger shadow, 4px red wrong-tap border, plus the same padding bump.
- **Nonogram** has not been reviewed yet.

## File map

- `src/app/shell/boardLayout.ts` — `createBoundedGridLayout` (the `maxCellSize`/`minCellSize` mechanism)
- `src/app/shell/renderTokens.ts` — `createSharedBoardRenderTokens` (`cellRaisedFill`/`cellSunkenFill`); shared across sudoku/takuzu/minesweeper/wordsearch — avoid editing this file directly for a single game's fix, prefer per-game overrides in that game's grid component
- `src/games/sudoku/ui/play/components/SudokuPuzzleGrid.tsx`, `src/games/sudoku/ui/play/adapter.tsx`
- `src/games/takuzu/ui/play/components/TakuzuPuzzleGrid.tsx`
- `src/games/minesweeper/ui/play/components/MinesweeperBoard.tsx`, `src/games/minesweeper/ui/play/adapter.tsx`
- `src/games/wordsearch/ui/play/components/WordSearchPuzzleGrid.tsx`, `src/games/wordsearch/ui/play/adapter.tsx`
- `src/games/chimptest/ui/play/components/ChimpTestGrid.tsx`, `src/games/chimptest/ui/play/adapter.tsx`

## Mistakes to avoid

- Do not blanket-apply the full recipe to a game without checking whether it has an established visual identity worth preserving (see minesweeper).
- Do not edit `renderTokens.ts` to fix one game's contrast — it silently changes every other consumer's look at once. Override colors locally in the game's own grid component instead.
- Do not confuse "locked" (given OR finished/validated) with "given" when color-coding values — using `locked` for color instead of `given` makes a player's correctly-entered digit revert to the neutral/given color once its row/box locks in, silently erasing the distinction the fix was meant to add.
