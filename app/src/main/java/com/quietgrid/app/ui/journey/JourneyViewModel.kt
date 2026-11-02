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
import com.quietgrid.app.ui.journey.map.JourneyBoardPreviewData
import com.quietgrid.engine.animaldoku.transformed
import com.quietgrid.engine.arrowescape.transformed
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
        else -> null
    }

    fun skip(gameId: GameId, stopId: String) {
        viewModelScope.launch { store.skip(gameId, stopId) }
    }
}
