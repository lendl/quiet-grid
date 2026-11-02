package com.quietgrid.cli.journey

import com.quietgrid.engine.journey.JOURNEY_RATING_MAX
import com.quietgrid.engine.journey.JOURNEY_RATING_MIN
import com.quietgrid.engine.journey.JourneyRatingScale
import com.quietgrid.engine.journey.RatingPoint
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.roundToInt

@Serializable
data class JourneyTargetSeconds(val atMinRating: Int, val atMaxRating: Int)

@Serializable
data class JourneyGameScale(
    val targetSeconds: JourneyTargetSeconds,
    val points: List<RatingPoint> = emptyList(),
) {
    val scale: JourneyRatingScale get() = JourneyRatingScale(points)
}

private val scalesJson = Json {
    prettyPrint = true
    encodeDefaults = true
}

private val scalesSerializer = MapSerializer(String.serializer(), JourneyGameScale.serializer())

fun readJourneyScales(path: String): Map<String, JourneyGameScale> =
    scalesJson.decodeFromString(scalesSerializer, File(path).readText())

fun writeJourneyScales(path: String, scales: Map<String, JourneyGameScale>) {
    File(path).writeText(scalesJson.encodeToString(scalesSerializer, scales.toSortedMap()) + "\n")
}

fun buildRatingScale(raws: List<Long>): List<RatingPoint> {
    require(raws.isNotEmpty()) { "Cannot build a rating scale from no puzzles" }
    val sorted = raws.sorted()
    val step = (JOURNEY_RATING_MAX - JOURNEY_RATING_MIN) / 100
    return (0..100)
        .map { q -> sorted[(q * (sorted.size - 1) / 100.0).roundToInt()] to JOURNEY_RATING_MIN + q * step }
        .groupBy({ it.first }, { it.second })
        .map { (raw, ratings) -> RatingPoint(raw, ratings.average().roundToInt()) }
}

fun targetSecondsFor(rating: Int, target: JourneyTargetSeconds): Int {
    val fraction = (rating - JOURNEY_RATING_MIN).toDouble() / (JOURNEY_RATING_MAX - JOURNEY_RATING_MIN)
    val seconds = target.atMinRating + fraction * (target.atMaxRating - target.atMinRating)
    return (seconds / 10).roundToInt() * 10
}
