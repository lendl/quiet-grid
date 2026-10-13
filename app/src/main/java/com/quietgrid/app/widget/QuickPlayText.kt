package com.quietgrid.app.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.quietgrid.app.ui.theme.PlusJakartaSans

data class QuickPlayLabel(val bitmap: Bitmap, val width: Dp, val height: Dp)

private const val MIN_FIT_SP = 10f
private const val FIT_STEP_SP = 1f
private const val LINE_HEIGHT_RATIO = 1.22f

class QuickPlayTextRenderer(context: Context) {
    private val density = Density(context)
    private val measurer = TextMeasurer(createFontFamilyResolver(context), density, LayoutDirection.Ltr)

    fun render(
        text: String,
        fontSize: TextUnit,
        fontWeight: FontWeight,
        maxWidth: Dp,
        maxLines: Int = 1,
        textAlign: TextAlign = TextAlign.Center,
        alpha: Float = 1f,
    ): QuickPlayLabel {
        val maxWidthPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val layout = fit(text, fontSize.value, fontWeight, textAlign, maxWidthPx, maxLines)
        val width = layout.size.width.coerceAtLeast(1)
        val height = layout.size.height.coerceAtLeast(1)
        val image = ImageBitmap(width, height)
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), Size(width.toFloat(), height.toFloat())) {
            drawText(layout, color = Color.White, alpha = alpha)
        }
        return QuickPlayLabel(image.asAndroidBitmap(), with(density) { width.toDp() }, with(density) { height.toDp() })
    }

    private fun fit(text: String, startSp: Float, weight: FontWeight, align: TextAlign, maxWidthPx: Int, maxLines: Int): TextLayoutResult {
        var sizeSp = startSp
        while (true) {
            val style = textStyle(sizeSp, weight, align)
            val layout = measurer.measure(
                text,
                style,
                overflow = TextOverflow.Ellipsis,
                softWrap = true,
                maxLines = maxLines,
                constraints = Constraints(maxWidth = maxWidthPx),
            )
            if (sizeSp <= MIN_FIT_SP || (!layout.hasVisualOverflow && longestWordFits(text, style, maxWidthPx))) return layout
            sizeSp = (sizeSp - FIT_STEP_SP).coerceAtLeast(MIN_FIT_SP)
        }
    }

    private fun longestWordFits(text: String, style: TextStyle, maxWidthPx: Int): Boolean =
        text.split(' ').filter { it.isNotEmpty() }.all { word ->
            measurer.measure(word, style, softWrap = false, maxLines = 1).size.width <= maxWidthPx
        }

    private fun textStyle(sizeSp: Float, weight: FontWeight, align: TextAlign) = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = weight,
        fontSize = sizeSp.sp,
        lineHeight = (sizeSp * LINE_HEIGHT_RATIO).sp,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
        textAlign = align,
    )
}
