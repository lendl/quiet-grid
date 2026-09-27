package com.quietgrid.app.core.themes

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object ThemeCountIndex {
    private var index: Map<String, Map<String, Map<String, Map<String, Int>>>>? = null

    suspend fun forGame(context: Context, game: String): Map<String, Map<String, Map<String, Int>>> {
        val loaded = index ?: withContext(Dispatchers.IO) {
            val text = context.assets.open("theme_counts.json").bufferedReader().use { it.readText() }
            Json.decodeFromString<Map<String, Map<String, Map<String, Map<String, Int>>>>>(text).also { index = it }
        }
        return loaded[game].orEmpty()
    }
}
