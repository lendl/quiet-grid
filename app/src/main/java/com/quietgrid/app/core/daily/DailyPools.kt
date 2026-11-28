package com.quietgrid.app.core.daily

import com.quietgrid.app.platform.BundledFiles
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.games.animaldoku.AnimalDokuPuzzleBank
import com.quietgrid.app.games.minisudoku.MiniSudokuPuzzleBank
import com.quietgrid.app.games.sudoku.SudokuPuzzleBank
import com.quietgrid.app.games.takuzu.TakuzuPuzzleBank
import com.quietgrid.app.games.whodidit.WhoDidItPuzzleBank
import com.quietgrid.app.games.wordguess.WordGuessPuzzleBank
import com.quietgrid.app.games.wordguess.currentWordGuessLocale
import com.quietgrid.app.games.wordsearch.WordSearchPuzzleBank
import com.quietgrid.app.games.wordsearch.currentWordSearchLocale

object DailyPools {
    private suspend fun poolSize(files: BundledFiles, gameId: GameId, difficulty: Difficulty, puzzleLanguage: String): Int = when (gameId) {
        GameId.SUDOKU -> SudokuPuzzleBank.dailyPool(files, difficulty).size
        GameId.MINISUDOKU -> MiniSudokuPuzzleBank.dailyPool(files, difficulty).size
        GameId.TAKUZU -> TakuzuPuzzleBank.dailyPool(files, difficulty).size
        GameId.ANIMALDOKU -> AnimalDokuPuzzleBank.dailyPool(files, difficulty).size
        GameId.WHODIDIT -> WhoDidItPuzzleBank.dailyPool(files, difficulty).size
        GameId.WORDGUESS -> WordGuessPuzzleBank.dailyPool(files, currentWordGuessLocale(puzzleLanguage), difficulty).size
        GameId.WORDSEARCH -> WordSearchPuzzleBank.dailyPoolSize(files, currentWordSearchLocale(puzzleLanguage), difficulty)
        else -> 0
    }

    suspend fun poolSizes(files: BundledFiles, gameId: GameId, puzzleLanguage: String): Map<Difficulty, Int> =
        Difficulty.entries.associateWith { poolSize(files, gameId, it, puzzleLanguage) }
}
