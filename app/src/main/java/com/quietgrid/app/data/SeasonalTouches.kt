package com.quietgrid.app.data

import com.quietgrid.app.core.isHalloween
import kotlinx.coroutines.flow.first
import java.time.LocalDate

suspend fun SettingsRepository.isHalloweenActive(today: LocalDate = LocalDate.now()): Boolean =
    isHalloween(today) && settings.first().seasonalTouchesEnabled
