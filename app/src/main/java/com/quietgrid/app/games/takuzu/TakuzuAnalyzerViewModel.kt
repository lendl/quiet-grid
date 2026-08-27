package com.quietgrid.app.games.takuzu

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.ui.analyzer.AnalyzerStep
import com.quietgrid.app.ui.analyzer.replayAnalyzerSteps
import com.quietgrid.engine.takuzu.TakuzuGrid
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }
private const val MAX_ANALYZER_STEPS = 400
private const val AUTO_PLAY_INTERVAL_MS = 2000L

sealed interface TakuzuAnalyzerState {
    data object Loading : TakuzuAnalyzerState
    data object LoadFailed : TakuzuAnalyzerState
    data object AlreadySolved : TakuzuAnalyzerState
    data class Ready(
        val puzzleSize: Int,
        val isGiven: List<List<Boolean>>,
        val steps: List<AnalyzerStep<TakuzuGrid, TakuzuNextMoveHint>>,
        val finalBoard: TakuzuGrid,
        val currentIndex: Int,
        val isPlaying: Boolean,
    ) : TakuzuAnalyzerState
}

@HiltViewModel(assistedFactory = TakuzuAnalyzerViewModel.Factory::class)
class TakuzuAnalyzerViewModel @AssistedInject constructor(
    @Assisted private val snapshot: String?,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(snapshot: String?): TakuzuAnalyzerViewModel
    }

    var state: TakuzuAnalyzerState by mutableStateOf(TakuzuAnalyzerState.Loading)
        private set

    internal var replayDispatcher: CoroutineDispatcher = Dispatchers.Default

    private var playJob: Job? = null
    private var hasStarted = false

    fun load() {
        if (hasStarted) return
        hasStarted = true
        viewModelScope.launch {
            val result = withContext(replayDispatcher) { computeInitialState() }
            state = result
        }
    }

    private fun applyHintToBoard(board: TakuzuGrid, hint: TakuzuNextMoveHint): TakuzuGrid {
        val next = board.map { it.toMutableList() }
        hint.targetCells.forEach { (r, c, v) -> next[r][c] = v }
        return next
    }

    private fun sanitizeBoard(board: TakuzuGrid, solution: TakuzuGrid): TakuzuGrid =
        board.mapIndexed { r, row -> row.mapIndexed { c, value -> if (value != null && value != solution[r][c]) null else value } }

    private fun computeInitialState(): TakuzuAnalyzerState {
        val persisted = snapshot?.let { runCatching { json.decodeFromString<TakuzuPersistedSession>(it) }.getOrNull() }
            ?: return TakuzuAnalyzerState.LoadFailed
        val size = persisted.puzzle.size
        val rawBoard: TakuzuGrid = List(size) { r -> List(size) { c -> persisted.board[r * size + c] } }
        val solution = decodeSolution(persisted.puzzle.solution, size)
        val isGiven = decodeMask(persisted.puzzle.mask, size)
        val board = sanitizeBoard(rawBoard, solution)

        if (isBoardSolved(board, solution)) return TakuzuAnalyzerState.AlreadySolved

        val steps = replayAnalyzerSteps(
            initialBoard = board,
            isSolved = { isBoardSolved(it, solution) },
            nextHint = { current ->
                getTakuzuNextMoveHint(current, solution).takeIf { it.targetCells.isNotEmpty() }
            },
            applyHint = ::applyHintToBoard,
            maxSteps = MAX_ANALYZER_STEPS,
        )

        val finalBoard = if (steps.isEmpty()) board else applyHintToBoard(steps.last().boardBefore, steps.last().hint)
        if (!isBoardSolved(finalBoard, solution)) return TakuzuAnalyzerState.LoadFailed

        return TakuzuAnalyzerState.Ready(size, isGiven, steps, finalBoard, currentIndex = 0, isPlaying = false)
    }

    fun next() {
        val ready = state as? TakuzuAnalyzerState.Ready ?: return
        if (ready.currentIndex >= ready.steps.size) {
            stopPlaying(ready)
            return
        }
        state = ready.copy(currentIndex = ready.currentIndex + 1)
    }

    fun back() {
        val ready = state as? TakuzuAnalyzerState.Ready ?: return
        if (ready.currentIndex <= 0) return
        state = ready.copy(currentIndex = ready.currentIndex - 1)
    }

    fun togglePlay() {
        val ready = state as? TakuzuAnalyzerState.Ready ?: return
        if (ready.isPlaying) {
            stopPlaying(ready)
            return
        }
        state = ready.copy(isPlaying = true)
        playJob = viewModelScope.launch {
            while (true) {
                delay(AUTO_PLAY_INTERVAL_MS)
                val current = state as? TakuzuAnalyzerState.Ready ?: return@launch
                if (!current.isPlaying) return@launch
                if (current.currentIndex >= current.steps.size) {
                    state = current.copy(isPlaying = false)
                    return@launch
                }
                state = current.copy(currentIndex = current.currentIndex + 1)
            }
        }
    }

    private fun stopPlaying(ready: TakuzuAnalyzerState.Ready) {
        playJob?.cancel()
        playJob = null
        state = ready.copy(isPlaying = false)
    }
}
