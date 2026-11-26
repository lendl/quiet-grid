package com.quietgrid.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.data.MixRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmptyMixCleanupViewModel(
    mixId: String,
    mixRepository: MixRepository,
) : ViewModel() {

    init {
        viewModelScope.launch {
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) { mixRepository.deleteMixIfEmpty(mixId) }
            }
        }
    }
}
