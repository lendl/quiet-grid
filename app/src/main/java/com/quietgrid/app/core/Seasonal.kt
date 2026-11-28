package com.quietgrid.app.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

fun isHalloween(date: LocalDate): Boolean = date.month == Month.OCTOBER && date.day == 31
