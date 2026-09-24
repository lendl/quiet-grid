package com.quietgrid.app.core.daily

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.games.animaldoku.AnimalDokuPuzzleBank
import com.quietgrid.app.games.sudoku.SudokuPuzzleBank
import com.quietgrid.app.games.takuzu.TakuzuPuzzleBank
import com.quietgrid.app.games.wordguess.WordGuessPuzzleBank
import com.quietgrid.app.games.wordguess.currentWordGuessLocale
import com.quietgrid.app.games.wordsearch.WordSearchPuzzleBank
import com.quietgrid.app.games.wordsearch.currentWordSearchLocale

object DailyPools {
    private suspend fun poolSize(context: Context, gameId: GameId, difficulty: Difficulty, puzzleLanguage: String): Int = when (gameId) {
        GameId.SUDOKU -> SudokuPuzzleBank.dailyPool(context, difficulty).size
        GameId.TAKUZU -> TakuzuPuzzleBank.dailyPool(context, difficulty).size
        GameId.ANIMALDOKU -> AnimalDokuPuzzleBank.dailyPool(context, difficulty).size
        GameId.WORDGUESS -> WordGuessPuzzleBank.dailyPool(context, currentWordGuessLocale(puzzleLanguage), difficulty).size
        GameId.WORDSEARCH -> WordSearchPuzzleBank.dailyPool(context, currentWordSearchLocale(puzzleLanguage), difficulty).size
        else -> 0
    }

    suspend fun poolSizes(context: Context, gameId: GameId, puzzleLanguage: String): Map<Difficulty, Int> =
        Difficulty.entries.associateWith { poolSize(context, gameId, it, puzzleLanguage) }
}
