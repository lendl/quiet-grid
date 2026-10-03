package com.quietgrid.app.core.themes

import android.content.Context
import com.quietgrid.engine.themeclear.parseThemeClearThemes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThemeWordIndex {
    private var words: Map<String, Map<String, List<String>>>? = null

    suspend fun all(context: Context): Map<String, Map<String, List<String>>> {
        words?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("themes.json").bufferedReader().use { it.readText() }
            parseThemeClearThemes(text)
                .mapValues { (_, themes) -> themes.associate { it.themeId to it.words } }
                .also { words = it }
        }
    }

    suspend fun wordsFor(context: Context, locale: String, themeId: String): List<String> {
        val byLocale = all(context)
        return (byLocale[locale] ?: byLocale["en"])?.get(themeId).orEmpty()
    }
}
