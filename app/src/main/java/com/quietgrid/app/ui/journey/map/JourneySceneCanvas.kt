package com.quietgrid.app.ui.journey.map

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.rememberTextMeasurer
import com.quietgrid.app.ui.components.drawGameGlyph
import com.quietgrid.app.ui.components.glyphPathsFor

private data class SceneStyle(val fill: ScenePaint?, val stroke: ScenePaint?, val strokeWidth: Float, val dash: List<Float>?, val alpha: Float)

private class SceneLayer(val style: SceneStyle, val path: Path)

private const val GLYPH_ALPHA = 0.3f

@Composable
fun JourneySceneCanvas(kind: JourneySceneKind, layout: JourneyRoadLayout, doneCount: Int, seed: Int, modifier: Modifier = Modifier) {
    val palette = scenePalette(kind, MaterialTheme.colorScheme)
    val glyphInk = MaterialTheme.colorScheme.onBackground.copy(alpha = GLYPH_ALPHA)
    val layers = remember(kind, layout, seed) { sceneLayers(journeyScene(kind, layout, seed)) }
    val glyphs = remember(layout, seed) { journeyGlyphs(layout, seed) }
    val glyphPaths = remember(glyphs) { glyphs.map { it.gameId }.distinct().associateWith { glyphPathsFor(it) } }
    val roadStyle = remember(kind) { journeyRoadStyle(kind) }
    val fullRoad = remember(layout) { PathParser().parsePathString(smoothRoadPath(layout.full)).toPath() }
    val doneRoad = remember(layout, doneCount) {
        if (doneCount > 0) PathParser().parsePathString(smoothRoadPath(layout.full, segments = doneCount)).toPath() else null
    }
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier.graphicsLayer()) {
        scale(density, pivot = Offset.Zero) {
            layers.forEach { layer -> drawSceneLayer(layer, palette) }
        }
        glyphs.forEach { glyph ->
            val half = glyph.size / 2 * density
            val left = glyph.x * density - half
            val top = glyph.y * density - half
            inset(left, top, size.width - left - half * 2, size.height - top - half * 2) {
                drawGameGlyph(glyph.gameId, Color.Transparent, false, textMeasurer, glyphPaths.getValue(glyph.gameId), flatColor = glyphInk)
            }
        }
        scale(density, pivot = Offset.Zero) {
            roadStyle.forEach { stroke ->
                val path = if (stroke.done) doneRoad else fullRoad
                if (path != null) {
                    translate(stroke.offsetX, stroke.offsetY) {
                        drawPath(
                            path,
                            palette.colorFor(stroke.paint).let { it.copy(alpha = it.alpha * stroke.alpha) },
                            style = Stroke(
                                width = stroke.width,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                                pathEffect = stroke.dash?.let { PathEffect.dashPathEffect(it.toFloatArray()) },
                            ),
                        )
                    }
                }
            }
        }
    }
}

private fun sceneLayers(items: List<SceneItem>): List<SceneLayer> =
    items.groupBy { SceneStyle(it.fill, it.stroke, it.strokeWidth, it.dash, it.alpha) }
        .map { (style, group) -> SceneLayer(style, PathParser().parsePathString(group.joinToString(" ") { it.pathData }).toPath()) }

private fun DrawScope.drawSceneLayer(layer: SceneLayer, palette: ScenePalette) {
    val style = layer.style
    style.fill?.let { paint ->
        val colour = palette.colorFor(paint)
        drawPath(layer.path, colour.copy(alpha = colour.alpha * style.alpha))
    }
    style.stroke?.let { paint ->
        val colour = palette.colorFor(paint)
        drawPath(
            layer.path,
            colour.copy(alpha = colour.alpha * style.alpha),
            style = Stroke(
                width = style.strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = style.dash?.let { PathEffect.dashPathEffect(it.toFloatArray()) },
            ),
        )
    }
}
