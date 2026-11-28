package com.quietgrid.app.ui.screens

import kotlin.uuid.Uuid
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.today as todayDate
import com.quietgrid.app.core.mix.Mix
import com.quietgrid.app.core.mix.MixEntry
import com.quietgrid.app.core.mix.MixEntryMode
import com.quietgrid.app.core.mix.nextMixName
import com.quietgrid.app.core.tips.QuietTip
import com.quietgrid.app.core.tips.SNOOZE_DAYS
import com.quietgrid.app.core.tips.TipInputs
import com.quietgrid.app.core.tips.pickQuietTip
import com.quietgrid.app.data.GamePreferencesRepository
import com.quietgrid.app.data.GameToggle
import com.quietgrid.app.data.MixRepository
import com.quietgrid.app.data.PlayHistoryRepository
import com.quietgrid.app.data.QuietTipsRepository
import com.quietgrid.app.data.SettingsRepository
import com.quietgrid.app.data.resolveGamePlayPreferences
import com.quietgrid.app.games.GameRegistry
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

private const val LOADED_KEY = "quiet_tip_loaded"
private const val MIX_TIP_ENTRY_WEIGHT = 5

class QuietTipViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val quietTipsRepository: QuietTipsRepository,
    private val settingsRepository: SettingsRepository,
    private val gamePreferencesRepository: GamePreferencesRepository,
    private val playHistoryRepository: PlayHistoryRepository,
    private val mixRepository: MixRepository,
) : ViewModel() {
    private val shown = MutableStateFlow<QuietTip?>(null)
    val tip: StateFlow<QuietTip?> = shown.asStateFlow()

    fun load(gameId: GameId, solved: Boolean, eligible: Boolean) {
        if (savedStateHandle.get<Boolean>(LOADED_KEY) == true) return
        savedStateHandle[LOADED_KEY] = true
        if (!eligible) return
        viewModelScope.launch {
            quietTipsRepository.recordResult()
            val today = todayDate()
            val settings = settingsRepository.settings.first()
            val overrides = gamePreferencesRepository.overrides(gameId).first()
            val picked = pickQuietTip(
                TipInputs(
                    gameId = gameId,
                    solved = solved,
                    today = today,
                    history = playHistoryRepository.allRecords().first(),
                    state = quietTipsRepository.state().first(),
                    beQuiet = settings.beQuiet,
                    suggestionsEnabled = settings.suggestionsEnabled,
                    overrides = overrides,
                    effective = resolveGamePlayPreferences(settings, overrides),
                    appReduceMotion = settings.reduceMotionInPlay,
                    hasAnyMix = mixRepository.mixes.first().isNotEmpty(),
                    gameKeepsScreenOn = GameRegistry.of(gameId).capabilities.alwaysKeepScreenOn,
                ),
            ) ?: return@launch
            quietTipsRepository.markShown(picked.key, today)
            shown.value = picked
        }
    }

    fun recordResultExit(early: Boolean) = persist { quietTipsRepository.recordResultExit(early) }

    fun opened(tip: QuietTip) = persist { quietTipsRepository.markOpened(tip.key) }

    fun applySetting(tip: QuietTip) = persist {
        when (tip) {
            is QuietTip.LockRotation -> gamePreferencesRepository.setRotation(tip.gameId, tip.target)
            is QuietTip.KeepAwake -> gamePreferencesRepository.setToggle(tip.gameId, GameToggle.KEEP_SCREEN_ON, true)
            QuietTip.ReduceMotion -> settingsRepository.setPlayDefault(GameToggle.REDUCE_MOTION, true)
            is QuietTip.MakeMix, QuietTip.TryDaily -> return@persist
        }
        quietTipsRepository.close(tip.key)
    }

    fun undoSetting(tip: QuietTip) = persist {
        when (tip) {
            is QuietTip.LockRotation -> gamePreferencesRepository.setRotation(tip.gameId, null)
            is QuietTip.KeepAwake -> gamePreferencesRepository.setToggle(tip.gameId, GameToggle.KEEP_SCREEN_ON, null)
            QuietTip.ReduceMotion -> settingsRepository.setPlayDefault(GameToggle.REDUCE_MOTION, false)
            is QuietTip.MakeMix, QuietTip.TryDaily -> return@persist
        }
        quietTipsRepository.reopen(tip.key)
    }

    fun makeMix(tip: QuietTip.MakeMix, nameForNumber: (Int) -> String, openEditor: (String) -> Unit) {
        shown.value = null
        persist {
            quietTipsRepository.close(tip.key)
            val mix = Mix(
                id = Uuid.random().toString(),
                name = nextMixName(mixRepository.mixes.first().map { it.name }, nameForNumber),
                entries = tip.games.map { (gameId, difficulty) ->
                    MixEntry(gameId.key, MixEntryMode.PUZZLE, difficulty.key, weight = MIX_TIP_ENTRY_WEIGHT)
                },
            )
            mixRepository.saveMix(mix)
            openEditor(mix.id)
        }
    }

    fun tryDaily(tip: QuietTip, openDaily: () -> Unit) {
        shown.value = null
        persist {
            quietTipsRepository.close(tip.key)
            openDaily()
        }
    }

    fun hide() {
        shown.value = null
    }

    fun quietThis(tip: QuietTip) {
        shown.value = null
        persist { quietTipsRepository.close(tip.key) }
    }

    fun quietAll() {
        shown.value = null
        persist { settingsRepository.setSuggestionsEnabled(false) }
    }

    fun snooze(tip: QuietTip) {
        shown.value = null
        persist { quietTipsRepository.snooze(tip.key, todayDate().plus(SNOOZE_DAYS, DateTimeUnit.DAY)) }
    }

    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch { withContext(NonCancellable) { block() } }
    }
}
