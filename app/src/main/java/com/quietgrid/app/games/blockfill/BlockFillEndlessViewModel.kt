package com.quietgrid.app.games.blockfill

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.SESSION_MODE_ENDLESS
import com.quietgrid.app.data.SessionStore
import com.quietgrid.app.data.StatsStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val endlessJson = Json { ignoreUnknownKeys = true }
private const val ENDLESS_FINISH_DELAY_MS = 450L
const val BLOCKFILL_ENDLESS_REASON_STUCK = "stuck"
const val BLOCKFILL_ENDLESS_REASON_ABANDONED = "abandoned"

data class BlockFillEndlessResult(
    val score: Int,
    val levelReached: Int,
    val linesCleared: Int,
    val moves: Int,
    val isNewBest: Boolean,
    val previousBest: Int,
    val reason: String,
)

@HiltViewModel(assistedFactory = BlockFillEndlessViewModel.Factory::class)
class BlockFillEndlessViewModel @AssistedInject constructor(
    private val sessionStore: SessionStore,
    private val statsStore: StatsStore,
    @Assisted resume: Boolean,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(resume: Boolean): BlockFillEndlessViewModel
    }

    var session by mutableStateOf<BlockFillEndlessSession?>(null)
        private set

    private val _result = MutableSharedFlow<BlockFillEndlessResult>(extraBufferCapacity = 1)
    val result: SharedFlow<BlockFillEndlessResult> = _result

    private var finalized = false

    init {
        viewModelScope.launch {
            val restored = if (resume) restoreRun() else null
            if (restored != null) {
                session = restored
            } else {
                val best = statsStore.endlessStatsFor(GameId.BLOCKFILL).first().bestScore
                val fresh = createBlockFillEndlessSession(bestAtStart = best)
                session = fresh
                save(fresh)
            }
        }
    }

    fun onPlacePiece(pieceIndex: Int, anchorRow: Int, anchorCol: Int): Boolean {
        if (finalized) return false
        val current = session ?: return false
        val next = applyBlockFillEndlessPlacement(current, pieceIndex, anchorRow, anchorCol) ?: return false
        session = next
        if (next.status == BlockFillStatus.LOST) {
            finalizeRun(next, BLOCKFILL_ENDLESS_REASON_STUCK)
        } else {
            viewModelScope.launch { save(next) }
        }
        return true
    }

    fun endRun() {
        val current = session ?: return
        finalizeRun(current, BLOCKFILL_ENDLESS_REASON_ABANDONED)
    }

    private suspend fun restoreRun(): BlockFillEndlessSession? {
        val envelope = sessionStore.activeSession.first() ?: return null
        if (envelope.gameId != GameId.BLOCKFILL.key || envelope.mode != SESSION_MODE_ENDLESS) return null
        val restored = runCatching { endlessJson.decodeFromString(BlockFillEndlessSession.serializer(), envelope.payload) }.getOrNull()
        return restored?.takeIf { it.status == BlockFillStatus.PLAYING }
    }

    private suspend fun save(run: BlockFillEndlessSession) {
        sessionStore.save(
            ActiveSessionEnvelope(
                gameId = GameId.BLOCKFILL.key,
                elapsedSeconds = 0.0,
                payload = endlessJson.encodeToString(BlockFillEndlessSession.serializer(), run),
                mode = SESSION_MODE_ENDLESS,
            ),
        )
    }

    private fun finalizeRun(run: BlockFillEndlessSession, reason: String) {
        if (finalized) return
        finalized = true
        viewModelScope.launch {
            statsStore.recordEndlessResult(GameId.BLOCKFILL, run.score, run.multiplier)
            sessionStore.clear()
            delay(ENDLESS_FINISH_DELAY_MS)
            _result.emit(
                BlockFillEndlessResult(
                    score = run.score,
                    levelReached = run.multiplier,
                    linesCleared = run.linesCleared,
                    moves = run.moves,
                    isNewBest = run.score > run.bestAtStart,
                    previousBest = run.bestAtStart,
                    reason = reason,
                ),
            )
        }
    }
}
