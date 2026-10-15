package com.quietgrid.app.widget

import com.quietgrid.app.data.MixRepository
import com.quietgrid.app.data.PlayHistoryStore
import com.quietgrid.app.data.SessionRepository
import com.quietgrid.app.data.SettingsRepository
import com.quietgrid.app.data.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

data class QuickPlayRender(val quickPlay: QuickPlayState, val mix: MixWidgetState, val themeMode: ThemeMode)

@Singleton
class QuickPlaySource @Inject constructor(
    sessionRepository: SessionRepository,
    mixRepository: MixRepository,
    historyStore: PlayHistoryStore,
    settingsRepository: SettingsRepository,
) {
    val render: Flow<QuickPlayRender> = combine(
        sessionRepository.activeSessionSummary,
        mixRepository.mixes,
        mixRepository.lastStartedMixId,
        historyStore.allRecords(),
        settingsRepository.settings,
    ) { session, mixes, lastStartedMixId, history, settings ->
        QuickPlayRender(
            quickPlay = quickPlayState(session, history, settings.betaGamesEnabled, settings.favoriteGameIds),
            mix = mixWidgetState(session, mixes, lastStartedMixId, settings.betaGamesEnabled),
            themeMode = settings.themeMode,
        )
    }.distinctUntilChanged()
}
