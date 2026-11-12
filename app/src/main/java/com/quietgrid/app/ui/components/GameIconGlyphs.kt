package com.quietgrid.app.ui.components


enum class GlyphPaint { ACCENT, NEUTRAL, EMPTY, SMALL, MARK, CUTOUT, LINE, LINE_ACCENT }

sealed interface GlyphShape {
    val paint: GlyphPaint
}

data class GlyphRect(val x: Float, val y: Float, val width: Float, val height: Float, val radius: Float, override val paint: GlyphPaint) : GlyphShape

data class GlyphOval(val cx: Float, val cy: Float, val rx: Float, val ry: Float, override val paint: GlyphPaint) : GlyphShape

data class GlyphPath(val pathData: String, override val paint: GlyphPaint, val strokeWidth: Float? = null) : GlyphShape

data class GlyphText(
    val x: Float,
    val y: Float,
    val text: String,
    val size: Float,
    override val paint: GlyphPaint,
    val letterSpacing: Float = 0f,
) : GlyphShape
