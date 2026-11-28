package com.quietgrid.app.data

import com.quietgrid.app.core.isHalloween
import com.quietgrid.app.core.today
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

suspend fun SettingsRepository.isHalloweenActive(today: LocalDate = today()): Boolean =
    isHalloween(today) && settings.first().seasonalTouchesEnabled
