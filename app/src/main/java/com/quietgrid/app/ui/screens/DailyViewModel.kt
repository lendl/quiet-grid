package com.quietgrid.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.daily.DailyGameUi
import com.quietgrid.app.core.daily.DailyPools
import com.quietgrid.app.core.daily.buildDailyGames
import com.quietgrid.app.core.daily.dailyEligibleGames
import com.quietgrid.app.core.daily.dailyStreak
import com.quietgrid.app.core.daily.shareDailyResult
import com.quietgrid.app.data.DailyRepository
import com.quietgrid.app.data.PlayHistoryRepository
import com.quietgrid.app.data.SessionRepository
import com.quietgrid.app.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val TODAY_POLL_MS = 60_000L

data class DailyUiState(
    val today: LocalDate,
    val overallStreak: Int,
    val subscribed: Set<GameId>,
    val eligible: List<GameId>,
    val games: List<DailyGameUi>,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DailyViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val dailyRepository: DailyRepository,
    private val historyRepository: PlayHistoryRepository,
    sessionRepository: SessionRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val eligible = dailyEligibleGames()

    private val today = flow {
        while (true) {
            emit(LocalDate.now())
            delay(TODAY_POLL_MS)
        }
    }.distinctUntilChanged()

    private val poolSizes = settingsRepository.settings
        .map { it.puzzleLanguage }
        .distinctUntilChanged()
        .mapLatest { language -> eligible.associateWith { DailyPools.poolSizes(appContext, it, language) } }

    val state: StateFlow<DailyUiState> = combine(
        today,
        dailyRepository.subscribedGames,
        historyRepository.allRecords(),
        sessionRepository.activeSession,
        poolSizes,
    ) { date, subscribed, records, envelope, sizes ->
        val games = buildDailyGames(subscribed, eligible, sizes, records, envelope, date)
        DailyUiState(
            today = date,
            overallStreak = dailyStreak(records, games.map { it.gameId }.toSet(), date),
            subscribed = subscribed,
            eligible = eligible,
            games = games,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DailyUiState(LocalDate.now(), 0, emptySet(), eligible, emptyList()),
    )

    fun setSubscribed(gameId: GameId, subscribed: Boolean) {
        viewModelScope.launch { dailyRepository.setSubscribed(gameId, subscribed) }
    }

    suspend fun share(context: Context, gameId: GameId, difficulty: Difficulty, date: LocalDate) {
        shareDailyResult(context, historyRepository, gameId, difficulty, date.toString())
    }
}
