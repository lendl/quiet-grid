package com.quietgrid.app.ui.journey

import com.quietgrid.app.platform.BundledFiles
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.appViewModel
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneyMilestone
import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.core.journey.loadJourneyFile
import com.quietgrid.app.data.JourneyProgressStore
import com.quietgrid.app.data.PlayHistoryStore
import com.quietgrid.app.data.SettingsRepository
import com.quietgrid.app.data.journeyTrack
import com.quietgrid.app.games.animaldoku.AnimalDokuPuzzleBank
import com.quietgrid.app.games.arrowescape.ArrowEscapePuzzleBank
import com.quietgrid.app.games.blockescape.BlockEscapePuzzleBank
import com.quietgrid.app.games.circuit.CircuitPuzzleBank
import com.quietgrid.app.games.killersudoku.KillerSudokuPuzzleBank
import com.quietgrid.app.games.links.LinksPuzzleBank
import com.quietgrid.app.games.minisudoku.MiniSudokuPuzzleBank
import com.quietgrid.app.games.nonogram.NonogramPuzzleBank
import com.quietgrid.app.games.starbattle.StarBattlePuzzleBank
import com.quietgrid.app.games.sudoku.SudokuPuzzleBank
import com.quietgrid.app.games.takuzu.TakuzuPuzzleBank
import com.quietgrid.app.games.wordswap.WordSwapPuzzleBank
import com.quietgrid.app.ui.journey.map.JourneyBoardPreviewData
import com.quietgrid.engine.animaldoku.transformed
import com.quietgrid.engine.arrowescape.transformed
import com.quietgrid.engine.blockescape.BLOCKESCAPE_EXIT_ROW
import com.quietgrid.engine.blockescape.BLOCKESCAPE_SIZE
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val NONOGRAM_PREVIEW_GUIDE = 5

data class JourneyState(val rows: List<JourneyStopRow>, val summary: JourneySummary, val mastered: Boolean = false)

