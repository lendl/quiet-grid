package com.quietgrid.app.ui.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.QuietTipsRepository
import com.quietgrid.app.data.ScreenRotation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuietTipSignalsViewModel @Inject constructor(
    private val quietTipsRepository: QuietTipsRepository,
) : ViewModel(), QuietTipSignals {
    private var dailyTabMarked = false

    override fun rotationBounce(gameId: GameId, target: ScreenRotation) {
        viewModelScope.launch { quietTipsRepository.recordRotationBounce(gameId, target) }
    }

    override fun screenTimeout(gameId: GameId) {
        viewModelScope.launch { quietTipsRepository.recordScreenTimeout(gameId) }
    }

    fun dailyTabOpened() {
        if (dailyTabMarked) return
        dailyTabMarked = true
        viewModelScope.launch { quietTipsRepository.markDailyTabOpened() }
    }
}
