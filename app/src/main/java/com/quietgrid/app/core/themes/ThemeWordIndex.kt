package com.quietgrid.app.core.themes

import com.quietgrid.app.platform.BundledFiles
import com.quietgrid.app.platform.readText
import com.quietgrid.engine.themeclear.parseThemeClearThemes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThemeWordIndex {
    private var words: Map<String, Map<String, List<String>>>? = null

    suspend fun all(files: BundledFiles): Map<String, Map<String, List<String>>> {
        words?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = files.readText("themes.json")
            parseThemeClearThemes(text)
                .mapValues { (_, themes) -> themes.associate { it.themeId to it.words } }
                .also { words = it }
        }
    }

    suspend fun wordsFor(files: BundledFiles, locale: String, themeId: String): List<String> {
        val byLocale = all(files)
        return (byLocale[locale] ?: byLocale["en"])?.get(themeId).orEmpty()
    }
}
