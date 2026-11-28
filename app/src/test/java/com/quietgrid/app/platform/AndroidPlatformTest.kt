package com.quietgrid.app.platform

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AndroidPlatformTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `bundled files read a packaged bank`() = runBlocking {
        assertTrue(AndroidBundledFiles(context).readText("sudoku_puzzles.json").trimStart().startsWith("["))
    }

    @Test
    fun `app language round trips a tag`() {
        val language = AndroidAppLanguage(context)
        language.setLanguage("fr")
        assertEquals("fr", language.currentTag())
        language.setLanguage("")
        assertEquals("", language.currentTag())
    }

    @Test
    fun `system language tag is never empty`() {
        assertTrue(AndroidAppLanguage(context).systemLanguageTag().isNotEmpty())
    }

    @Test
    fun `animations are off when the animator scale is zero`() {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        assertFalse(AndroidMotionSettings(context).animationsEnabled())
    }

    @Test
    fun `app info reports a version name`() {
        assertTrue(AndroidAppInfo(context).versionName.isNotEmpty())
    }
}
