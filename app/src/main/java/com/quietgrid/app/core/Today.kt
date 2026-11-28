package com.quietgrid.app.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

fun today(clock: Clock = Clock.System, zone: TimeZone = TimeZone.currentSystemDefault()): LocalDate = clock.todayIn(zone)
