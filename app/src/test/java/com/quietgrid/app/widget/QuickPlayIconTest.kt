package com.quietgrid.app.widget

import android.content.Context
import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.GAME_ICON_HUES
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuickPlayIconTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `icon bitmap has requested size, transparent corner and tile colour at top edge`() {
        val bitmap = renderGameIconBitmap(context, GameId.SUDOKU, 96, isPencilTheme = false)
        assertEquals(96, bitmap.width)
        assertEquals(96, bitmap.height)
        assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
        assertEquals(GAME_ICON_HUES.getValue(GameId.SUDOKU).toArgb(), bitmap.getPixel(48, 2))
    }

    @Test
    fun `pencil icon bitmap uses the grey tile`() {
        val bitmap = renderGameIconBitmap(context, GameId.SUDOKU, 96, isPencilTheme = true)
        assertEquals(0xFFE0E0E0.toInt(), bitmap.getPixel(48, 2))
    }
}
