package com.quietgrid.cli.themes

import com.quietgrid.engine.themeclear.ThemeClearTheme
import com.quietgrid.engine.themeclear.parseThemeClearThemes
import kotlinx.serialization.json.JsonPrimitive
import java.io.File

const val SHARED_THEMES_ASSET = "app/src/main/assets/themes.json"

private val defaultThemes: Map<String, List<ThemeClearTheme>> by lazy { parseThemeClearThemes(File(SHARED_THEMES_ASSET).readText()) }

fun loadSharedThemes(path: String = SHARED_THEMES_ASSET): Map<String, List<ThemeClearTheme>> =
    if (path == SHARED_THEMES_ASSET) defaultThemes else parseThemeClearThemes(File(path).readText())

fun encodeSharedThemes(themes: Map<String, List<ThemeClearTheme>>): String {
    fun quote(value: String) = JsonPrimitive(value).toString()
    val locales = themes.entries.joinToString(",\n") { (locale, list) ->
        val blocks = list.joinToString(",\n") { theme ->
            val words = theme.words.joinToString(", ") { quote(it) }
            "  {\n   \"themeId\": ${quote(theme.themeId)},\n   \"words\": [$words]\n  }"
        }
        " ${quote(locale)}: [\n$blocks\n ]"
    }
    return "{\n$locales\n}\n"
}
