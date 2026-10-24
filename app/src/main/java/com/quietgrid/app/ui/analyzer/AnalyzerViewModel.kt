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

    private val ready: AnalyzerState.Ready<Puzzle, Board, Hint>?
        get() = state as? AnalyzerState.Ready<Puzzle, Board, Hint>

    fun load() {
        if (hasStarted) return
        hasStarted = true
        viewModelScope.launch {
            state = withContext(replayDispatcher) { computeInitialState() }
        }
    }

    fun next() {
        val current = ready ?: return
        if (current.isFinalFrame) {
            stopPlaying(current)
            return
        }
        state = current.copy(currentIndex = current.currentIndex + 1)
    }

    fun back() {
        val current = ready ?: return
        if (current.currentIndex <= 0) return
        state = current.copy(currentIndex = current.currentIndex - 1)
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
                state = playing.copy(currentIndex = playing.currentIndex + 1)
            }
        }
    }

    private fun stopPlaying(current: AnalyzerState.Ready<Puzzle, Board, Hint>) {
        playJob?.cancel()
        playJob = null
        state = current.copy(isPlaying = false)
    }
}