class JourneyViewModel(
    private val files: BundledFiles,
    private val store: JourneyProgressStore,
    private val history: PlayHistoryStore,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val pendingByGame = mutableMapOf<GameId, List<JourneyMilestone>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun state(gameId: GameId): Flow<JourneyState?> = settings.journeyTrack(gameId).flatMapLatest { track ->
        flow {
            val file = loadJourneyFile(files, track)
            if (file == null) {
                emit(null)
                return@flow
            }
            emitAll(
                combine(store.progressFor(gameId), store.isMastered(gameId)) { progress, mastered ->
                    JourneyState(journeyRows(file, progress), journeySummary(file, progress), mastered)
                },
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun activeJourney(enabledGameIds: Set<GameId>): Flow<ActiveJourney?> = history.journeyGameKeysByRecency()
        .map { keys -> journeyCandidates(keys, enabledGameIds) }
        .distinctUntilChanged()
        .flatMapLatest { candidates ->
            if (candidates.isEmpty()) {
                flowOf(null)
            } else {
                combine(candidates.map { gameId -> state(gameId).map { gameId to it?.summary } }) { firstActiveJourney(it.asList()) }
            }
        }
        .distinctUntilChanged()

    suspend fun pendingMilestones(gameId: GameId): List<JourneyMilestone> =
        pendingByGame[gameId] ?: store.pendingMilestones(gameId).also { pendingByGame[gameId] = it }

    fun markShown(gameId: GameId, milestones: List<JourneyMilestone>) {
        viewModelScope.launch { milestones.forEach { store.markShown(gameId, it) } }
    }

    suspend fun playSecondsByStop(gameId: GameId): Map<String, Int> = journeySecondsByStop(history.recordsFor(gameId).first())

    suspend fun playTarget(gameId: GameId, stopId: String): Pair<Difficulty, JourneyRequest>? =
        state(gameId).first()?.rows?.firstOrNull { it.stop.puzzleId == stopId }
            ?.let { Difficulty.fromKey(it.stop.difficulty) to journeyRequestFor(it) }

    suspend fun previewFor(gameId: GameId, difficulty: String, puzzleId: String, transform: Int): JourneyBoardPreviewData? = when (gameId) {
        GameId.ARROWESCAPE -> ArrowEscapePuzzleBank.puzzleById(files, Difficulty.fromKey(difficulty), puzzleId)
            ?.transformed(gridTransformById(transform))
            ?.let { JourneyBoardPreviewData.Arrows(it.rows, it.cols, it.pieces, it.mask) }
        GameId.ANIMALDOKU -> AnimalDokuPuzzleBank.puzzleById(files, Difficulty.fromKey(difficulty), puzzleId)
            ?.transformed(gridTransformById(transform))
            ?.let { JourneyBoardPreviewData.Regions(it.size, it.regions) }
        GameId.STARBATTLE -> StarBattlePuzzleBank.puzzleById(files, Difficulty.fromKey(difficulty), puzzleId)
            ?.transformed(gridTransformById(transform))
            ?.let { JourneyBoardPreviewData.Regions(it.size, it.regions) }
        GameId.SUDOKU, GameId.MINISUDOKU -> {
            val bank = if (gameId == GameId.SUDOKU) SudokuPuzzleBank else MiniSudokuPuzzleBank
            bank.puzzleById(files, Difficulty.fromKey(difficulty), puzzleId)
                ?.transformed(gridTransformById(transform))
                ?.let { digitsPreview(it.givens) }
        }
        GameId.KILLERSUDOKU -> KillerSudokuPuzzleBank.puzzleById(files, Difficulty.fromKey(difficulty), puzzleId)
            ?.transformed(gridTransformById(transform))
            ?.let { entry ->
                val cageOf = MutableList(SUDOKU_SIZE * SUDOKU_SIZE) { 0 }
                entry.cages.forEachIndexed { index, cage -> cage.cells.forEach { cageOf[it] = index } }
                JourneyBoardPreviewData.Cages(SUDOKU_SIZE, SUDOKU_BOX_SIZE, cageOf)
            }
        GameId.NONOGRAM -> NonogramPuzzleBank.puzzleById(files, Difficulty.fromKey(difficulty), puzzleId)
            ?.transformed(gridTransformById(transform))
            ?.let { JourneyBoardPreviewData.EmptyGrid(it.rows, it.cols, NONOGRAM_PREVIEW_GUIDE) }
        GameId.TAKUZU -> TakuzuPuzzleBank.puzzleById(files, Difficulty.fromKey(difficulty), puzzleId)
            ?.transformed(gridTransformById(transform))
            ?.let { JourneyBoardPreviewData.Digits(it.size, it.size, it.size, decodePuzzleBoard(it.solution, it.mask, it.size)) }
        GameId.BLOCKESCAPE -> BlockEscapePuzzleBank.puzzleById(files, Difficulty.fromKey(difficulty), puzzleId)
            ?.let { JourneyBoardPreviewData.Blocks(BLOCKESCAPE_SIZE, it.blocks, BLOCKESCAPE_EXIT_ROW) }
        GameId.CIRCUIT -> CircuitPuzzleBank.puzzleById(files, Difficulty.fromKey(difficulty), puzzleId)
            ?.let { JourneyBoardPreviewData.Wires(it.rows, it.cols, it.start, it.source) }
        GameId.LINKS -> LinksPuzzleBank.puzzleById(files, Difficulty.fromKey(difficulty), puzzleId)
            ?.let { JourneyBoardPreviewData.Pairs(it) }
        GameId.WORDSWAP -> WordSwapPuzzleBank.puzzleById(files, Difficulty.fromKey(difficulty), puzzleId)
            ?.let { JourneyBoardPreviewData.Letters(it.rows, it.cols, it.start) }
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

@Composable
fun journeyViewModel(): JourneyViewModel = appViewModel { graph ->
    JourneyViewModel(graph.bundledFiles, graph.journeyProgressRepository, graph.playHistoryRepository, graph.settingsRepository)
}
