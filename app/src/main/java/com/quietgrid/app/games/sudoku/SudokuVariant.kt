package com.quietgrid.app.games.sudoku

import com.quietgrid.app.core.GameId
import com.quietgrid.app.games.minisudoku.MiniSudokuPuzzleBank

enum class SudokuVariant(val gameId: GameId) {
    CLASSIC(GameId.SUDOKU),
    MINI(GameId.MINISUDOKU),
    ;

    val bank: SudokuEntryBank
        get() = when (this) {
            CLASSIC -> SudokuPuzzleBank
            MINI -> MiniSudokuPuzzleBank
        }
}
