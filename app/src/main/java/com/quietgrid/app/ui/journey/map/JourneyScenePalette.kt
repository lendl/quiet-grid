package com.quietgrid.app.ui.journey.map

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

private val NORD_AURORA = listOf(Color(0xFF88C0D0), Color(0xFFA3BE8C), Color(0xFFB48EAD))

data class ScenePalette(private val colors: Map<ScenePaint, Color>) {
    fun colorFor(paint: ScenePaint): Color = colors.getValue(paint)
}

fun scenePalette(kind: JourneySceneKind, colors: ColorScheme): ScenePalette {
    val ink = colors.onBackground.copy(alpha = 0.12f)
    fun aurora(index: Int) = if (kind == JourneySceneKind.ARCTIC) NORD_AURORA[index] else ink
    return ScenePalette(
        ScenePaint.entries.associateWith { paint ->
            when (paint) {
                ScenePaint.INK -> ink
                ScenePaint.INK_STRONG -> colors.onBackground.copy(alpha = 0.24f)
                ScenePaint.FILL_SOFT -> colors.surfaceVariant.copy(alpha = 0.4f)
                ScenePaint.FILL_ACCENT -> colors.primary.copy(alpha = 0.12f)
                ScenePaint.GLOW -> colors.primary.copy(alpha = 0.5f)
                ScenePaint.SURFACE -> colors.surface
                ScenePaint.BACKDROP -> colors.background
                ScenePaint.AURORA_1 -> aurora(0)
                ScenePaint.AURORA_2 -> aurora(1)
                ScenePaint.AURORA_3 -> aurora(2)
                ScenePaint.ROAD -> colors.outlineVariant
                ScenePaint.ROAD_FILL -> colors.surfaceContainer
                ScenePaint.ROAD_DONE -> colors.primary
            }
        },
    )
}
