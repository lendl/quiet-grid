package com.quietgrid.app.data

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Hilt-injected access to the app-wide repositories, for screens that read/write settings, stats,
 * or session state directly without otherwise needing a dedicated ViewModel of their own. */
@HiltViewModel
class RepositoriesViewModel @Inject constructor(
    val settingsRepository: SettingsRepository,
    val statsRepository: StatsRepository,
    val sessionRepository: SessionRepository,
) : ViewModel()
