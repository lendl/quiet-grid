package com.quietgrid.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.data.MixRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel(assistedFactory = EmptyMixCleanupViewModel.Factory::class)
class EmptyMixCleanupViewModel @AssistedInject constructor(
    @Assisted mixId: String,
    mixRepository: MixRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(mixId: String): EmptyMixCleanupViewModel
    }

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
