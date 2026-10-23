package com.quietgrid.cli.links

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.links.LINKS_PAIR_RANGE_BY_DIFFICULTY
import com.quietgrid.engine.links.LINKS_SIZE_BY_DIFFICULTY
import com.quietgrid.engine.links.LinksPuzzleEntry
import com.quietgrid.engine.links.countLinksSolutions
import com.quietgrid.engine.links.generateLinksSolution
import com.quietgrid.engine.links.linksEndpointsOf
import java.security.MessageDigest
import kotlin.random.Random

private const val LINKS_MAX_PATH_SHARE = 0.3

fun linksPuzzleKey(size: Int, endpoints: List<List<Int>>): String =
    "$size:" + endpoints.map { it.sorted() }.sortedBy { it.first() }.joinToString(";") { "${it[0]},${it[1]}" }

fun linksPuzzleId(size: Int, endpoints: List<List<Int>>): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(linksPuzzleKey(size, endpoints).toByteArray())
    return "lk$size-" + digest.take(6).joinToString("") { "%02x".format(it) }
}

fun generateLinksPuzzle(difficulty: Difficulty, random: Random = Random.Default, maxAttempts: Int = 400): LinksPuzzleEntry? {
    val size = LINKS_SIZE_BY_DIFFICULTY.getValue(difficulty)
    val pairRange = LINKS_PAIR_RANGE_BY_DIFFICULTY.getValue(difficulty)
    repeat(maxAttempts) {
        val pairCount = pairRange.random(random)
        val solution = generateLinksSolution(size, pairCount, random) ?: return@repeat
        if (solution.maxOf { it.size } > size * size * LINKS_MAX_PATH_SHARE) return@repeat
        val endpoints = linksEndpointsOf(solution)
        val count = countLinksSolutions(size, endpoints)
        if (!count.exhausted || count.solutions != 1) return@repeat
        return LinksPuzzleEntry(
            id = linksPuzzleId(size, endpoints),
            size = size,
            difficulty = difficulty.key,
            endpoints = endpoints,
            solution = solution,
        )
    }
    return null
}
