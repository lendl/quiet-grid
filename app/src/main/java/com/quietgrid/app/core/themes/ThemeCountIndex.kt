package com.quietgrid.app.core.themes

import com.quietgrid.app.platform.BundledFiles
import com.quietgrid.app.platform.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object ThemeCountIndex {
    private var index: Map<String, Map<String, Map<String, Map<String, Int>>>>? = null

    suspend fun forGame(files: BundledFiles, game: String): Map<String, Map<String, Map<String, Int>>> {
        val loaded = index ?: withContext(Dispatchers.IO) {
            val text = files.readText("theme_counts.json")
            Json.decodeFromString<Map<String, Map<String, Map<String, Map<String, Int>>>>>(text).also { index = it }
        }
        return loaded[game].orEmpty()
    }
}
