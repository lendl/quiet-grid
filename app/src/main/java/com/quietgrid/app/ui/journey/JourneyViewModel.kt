package com.quietgrid.app.ui.journey

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.core.journey.loadJourneyFile
import com.quietgrid.app.data.JourneyProgressStore
import com.quietgrid.app.games.animaldoku.AnimalDokuPuzzleBank
import com.quietgrid.app.games.arrowescape.ArrowEscapePuzzleBank
import com.quietgrid.app.games.killersudoku.KillerSudokuPuzzleBank
import com.quietgrid.app.games.minisudoku.MiniSudokuPuzzleBank
import com.quietgrid.app.games.nonogram.NonogramPuzzleBank
import com.quietgrid.app.games.starbattle.StarBattlePuzzleBank
import com.quietgrid.app.games.sudoku.SudokuPuzzleBank
import com.quietgrid.app.games.takuzu.TakuzuPuzzleBank
import com.quietgrid.app.ui.journey.map.JourneyBoardPreviewData
import com.quietgrid.engine.animaldoku.transformed
import com.quietgrid.engine.arrowescape.transformed
import com.quietgrid.engine.starbattle.transformed
import com.quietgrid.engine.killersudoku.transformed
import com.quietgrid.engine.nonogram.transformed
import com.quietgrid.engine.sudoku.SUDOKU_BOX_SIZE
import com.quietgrid.engine.sudoku.SUDOKU_SIZE
import com.quietgrid.engine.sudoku.SudokuLayout
import com.quietgrid.engine.sudoku.transformed
import com.quietgrid.engine.takuzu.decodePuzzleBoard
import com.quietgrid.engine.takuzu.transformed
import com.quietgrid.engine.journey.gridTransformById
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val NONOGRAM_PREVIEW_GUIDE = 5

data class JourneyState(val rows: List<JourneyStopRow>, val summary: JourneySummary)

@HiltViewModel
class JourneyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: JourneyProgressStore,
) : ViewModel() {

    fun state(gameId: GameId): Flow<JourneyState?> = flow {
        val file = loadJourneyFile(context, gameId)
        if (file == null) {
            emit(null)
            return@flow
        }
        emitAll(store.progressFor(gameId).map { progress -> JourneyState(journeyRows(file, progress), journeySummary(file, progress)) })
    }

    suspend fun playTarget(gameId: GameId, stopId: String): Pair<Difficulty, JourneyRequest>? =
        state(gameId).first()?.rows?.firstOrNull { it.stop.puzzleId == stopId }
            ?.let { Difficulty.fromKey(it.stop.difficulty) to journeyRequestFor(it) }

    suspend fun previewFor(gameId: GameId, difficulty: String, puzzleId: String, transform: Int): JourneyBoardPreviewData? = when (gameId) {
        GameId.ARROWESCAPE -> ArrowEscapePuzzleBank.puzzleById(context, Difficulty.fromKey(difficulty), puzzleId)
            ?.transformed(gridTransformById(transform))
            ?.let { JourneyBoardPreviewData.Arrows(it.rows, it.cols, it.pieces, it.mask) }
        GameId.ANIMALDOKU -> AnimalDokuPuzzleBank.puzzleById(context, Difficulty.fromKey(difficulty), puzzleId)
            ?.transformed(gridTransformById(transform))
            ?.let { JourneyBoardPreviewData.Regions(it.size, it.regions) }
        GameId.STARBATTLE -> StarBattlePuzzleBank.puzzleById(context, Difficulty.fromKey(difficulty), puzzleId)
            ?.transformed(gridTransformById(transform))
            ?.let { JourneyBoardPreviewData.Regions(it.size, it.regions) }
        GameId.SUDOKU, GameId.MINISUDOKU -> {
            val bank = if (gameId == GameId.SUDOKU) SudokuPuzzleBank else MiniSudokuPuzzleBank
            bank.puzzleById(context, Difficulty.fromKey(difficulty), puzzleId)
                ?.transformed(gridTransformById(transform))
                ?.let { digitsPreview(it.givens) }
        }
        GameId.KILLERSUDOKU -> KillerSudokuPuzzleBank.puzzleById(context, Difficulty.fromKey(difficulty), puzzleId)
            ?.transformed(gridTransformById(transform))
            ?.let { entry ->
                val cageOf = MutableList(SUDOKU_SIZE * SUDOKU_SIZE) { 0 }
                entry.cages.forEachIndexed { index, cage -> cage.cells.forEach { cageOf[it] = index } }
                JourneyBoardPreviewData.Cages(SUDOKU_SIZE, SUDOKU_BOX_SIZE, cageOf)
            }
        GameId.NONOGRAM -> NonogramPuzzleBank.puzzleById(context, Difficulty.fromKey(difficulty), puzzleId)
            ?.transformed(gridTransformById(transform))
            ?.let { JourneyBoardPreviewData.EmptyGrid(it.rows, it.cols, NONOGRAM_PREVIEW_GUIDE) }
        GameId.TAKUZU -> TakuzuPuzzleBank.puzzleById(context, Difficulty.fromKey(difficulty), puzzleId)
            ?.transformed(gridTransformById(transform))
            ?.let { JourneyBoardPreviewData.Digits(it.size, it.size, it.size, decodePuzzleBoard(it.solution, it.mask, it.size)) }
        else -> null
    }

    private fun digitsPreview(givens: List<List<Int?>>): JourneyBoardPreviewData.Digits {
        val layout = SudokuLayout.forSize(givens.size)
        return JourneyBoardPreviewData.Digits(layout.size, layout.boxRows, layout.boxCols, givens)
    }

    fun skip(gameId: GameId, stopId: String) {
        viewModelScope.launch { store.skip(gameId, stopId) }
    }
}
