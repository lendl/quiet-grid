package com.quietgrid.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class SharedStringsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `apostrophes reach the Android strings`() {
        assertEquals("Today's puzzles", context.getString(R.string.daily_title))
    }

    @Test
    @Config(qualifiers = "fr")
    fun `apostrophes reach the translated Android strings`() {
        assertEquals("Garder l'écran allumé", context.getString(R.string.settings_keep_screen_on_label))
    }

    @Test
    fun `double quotes reach the Android strings`() {
        assertEquals("Find \"CAT\"", context.getString(R.string.wordsearch_next_move_title, "CAT"))
    }
}
