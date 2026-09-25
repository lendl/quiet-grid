package com.quietgrid.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.daily.DailyGameUi
import com.quietgrid.app.core.daily.DailyPools
import com.quietgrid.app.core.daily.availableDailyTiers
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val TODAY_POLL_MS = 60_000L

data class DailyUiState(
    val today: LocalDate,
    val overallStreak: Int,
    val subscribed: Map<GameId, Set<Difficulty>>,
    val eligible: List<GameId>,
    val available: Map<GameId, List<Difficulty>>,
    val games: List<DailyGameUi>,
    val dailySolvedTotal: Int = 0,
    val loading: Boolean = false,
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
        .flowOn(Dispatchers.IO)
        .shareIn(viewModelScope, SharingStarted.Lazily, replay = 1)

    val state: StateFlow<DailyUiState> = combine(
        today,
        dailyRepository.subscriptions,
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
            available = eligible
                .associateWith { availableDailyTiers(sizes[it].orEmpty()) }
                .filterValues { it.isNotEmpty() },
            games = games,
            dailySolvedTotal = records.count { it.solved && it.dailyDate != null },
        )
    }.flowOn(Dispatchers.Default).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DailyUiState(LocalDate.now(), 0, emptyMap(), eligible, emptyMap(), emptyList(), loading = true),
    )

    fun setSubscribed(gameId: GameId, subscribed: Boolean) {
        viewModelScope.launch { dailyRepository.setSubscribed(gameId, subscribed) }
    }

    fun setSubscribed(gameId: GameId, difficulty: Difficulty, subscribed: Boolean) {
        viewModelScope.launch { dailyRepository.setSubscribed(gameId, difficulty, subscribed) }
    }

    suspend fun share(context: Context, gameId: GameId, difficulty: Difficulty, date: LocalDate) {
        shareDailyResult(context, historyRepository, gameId, difficulty, date.toString())
    }
}
