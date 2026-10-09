package com.quietgrid.app.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.CORNER_FRACTION
import com.quietgrid.app.ui.components.drawGameGlyph
import com.quietgrid.app.ui.components.gameIconTile
import com.quietgrid.app.ui.components.glyphPathsFor
import com.quietgrid.app.ui.theme.ResolvedTheme
import com.quietgrid.app.ui.theme.colorSchemeFor

fun renderGameIconBitmap(context: Context, gameId: GameId, sizePx: Int, isPencilTheme: Boolean): Bitmap {
    val density = Density(context)
    val tile = gameIconTile(gameId, isPencilTheme) ?: colorSchemeFor(ResolvedTheme.LIGHT).primary
    val textMeasurer = TextMeasurer(createFontFamilyResolver(context), density, LayoutDirection.Ltr)
    val image = ImageBitmap(sizePx, sizePx)
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), Size(sizePx.toFloat(), sizePx.toFloat())) {
        drawRoundRect(tile, cornerRadius = CornerRadius(sizePx * CORNER_FRACTION))
        drawGameGlyph(gameId, tile, isPencilTheme, textMeasurer, glyphPathsFor(gameId))
    }
    return image.asAndroidBitmap()
}
