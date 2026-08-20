package com.quietgrid.app.data

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RepositoriesViewModel @Inject constructor(
    val settingsRepository: SettingsRepository,
    val statsRepository: StatsRepository,
    val sessionRepository: SessionRepository,
) : ViewModel()
