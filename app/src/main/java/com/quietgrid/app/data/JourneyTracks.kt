package com.quietgrid.app.data

import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneyTrack
import com.quietgrid.app.core.journey.journeyTrack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

fun SettingsRepository.journeyTrack(gameId: GameId): Flow<JourneyTrack> =
    settings.map { journeyTrack(gameId, it.puzzleLanguage) }.distinctUntilChanged()
