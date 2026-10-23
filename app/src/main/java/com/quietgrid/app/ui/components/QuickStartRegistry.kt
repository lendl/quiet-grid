package com.quietgrid.app.ui.components

import com.quietgrid.app.core.GameId
import com.quietgrid.app.games.animaldoku.AnimalDokuQuickStart
import com.quietgrid.app.games.arrowescape.ArrowEscapeQuickStart
import com.quietgrid.app.games.blockfill.BlockFillQuickStart
import com.quietgrid.app.games.chimptest.ChimpTestQuickStart
import com.quietgrid.app.games.game2048.Game2048QuickStart
import com.quietgrid.app.games.guessbynumbers.GuessByNumbersQuickStart
import com.quietgrid.app.games.hashi.HashiQuickStart
import com.quietgrid.app.games.links.LinksQuickStart
import com.quietgrid.app.games.killersudoku.KillerSudokuQuickStart
import com.quietgrid.app.games.minesweeper.MinesweeperQuickStart
import com.quietgrid.app.games.minisudoku.MiniSudokuQuickStart
import com.quietgrid.app.games.nback.NBackQuickStart
import com.quietgrid.app.games.nonogram.NonogramQuickStart
import com.quietgrid.app.games.sequencememory.SequenceMemoryQuickStart
import com.quietgrid.app.games.starbattle.StarBattleQuickStart
import com.quietgrid.app.games.sudoku.SudokuQuickStart
import com.quietgrid.app.games.takuzu.TakuzuQuickStart
import com.quietgrid.app.games.themeclear.ThemeClearQuickStart
import com.quietgrid.app.games.whodidit.WhoDidItQuickStart
import com.quietgrid.app.games.wordguess.WordGuessQuickStart
import com.quietgrid.app.games.wordsearch.WordSearchQuickStart

internal fun quickStartFor(gameId: GameId): QuickStartContent = when (gameId) {
    GameId.CHIMPTEST -> ChimpTestQuickStart
    GameId.TAKUZU -> TakuzuQuickStart
    GameId.NONOGRAM -> NonogramQuickStart
    GameId.MINESWEEPER -> MinesweeperQuickStart
    GameId.SUDOKU -> SudokuQuickStart
    GameId.WORDSEARCH -> WordSearchQuickStart
    GameId.BLOCKFILL -> BlockFillQuickStart
    GameId.WORDGUESS -> WordGuessQuickStart
    GameId.ANIMALDOKU -> AnimalDokuQuickStart
    GameId.ARROWESCAPE -> ArrowEscapeQuickStart
    GameId.GAME_2048 -> Game2048QuickStart
    GameId.STARBATTLE -> StarBattleQuickStart
    GameId.WHODIDIT -> WhoDidItQuickStart
    GameId.GUESSBYNUMBERS -> GuessByNumbersQuickStart
    GameId.NBACK -> NBackQuickStart
    GameId.THEMECLEAR -> ThemeClearQuickStart
    GameId.SEQUENCEMEMORY -> SequenceMemoryQuickStart
    GameId.HASHI -> HashiQuickStart
    GameId.LINKS -> LinksQuickStart
    GameId.KILLERSUDOKU -> KillerSudokuQuickStart
    GameId.MINISUDOKU -> MiniSudokuQuickStart
}
