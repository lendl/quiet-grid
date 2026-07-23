package com.quietgrid.app.games.wordsearch

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.SessionRepository
import com.quietgrid.app.data.StatsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

data class WordSearchResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
)

class WordSearchPlayViewModel(
    private val appContext: Context,
    private val sessionRepository: SessionRepository,
    private val statsRepository: StatsRepository,
    private val requestedDifficulty: Difficulty,
    private val resume: Boolean,
) : ViewModel() {

    var session by mutableStateOf<WordSearchSession?>(null)
        private set
    var elapsedSeconds by mutableStateOf(0.0)
        private set
    var nextMoveHint by mutableStateOf<WSNextMoveHint?>(null)
        private set

    private var difficulty: Difficulty = requestedDifficulty
    private var finalized = false

    private val _result = MutableSharedFlow<WordSearchResult>(extraBufferCapacity = 1)
    val result: SharedFlow<WordSearchResult> = _result

    init {
        viewModelScope.launch {
            session = if (resume) restoreOrCreate() else freshSession(requestedDifficulty)
            runTicker()
        }
    }

    private suspend fun freshSession(difficulty: Difficulty): WordSearchSession? {
        val entry = WordSearchPuzzleBank.randomPuzzle(appContext, difficulty) ?: return null
        return WordSearchSession(
            puzzle = entry,
            foundWordIds = emptyList(),
            tempSelection = null,
            hiddenWordMode = false,
            hiddenWordProgress = emptyList(),
            hiddenWordSolved = false,
        )
    }

    private suspend fun restoreOrCreate(): WordSearchSession? {
        val envelope = sessionRepository.activeSession.first()
        if (envelope != null && envelope.gameId == GameId.WORDSEARCH.key) {
            val persisted = runCatching { json.decodeFromString<WordSearchPersistedSession>(envelope.payload) }.getOrNull()
            if (persisted != null) {
                elapsedSeconds = envelope.elapsedSeconds
                difficulty = Difficulty.fromKey(persisted.puzzle.difficulty)
                return WordSearchSession(
                    puzzle = persisted.puzzle,
                    foundWordIds = persisted.foundWordIds,
                    tempSelection = null,
                    hiddenWordMode = persisted.hiddenWordMode,
                    hiddenWordProgress = persisted.hiddenWordProgress,
                    hiddenWordSolved = persisted.hiddenWordSolved,
                )
            }
        }
        return freshSession(requestedDifficulty)
    }

    private suspend fun runTicker() {
        while (true) {
            delay(1000)
            if (finalized || session == null) continue
            elapsedSeconds += 1.0
            persistIfMeaningful()
        }
    }

    private fun onCommitSelection() {
        val current = session ?: return
        if (finalized || current.hiddenWordMode) return
        val updated = wsCommitSelection(current) ?: return
        session = updated
        persistIfMeaningful()
    }

    /**
     * Tap-to-select: tapping a cell already in the active selection clears it; tapping with no
     * selection starts one; tapping again tries to extend the selection in a straight line and
     * commits immediately if it lines up, otherwise starts a fresh selection at the tapped cell.
     * Mirrors the RN app's actual play-screen behavior (WordSearchPuzzleGrid is used with
     * allowDrag={false} there), which is tap-only rather than drag-select — drag is instead
     * reserved for panning the board once zoomed in.
     */
    fun onCellTap(row: Int, col: Int) {
        val current = session ?: return
        if (finalized) return
        if (current.hiddenWordMode) {
            onHiddenWordCellTap(row, col)
            return
        }
        nextMoveHint = null
        val cell = WSCellRef(row, col)
        val existingSelection = current.tempSelection
        if (existingSelection != null && cell in existingSelection.path) {
            session = wsClearSelection(current) ?: current
            return
        }
        if (existingSelection == null) {
            session = wsBeginSelection(current, cell) ?: return
            return
        }
        val updated = wsUpdateSelection(current, cell)
        if (updated != null) {
            session = updated
            onCommitSelection()
        } else {
            session = wsBeginSelection(current, cell) ?: return
        }
    }

    fun onHiddenWordCellTap(row: Int, col: Int) {
        val current = session ?: return
        if (finalized) return
        nextMoveHint = null
        val updated = wsInputHiddenWordCell(current, WSCellRef(row, col)) ?: return
        session = updated
        persistIfMeaningful()
        if (updated.hiddenWordSolved) finishAsWin()
    }

    fun onToggleHiddenWordMode() {
        val current = session ?: return
        if (finalized || current.foundWordIds.size < current.puzzle.words.size) return
        nextMoveHint = null
        session = wsToggleHiddenWordMode(current) ?: return
    }

    fun toggleNextMoveHint() {
        if (finalized) return
        if (nextMoveHint != null) {
            nextMoveHint = null
            return
        }
        val current = session ?: return
        nextMoveHint = wsNextMoveHint(current)
    }

    fun endPuzzle() {
        if (finalized) return
        finishAsLoss("abandoned")
    }

    private fun finishAsWin() {
        if (finalized) return
        finalized = true
        val current = session ?: return
        val score = wordSearchScore(elapsedSeconds.toInt(), current)
        viewModelScope.launch {
            val previous = statsRepository.statsFor(GameId.WORDSEARCH).first().forDifficulty(difficulty)
            statsRepository.recordResult(GameId.WORDSEARCH, difficulty, solved = true, score = score)
            sessionRepository.clear()
            _result.emit(
                WordSearchResult(
                    difficulty = difficulty,
                    solved = true,
                    score = score,
                    elapsedSeconds = elapsedSeconds.toInt(),
                    lossReason = null,
                    isFirstSolve = previous.solved == 0,
                    isNewHighScore = previous.solved > 0 && score > previous.bestScore,
                ),
            )
        }
    }

    private fun finishAsLoss(reason: String) {
        if (finalized) return
        finalized = true
        viewModelScope.launch {
            statsRepository.recordResult(GameId.WORDSEARCH, difficulty, solved = false, score = 0)
            sessionRepository.clear()
            _result.emit(
                WordSearchResult(
                    difficulty = difficulty,
                    solved = false,
                    score = 0,
                    elapsedSeconds = elapsedSeconds.toInt(),
                    lossReason = reason,
                ),
            )
        }
    }

    private fun persistIfMeaningful() {
        val current = session ?: return
        if (finalized) return
        if (!wordSearchHasMeaningfulProgress(current)) return
        val payload = json.encodeToString(
            WordSearchPersistedSession(
                puzzle = current.puzzle,
                foundWordIds = current.foundWordIds,
                hiddenWordMode = current.hiddenWordMode,
                hiddenWordProgress = current.hiddenWordProgress,
                hiddenWordSolved = current.hiddenWordSolved,
            ),
        )
        viewModelScope.launch {
            sessionRepository.save(
                ActiveSessionEnvelope(
                    gameId = GameId.WORDSEARCH.key,
                    elapsedSeconds = elapsedSeconds,
                    payload = payload,
                ),
            )
        }
    }
}
