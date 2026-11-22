package com.quietgrid.engine.journey

import com.quietgrid.engine.core.Difficulty
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

const val JOURNEY_RATING_MIN = 100
const val JOURNEY_RATING_MAX = 1000
const val JOURNEY_TIER_DIGITS = 15
const val JOURNEY_TIER_SPAN = 1_000_000_000_000_000L

@Serializable
data class RatingPoint(val raw: Long, val rating: Int)

@Serializable
data class JourneyRatingScale(val points: List<RatingPoint>) {
    fun rate(raw: Long): Int {
        require(points.isNotEmpty()) { "Rating scale has no points" }
        if (raw <= points.first().raw) return points.first().rating
        if (raw >= points.last().raw) return points.last().rating
        val upperIndex = points.indexOfFirst { it.raw >= raw }
        val upper = points[upperIndex]
        val lower = points[upperIndex - 1]
        val fraction = (raw - lower.raw).toDouble() / (upper.raw - lower.raw)
        return (lower.rating + fraction * (upper.rating - lower.rating)).roundToInt()
    }
}

fun packRawScore(vararg slots: Pair<Int, Int>): Long {
    require(slots.sumOf { it.second } <= JOURNEY_TIER_DIGITS) { "Raw score slots use more than $JOURNEY_TIER_DIGITS digits" }
    return slots.fold(0L) { packed, (value, digits) ->
        val limit = (1..digits).fold(1L) { power, _ -> power * 10 }
        packed * limit + value.toLong().coerceIn(0, limit - 1)
    }
}

fun tieredRawScore(difficultyKey: String, fine: Long): Long {
    val tier = Difficulty.entries.firstOrNull { it.key == difficultyKey }
        ?: throw IllegalArgumentException("Unknown difficulty '$difficultyKey'")
    return tier.ordinal * JOURNEY_TIER_SPAN + fine.coerceIn(0, JOURNEY_TIER_SPAN - 1)
}
