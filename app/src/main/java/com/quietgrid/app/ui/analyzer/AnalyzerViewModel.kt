package com.quietgrid.app.ui.analyzer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val AUTO_PLAY_INTERVAL_MS = 2000L

abstract class AnalyzerViewModel<Puzzle, Board, Hint> : ViewModel() {

    var state: AnalyzerState<Puzzle, Board, Hint> by mutableStateOf(AnalyzerState.Loading)
        private set

    internal var replayDispatcher: CoroutineDispatcher = Dispatchers.Default

    private var playJob: Job? = null
    private var hasStarted = false

    protected abstract fun computeInitialState(): AnalyzerState<Puzzle, Board, Hint>

    open fun detailFrameCount(hint: Hint): Int = 0

    private val ready: AnalyzerState.Ready<Puzzle, Board, Hint>?
        get() = state as? AnalyzerState.Ready<Puzzle, Board, Hint>

    fun load() {
        if (hasStarted) return
        hasStarted = true
        viewModelScope.launch {
            state = withContext(replayDispatcher) { computeInitialState() }
        }
    }

    private fun advanced(current: AnalyzerState.Ready<Puzzle, Board, Hint>): AnalyzerState.Ready<Puzzle, Board, Hint> {
        val detailIndex = current.detailIndex
        val hint = current.hint
        if (detailIndex != null && hint != null && detailIndex < detailFrameCount(hint) - 1) {
            return current.copy(detailIndex = detailIndex + 1)
        }
        return current.copy(currentIndex = current.currentIndex + 1, detailIndex = null)
    }

    fun next() {
        val current = ready ?: return
        if (current.isFinalFrame) {
            stopPlaying(current)
            return
        }
        state = advanced(current)
    }

    fun back() {
        val current = ready ?: return
        val detailIndex = current.detailIndex
        state = when {
            detailIndex != null -> current.copy(detailIndex = if (detailIndex > 0) detailIndex - 1 else null)
            current.currentIndex > 0 -> current.copy(currentIndex = current.currentIndex - 1)
            else -> return
        }
    }

    fun showDetail() {
        val current = ready ?: return
        val hint = current.hint ?: return
        if (detailFrameCount(hint) > 0) state = current.copy(detailIndex = 0)
    }

    fun hideDetail() {
        val current = ready ?: return
        state = current.copy(detailIndex = null)
    }

    fun togglePlay() {
        val current = ready ?: return
        if (current.isPlaying) {
            stopPlaying(current)
            return
        }
        state = current.copy(isPlaying = true)
        playJob = viewModelScope.launch {
            while (true) {
                delay(AUTO_PLAY_INTERVAL_MS)
                val playing = ready ?: return@launch
                if (!playing.isPlaying) return@launch
                if (playing.isFinalFrame) {
                    state = playing.copy(isPlaying = false)
                    return@launch
                }
                state = advanced(playing)
            }
        }
    }

    private fun stopPlaying(current: AnalyzerState.Ready<Puzzle, Board, Hint>) {
        playJob?.cancel()
        playJob = null
        state = current.copy(isPlaying = false)
    }
}
