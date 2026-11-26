package com.quietgrid.app.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.badges.BADGE_JOURNEY_GAMES
import com.quietgrid.app.core.badges.BadgeCatalog
import com.quietgrid.app.core.badges.BadgeDef
import com.quietgrid.app.core.badges.BadgeProgress
import com.quietgrid.app.core.badges.JourneySnapshot
import com.quietgrid.app.data.BadgeRepository
import com.quietgrid.app.data.SettingsRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private const val SUBSCRIBE_TIMEOUT_MS = 5_000L
private const val ANNOUNCEMENT_LOADED_KEY = "badge_announcement_loaded"
private const val ANNOUNCEMENT_ID_KEY = "badge_announcement_id"

data class MilestonesState(
    val loaded: Boolean = false,
    val earned: Map<String, LocalDate> = emptyMap(),
    val progress: BadgeProgress = BadgeProgress(),
    val journeys: JourneySnapshot = JourneySnapshot(emptyMap(), emptyMap(), emptyMap()),
    val newBadgeId: String? = null,
)

fun MilestonesState.journeyDots(def: BadgeDef): List<Boolean>? = when (def.id) {
    "all_journeys" -> BADGE_JOURNEY_GAMES.map { journeys.journeyFinished[it] == true }
    "starry_road" -> BADGE_JOURNEY_GAMES.map { journeys.allStars[it] == true }
    else -> null
}

private fun Map<String, LocalDate>.knownBadges(): Map<String, LocalDate> = filterKeys { BadgeCatalog.byId(it) != null }

class MilestonesViewModel(private val badgeRepository: BadgeRepository) : ViewModel() {
    private val current = MutableStateFlow(MilestonesState())
    val state: StateFlow<MilestonesState> = current.asStateFlow()

    init {
        viewModelScope.launch {
            var newBadgeId: String? = null
            var first = true
            badgeRepository.overview().collect { overview ->
                val earned = overview.earned.knownBadges()
                if (first) {
                    first = false
                    val unseen = earned.keys.lastOrNull()?.takeIf { it != overview.seenNewest }
                    newBadgeId = unseen
                    if (unseen != null) withContext(NonCancellable) { badgeRepository.markSeen(unseen) }
                }
                current.value = MilestonesState(true, earned, overview.progress, overview.journeys, newBadgeId)
            }
        }
    }
}

data class BadgeSummary(val show: Boolean = false, val earned: Map<String, LocalDate> = emptyMap())

class BadgeSummaryViewModel(
    badgeRepository: BadgeRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val summary: StateFlow<BadgeSummary> = combine(
        settingsRepository.settings.map { it.showMilestones }.distinctUntilChanged(),
        badgeRepository.earned,
    ) { show, earned -> BadgeSummary(show, earned.knownBadges()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MS), BadgeSummary())
}

class BadgeAnnouncementViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val badgeRepository: BadgeRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val badgeId: StateFlow<String?> = savedStateHandle.getStateFlow(ANNOUNCEMENT_ID_KEY, null)

    init {
        if (savedStateHandle.get<Boolean>(ANNOUNCEMENT_LOADED_KEY) != true) {
            savedStateHandle[ANNOUNCEMENT_LOADED_KEY] = true
            viewModelScope.launch {
                val taken = withContext(NonCancellable) { badgeRepository.takeAnnouncement() }
                val show = settingsRepository.settings.first().showMilestones
                savedStateHandle[ANNOUNCEMENT_ID_KEY] = taken?.takeIf { show && BadgeCatalog.byId(it) != null }
            }
        }
    }
}
