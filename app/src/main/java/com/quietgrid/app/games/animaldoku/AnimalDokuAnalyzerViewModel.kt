package com.quietgrid.app.games.animaldoku

import com.quietgrid.app.ui.analyzer.AnalyzerState
import com.quietgrid.app.ui.analyzer.AnalyzerViewModel
import com.quietgrid.app.ui.analyzer.buildAnalyzerState
import com.quietgrid.engine.animaldoku.AnimalDokuHint
import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

@HiltViewModel(assistedFactory = AnimalDokuAnalyzerViewModel.Factory::class)
class AnimalDokuAnalyzerViewModel @AssistedInject constructor(
    @Assisted private val snapshot: String?,
) : AnalyzerViewModel<AnimalDokuPuzzleEntry, List<List<AnimalDokuCellState>>, AnimalDokuHint>() {

    @AssistedFactory
    interface Factory {
        fun create(snapshot: String?): AnimalDokuAnalyzerViewModel
    }

    override fun detailFrameCount(hint: AnimalDokuHint): Int = animalDokuChainFrameCount(hint)

    override fun computeInitialState(): AnalyzerState<AnimalDokuPuzzleEntry, List<List<AnimalDokuCellState>>, AnimalDokuHint> {
        val persisted = snapshot?.let { runCatching { json.decodeFromString<AnimalDokuPersistedSession>(it) }.getOrNull() }
            ?: return AnalyzerState.LoadFailed
        val puzzle = persisted.puzzle
        val cells = runCatching { decodeAnimalDokuCells(persisted.cells, puzzle.size) }.getOrNull()
            ?: return AnalyzerState.LoadFailed

        return buildAnalyzerState(
            puzzle = puzzle,
            initialBoard = withoutWrongAnimalDokuMarks(puzzle, cells),
            isSolved = ::isAnimalDokuSolved,
            nextHint = { current -> findAnimalDokuCellsHint(puzzle, current) },
            applyHint = ::applyAnimalDokuHintToCells,
        )
    }
}
