package com.quietgrid.cli.themeclear

import com.quietgrid.cli.themes.SHARED_THEMES_ASSET
import com.quietgrid.cli.themes.loadSharedThemes
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.themeclear.THEMECLEAR_MAX_COLS
import com.quietgrid.engine.themeclear.THEMECLEAR_MIN_WORD_LENGTH
import com.quietgrid.engine.themeclear.THEMECLEAR_TIERS
import com.quietgrid.engine.themeclear.ThemeClearDictionary
import com.quietgrid.engine.themeclear.ThemeClearMetrics
import com.quietgrid.engine.themeclear.ThemeClearPuzzleEntry
import com.quietgrid.engine.themeclear.ThemeClearSolver
import com.quietgrid.engine.themeclear.ThemeClearTheme
import com.quietgrid.engine.themeclear.ThemeClearTierSpec
import com.quietgrid.engine.themeclear.themeClearMeetsTier
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

data class ThemeClearCandidate(
    val themeId: String,
    val words: List<String>,
    val rows: Int,
    val cols: Int,
    val metrics: ThemeClearMetrics,
) {
    val letterCount: Int get() = rows * cols
}

fun ThemeClearCandidate.toEntry(difficulty: Difficulty, locale: String, random: Random): ThemeClearPuzzleEntry {
    val scattered = words.joinToString("").toList().shuffled(random).joinToString("")
    return ThemeClearPuzzleEntry(
        id = "tc-${difficulty.key}-$themeId-${scattered.lowercase()}",
        difficulty = difficulty.key,
        themeId = themeId,
        rows = rows,
        cols = cols,
        grid = scattered.chunked(cols),
        words = words,
        metrics = metrics,
        locale = locale,
    )
}

class ThemeClearGenerator(themes: List<ThemeClearTheme>, private val random: Random = Random.Default) {
    private class ThemePool(val themeId: String, val solver: ThemeClearSolver, val words: List<String>)

    private val pools: List<ThemePool> = themes.map { theme ->
        val dictionary = ThemeClearDictionary(theme.words)
        ThemePool(theme.themeId, ThemeClearSolver(dictionary), dictionary.words.sorted())
    }

    fun drawCandidate(spec: ThemeClearTierSpec): ThemeClearCandidate? {
        val pool = pools.random(random)
        val words = drawThemeClearWords(pool.words, spec, random) ?: return null
        val letters = words.joinToString("")
        val (rows, cols) = themeClearRectangles(letters.length).randomOrNull(random) ?: return null
        return ThemeClearCandidate(pool.themeId, words, rows, cols, pool.solver.analyze(letters))
    }

    fun generate(difficulty: Difficulty, locale: String, maxAttempts: Int = 400): ThemeClearPuzzleEntry? {
        val spec = THEMECLEAR_TIERS.getValue(difficulty)
        repeat(maxAttempts) {
            val candidate = drawCandidate(spec) ?: return@repeat
            if (themeClearMeetsTier(difficulty, candidate.letterCount, candidate.metrics)) {
                return candidate.toEntry(difficulty, locale, random)
            }
        }
        return null
    }
}
