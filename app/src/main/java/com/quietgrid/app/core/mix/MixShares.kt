package com.quietgrid.app.core.mix

import kotlin.math.roundToInt

private const val MIN_ENTRY_WEIGHT = 1
private const val MAX_ENTRY_WEIGHT = 10

private fun MixEntry.sameSlot(other: MixEntry): Boolean =
    gameId == other.gameId && mode == other.mode && difficulty == other.difficulty

private fun sharePercent(weight: Int, entries: List<MixEntry>): Int {
    val total = entries.sumOf { it.weight }
    if (total <= 0) return 0
    return (weight * 100f / total).roundToInt()
}

fun entrySharePercent(entry: MixEntry, entries: List<MixEntry>): Int = sharePercent(entry.weight, entries)

fun gameSharePercent(gameId: String, entries: List<MixEntry>): Int =
    sharePercent(entries.filter { it.gameId == gameId }.sumOf { it.weight }, entries)

fun stepEntryWeight(entries: List<MixEntry>, target: MixEntry, delta: Int): List<MixEntry> =
    entries.mapNotNull { entry ->
        if (!entry.sameSlot(target)) return@mapNotNull entry
        val newWeight = entry.weight + delta
        if (newWeight < MIN_ENTRY_WEIGHT) null else entry.copy(weight = newWeight.coerceAtMost(MAX_ENTRY_WEIGHT))
    }
