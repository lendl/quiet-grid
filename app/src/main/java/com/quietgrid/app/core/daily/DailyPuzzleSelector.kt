package com.quietgrid.app.core.daily

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.random.Random

const val DAILY_MIN_POOL: Int = 90

val DAILY_EPOCH: LocalDate = LocalDate.of(2026, 1, 1)

private const val FNV_PRIME: Long = 0x100000001b3L

fun fnv1a64(text: String): Long {
    var hash = 0xcbf29ce484222325uL.toLong()
    for (byte in text.toByteArray(Charsets.UTF_8)) {
        hash = hash xor (byte.toLong() and 0xff)
        hash *= FNV_PRIME
    }
    return hash
}

fun <T> selectDaily(pool: List<T>, idOf: (T) -> String, gameKey: String, tierKey: String, date: LocalDate): T? {
    if (pool.isEmpty()) return null
    val ordered = pool.sortedBy(idOf).toMutableList()
    val random = Random(fnv1a64("$gameKey|$tierKey"))
    for (i in ordered.lastIndex downTo 1) {
        val j = random.nextInt(i + 1)
        val swap = ordered[i]
        ordered[i] = ordered[j]
        ordered[j] = swap
    }
    val dayIndex = ChronoUnit.DAYS.between(DAILY_EPOCH, date)
    return ordered[Math.floorMod(dayIndex, ordered.size.toLong()).toInt()]
}
