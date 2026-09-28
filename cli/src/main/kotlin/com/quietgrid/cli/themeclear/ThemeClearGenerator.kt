package com.quietgrid.cli.themeclear

import com.quietgrid.cli.themes.SHARED_THEMES_ASSET
import com.quietgrid.cli.themes.loadSharedThemes
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.themeclear.THEMECLEAR_MAX_COLS
import com.quietgrid.engine.themeclear.THEMECLEAR_MIN_WORD_LENGTH
import com.quietgrid.engine.themeclear.THEMECLEAR_TIERS
import com.quietgrid.engine.themeclear.ThemeClearDictionary
import com.quietgrid.engine.themeclear.ThemeClearPuzzleEntry
import com.quietgrid.engine.themeclear.ThemeClearSolver
import com.quietgrid.engine.themeclear.ThemeClearTheme
import com.quietgrid.engine.themeclear.ThemeClearTierSpec
import com.quietgrid.engine.themeclear.themeClearMeetsTier
import com.quietgrid.engine.themeclear.themeClearPuzzleId
import kotlin.random.Random

fun loadThemeClearThemes(locale: String, path: String = SHARED_THEMES_ASSET): List<ThemeClearTheme> =
    loadSharedThemes(path)[locale] ?: error("No themes for locale '$locale' in $path")

fun themeClearRectangles(area: Int): List<Pair<Int, Int>> =
    (3..THEMECLEAR_MAX_COLS)
        .filter { cols -> area % cols == 0 }
        .map { cols -> (area / cols) to cols }
        .filter { (rows, cols) -> rows >= 3 && maxOf(rows, cols) <= 2 * minOf(rows, cols) }

fun drawThemeClearWords(pool: List<String>, spec: ThemeClearTierSpec, random: Random): List<String>? {
    val target = spec.letters.random(random)
    val picked = mutableListOf<String>()
    var letters = 0
    for (word in pool.shuffled(random)) {
        if (letters >= target && picked.size >= spec.minWords) break
        val stillNeeded = maxOf(0, spec.minWords - picked.size - 1)
        if (letters + word.length + stillNeeded * THEMECLEAR_MIN_WORD_LENGTH > spec.letters.last) continue
        picked += word
        letters += word.length
    }
    return picked.takeIf { it.size >= spec.minWords && letters in spec.letters }
}

const val THEMECLEAR_MAX_PUZZLES_PER_THEME = 50

fun themeClearThemeDeficits(perTheme: Map<String, Int>, themeIds: List<String>, target: Int): Map<String, Int> {
    val capped = minOf(target, THEMECLEAR_MAX_PUZZLES_PER_THEME)
    return themeIds.associateWith { capped - (perTheme[it] ?: 0) }.filterValues { it > 0 }
}

data class ThemeClearCandidate(
    val themeId: String,
    val words: List<String>,
    val rows: Int,
    val cols: Int,
) {
    val letters: String get() = words.joinToString("")
}

fun ThemeClearCandidate.toEntry(difficulty: Difficulty, locale: String, random: Random): ThemeClearPuzzleEntry {
    val grid = words.joinToString("").toList().shuffled(random).joinToString("").chunked(cols)
    return ThemeClearPuzzleEntry(
        id = themeClearPuzzleId(difficulty.key, themeId, grid),
        difficulty = difficulty.key,
        themeId = themeId,
        rows = rows,
        cols = cols,
        grid = grid,
        words = words,
        locale = locale,
    )
}

class ThemeClearGenerator(themes: List<ThemeClearTheme>, private val random: Random = Random.Default) {
    private class ThemePool(val themeId: String, val solver: ThemeClearSolver, val words: List<String>)

    private val pools: List<ThemePool> = themes.map { theme ->
        val dictionary = ThemeClearDictionary(theme.words)
        ThemePool(theme.themeId, ThemeClearSolver(dictionary), dictionary.words.sorted())
    }

    val themeIds: List<String> get() = pools.map { it.themeId }

    private fun poolFor(themeId: String?): ThemePool =
        if (themeId == null) pools.random(random) else pools.first { it.themeId == themeId }

    fun drawCandidate(spec: ThemeClearTierSpec, themeId: String? = null): ThemeClearCandidate? {
        val pool = poolFor(themeId)
        val words = drawThemeClearWords(pool.words, spec, random) ?: return null
        val (rows, cols) = themeClearRectangles(words.sumOf { it.length }).randomOrNull(random) ?: return null
        return ThemeClearCandidate(pool.themeId, words, rows, cols)
    }

    fun generate(difficulty: Difficulty, locale: String, themeId: String? = null, maxAttempts: Int = 400): ThemeClearPuzzleEntry? {
        val spec = THEMECLEAR_TIERS.getValue(difficulty)
        repeat(maxAttempts) {
            val candidate = drawCandidate(spec, themeId) ?: return@repeat
            if (themeClearMeetsTier(difficulty, candidate.letters, poolFor(candidate.themeId).solver)) {
                return candidate.toEntry(difficulty, locale, random)
            }
        }
        return null
    }
}
