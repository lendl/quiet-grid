package com.quietgrid.app.widget

import android.content.Context
import android.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuickPlayTextTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val density = context.resources.displayMetrics.density

    @Test
    fun `label bitmap matches its reported size and stays inside the width`() {
        val label = QuickPlayTextRenderer(context).render("Sudoku", 16.sp, FontWeight.SemiBold, 120.dp)
        assertEquals(label.bitmap.width.toFloat(), label.width.value * density, 1f)
        assertEquals(label.bitmap.height.toFloat(), label.height.value * density, 1f)
        assertTrue(label.width.value <= 120f)
    }

    @Test
    fun `long word shrinks to fit instead of overflowing`() {
        val renderer = QuickPlayTextRenderer(context)
        val roomy = renderer.render("Minesweeper", 30.sp, FontWeight.SemiBold, 400.dp)
        val tight = renderer.render("Minesweeper", 30.sp, FontWeight.SemiBold, 80.dp, maxLines = 2)
        assertTrue(tight.width.value <= 80f)
        assertTrue(tight.height < roomy.height * 1.5f)
    }

    @Test
    fun `text is drawn in white so the widget can tint it`() {
        val label = QuickPlayTextRenderer(context).render("W", 40.sp, FontWeight.ExtraBold, 200.dp)
        val pixels = IntArray(label.bitmap.width * label.bitmap.height)
        label.bitmap.getPixels(pixels, 0, label.bitmap.width, 0, 0, label.bitmap.width, label.bitmap.height)
        val opaque = pixels.filter { Color.alpha(it) == 255 }
        assertTrue(opaque.isNotEmpty())
        assertTrue(opaque.all { Color.red(it) == 255 && Color.green(it) == 255 && Color.blue(it) == 255 })
    }
}
