package com.quietgrid.app.core.themes

import com.quietgrid.app.core.Difficulty

const val THEME_POOL_WARNING_THRESHOLD = 30

data class ThemeCount(val themeId: String, val perTier: Map<Difficulty, Int>) {
    val total: Int get() = perTier.values.sum()
}

data class SparseTier(val difficulty: Difficulty, val count: Int)

fun <T> filterPoolByThemes(pool: List<T>, excluded: Set<String>, themeOf: (T) -> String): List<T> {
    if (excluded.isEmpty()) return pool
    return pool.filter { themeOf(it) !in excluded }.ifEmpty { pool }
}

fun sparseTiers(
    counts: List<ThemeCount>,
    excluded: Set<String>,
    threshold: Int = THEME_POOL_WARNING_THRESHOLD,
): List<SparseTier> {
    val effective = excluded intersect counts.map { it.themeId }.toSet()
    if (effective.isEmpty()) return emptyList()
    val selected = counts.filter { it.themeId !in effective }
    return Difficulty.entries
        .map { tier -> SparseTier(tier, selected.sumOf { it.perTier[tier] ?: 0 }) }
        .filter { it.count < threshold }
}

fun toggleThemeExclusion(excluded: Set<String>, themeId: String, allThemeIds: Set<String>): Set<String> {
    val effective = excluded intersect allThemeIds
    if (themeId in effective) return effective - themeId
    if (allThemeIds.size - effective.size <= 1) return effective
    return effective + themeId
}

fun themeCountsForLocale(
    byTierAndLocale: Map<String, Map<String, Map<String, Int>>>,
    locale: String,
    fallbackToAllLocales: Boolean,
): List<ThemeCount> {
    val perTier = Difficulty.entries.associateWith { tier ->
        val byLocale = byTierAndLocale[tier.key].orEmpty()
        byLocale[locale] ?: byLocale["en"] ?: if (fallbackToAllLocales) {
            byLocale.values.flatMap { it.entries }.groupBy({ it.key }, { it.value }).mapValues { it.value.sum() }
        } else {
            emptyMap()
        }
    }
    val themeIds = perTier.values.flatMap { it.keys }.distinct()
    return themeIds.map { themeId ->
        ThemeCount(themeId, Difficulty.entries.associateWith { perTier.getValue(it)[themeId] ?: 0 })
    }
}
