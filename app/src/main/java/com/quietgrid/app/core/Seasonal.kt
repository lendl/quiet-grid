package com.quietgrid.app.core

import java.time.LocalDate
import java.time.Month

fun isHalloween(date: LocalDate): Boolean = date.month == Month.OCTOBER && date.dayOfMonth == 31
