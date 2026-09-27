package com.quietgrid.engine.themeclear

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeClearSelectionTest {
    private val dictionary = ThemeClearDictionary(listOf("LEMON", "MELON", "APPLE", "APPLEPIE"))

    @Test
    fun `empty selection is EMPTY`() {
        assertEquals(ThemeClearSelectionState.EMPTY, evaluateThemeClearSelection("", "LEMON", dictionary))
    }

    @Test
    fun `selection that starts no theme word is NO_MATCH`() {
        assertEquals(ThemeClearSelectionState.NO_MATCH, evaluateThemeClearSelection("LX", "", dictionary))
    }

    @Test
    fun `prefix that is not yet a word is BUILDING`() {
        assertEquals(ThemeClearSelectionState.BUILDING, evaluateThemeClearSelection("LEM", "ON", dictionary))
    }

    @Test
    fun `complete word with no possible extension is ACCEPT`() {
        assertEquals(ThemeClearSelectionState.ACCEPT, evaluateThemeClearSelection("LEMON", "", dictionary))
    }

    @Test
    fun `complete word that could still become a longer word is PENDING`() {
        assertEquals(ThemeClearSelectionState.PENDING, evaluateThemeClearSelection("APPLE", "PIE", dictionary))
    }

    @Test
    fun `complete word whose extension letters are gone is ACCEPT`() {
        assertEquals(ThemeClearSelectionState.ACCEPT, evaluateThemeClearSelection("APPLE", "XYZ", dictionary))
    }
}
