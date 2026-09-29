package com.quietgrid.cli.themes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedThemesAssetTest {
    private val expectedLocales = setOf("en", "nl", "de", "es", "fr", "it", "pl", "pt")
    private val expectedThemeIds = setOf(
        "animals", "food", "nature", "weather", "sports", "clothing", "transport", "home", "professions",
        "emotions", "space", "art", "bodyparts", "school", "music", "technology", "geography", "fantasy",
    )
    private val themes = loadSharedThemes()

    @Test
    fun `asset has exactly the expected locales`() {
        assertEquals(expectedLocales, themes.keys)
    }

    @Test
    fun `every locale has exactly the expected themes`() {
        themes.forEach { (locale, list) ->
            assertEquals("$locale themes", expectedThemeIds, list.map { it.themeId }.toSet())
            assertEquals("$locale has duplicate theme ids", list.size, list.map { it.themeId }.toSet().size)
        }
    }

    @Test
    fun `every word is a single title case word of 3 to 14 letters`() {
        val pattern = Regex("[A-Z][a-z]{2,13}")
        themes.forEach { (locale, list) ->
            list.forEach { theme ->
                theme.words.forEach { word ->
                    assertTrue("$locale/${theme.themeId}: '$word'", pattern.matches(word))
                }
            }
        }
    }

    @Test
    fun `no theme lists a word twice`() {
        themes.forEach { (locale, list) ->
            list.forEach { theme ->
                val normalized = theme.words.map { it.uppercase() }
                assertEquals("$locale/${theme.themeId} has duplicates", normalized.size, normalized.toSet().size)
            }
        }
    }

    @Test
    fun `every theme has at least 80 words`() {
        themes.forEach { (locale, list) ->
            list.forEach { theme ->
                assertTrue("$locale/${theme.themeId} has only ${theme.words.size}", theme.words.size >= 80)
            }
        }
    }

    @Test
    fun `encoder round-trips the asset`() {
        assertEquals(themes, com.quietgrid.engine.themeclear.parseThemeClearThemes(encodeSharedThemes(themes)))
    }
}
