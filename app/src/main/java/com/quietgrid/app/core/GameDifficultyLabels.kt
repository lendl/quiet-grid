package com.quietgrid.app.core

import com.quietgrid.app.games.animaldoku.animalDokuDifficultyLabelRes
import com.quietgrid.app.games.arrowescape.arrowEscapeDifficultyLabelRes
import com.quietgrid.app.games.blockfill.blockFillDifficultyLabelRes
import com.quietgrid.app.games.chimptest.chimpDifficultyLabelRes
import com.quietgrid.app.games.flowfree.flowFreeDifficultyLabelRes
import com.quietgrid.app.games.guessbynumbers.guessByNumbersDifficultyLabelRes
import com.quietgrid.app.games.minesweeper.minesweeperDifficultyLabelRes
import com.quietgrid.app.games.nback.nbackDifficultyLabelRes
import com.quietgrid.app.games.nonogram.nonogramDifficultyLabelRes
import com.quietgrid.app.games.starbattle.starBattleDifficultyLabelRes
import com.quietgrid.app.games.sudoku.sudokuDifficultyLabelRes
import com.quietgrid.app.games.takuzu.takuzuDifficultyLabelRes
import com.quietgrid.app.games.wordguess.wordGuessDifficultyLabelRes
import com.quietgrid.app.games.wordsearch.wordSearchDifficultyLabelRes

fun gameDifficultyLabelRes(gameId: GameId, difficulty: Difficulty): Int = when (gameId) {
    GameId.TAKUZU -> takuzuDifficultyLabelRes(difficulty)
    GameId.NONOGRAM -> nonogramDifficultyLabelRes(difficulty)
    GameId.MINESWEEPER -> minesweeperDifficultyLabelRes(difficulty)
    GameId.SUDOKU -> sudokuDifficultyLabelRes(difficulty)
    GameId.WORDSEARCH -> wordSearchDifficultyLabelRes(difficulty)
    GameId.BLOCKFILL -> blockFillDifficultyLabelRes(difficulty)
    GameId.WORDGUESS -> wordGuessDifficultyLabelRes(difficulty)
    GameId.ANIMALDOKU -> animalDokuDifficultyLabelRes(difficulty)
    GameId.ARROWESCAPE -> arrowEscapeDifficultyLabelRes(difficulty)
    GameId.STARBATTLE -> starBattleDifficultyLabelRes(difficulty)
    GameId.GUESSBYNUMBERS -> guessByNumbersDifficultyLabelRes(difficulty)
    GameId.NBACK -> nbackDifficultyLabelRes(difficulty)
    GameId.FLOWFREE -> flowFreeDifficultyLabelRes(difficulty)
    else -> chimpDifficultyLabelRes(difficulty)
}
