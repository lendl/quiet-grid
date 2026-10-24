package com.quietgrid.app.games.takuzu

import com.quietgrid.app.ui.analyzer.AnalyzerState
import com.quietgrid.app.ui.analyzer.AnalyzerViewModel
import com.quietgrid.app.ui.analyzer.buildAnalyzerState
import com.quietgrid.engine.takuzu.TakuzuGrid
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

data class TakuzuAnalyzerPuzzle(val size: Int, val isGiven: List<List<Boolean>>)

@HiltViewModel(assistedFactory = TakuzuAnalyzerViewModel.Factory::class)
class TakuzuAnalyzerViewModel @AssistedInject constructor(
    @Assisted private val snapshot: String?,
) : AnalyzerViewModel<TakuzuAnalyzerPuzzle, TakuzuGrid, TakuzuNextMoveHint>() {

    @AssistedFactory
    interface Factory {
        fun create(snapshot: String?): TakuzuAnalyzerViewModel
    }

    private fun applyHintToBoard(board: TakuzuGrid, hint: TakuzuNextMoveHint): TakuzuGrid {
        val next = board.map { it.toMutableList() }
        hint.targetCells.forEach { (r, c, v) -> next[r][c] = v }
        return next
    }

    private fun sanitizeBoard(board: TakuzuGrid, solution: TakuzuGrid): TakuzuGrid =
        board.mapIndexed { r, row -> row.mapIndexed { c, value -> if (value != null && value != solution[r][c]) null else value } }

    override fun computeInitialState(): AnalyzerState<TakuzuAnalyzerPuzzle, TakuzuGrid, TakuzuNextMoveHint> {
        val persisted = snapshot?.let { runCatching { json.decodeFromString<TakuzuPersistedSession>(it) }.getOrNull() }
            ?: return AnalyzerState.LoadFailed
        val size = persisted.puzzle.size
        val rawBoard: TakuzuGrid = List(size) { r -> List(size) { c -> persisted.board[r * size + c] } }
        val solution = decodeSolution(persisted.puzzle.solution, size)
        val isGiven = decodeMask(persisted.puzzle.mask, size)

        return buildAnalyzerState(
            puzzle = TakuzuAnalyzerPuzzle(size, isGiven),
            initialBoard = sanitizeBoard(rawBoard, solution),
            isSolved = { isBoardSolved(it, solution) },
            nextHint = { current -> getTakuzuNextMoveHint(current, solution).takeIf { it.targetCells.isNotEmpty() } },
            applyHint = ::applyHintToBoard,
        )
    }
}
