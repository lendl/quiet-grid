package com.quietgrid.app.ui.seasonal

import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import com.quietgrid.app.R

const val HALLOWEEN_UNITS = 120f

enum class HalloweenCharacter(
    @StringRes val lineRes: Int,
    @StringRes val descriptionRes: Int,
    val glow: Color,
) {
    PUMPKIN(R.string.halloween_line_pumpkin, R.string.halloween_character_pumpkin, Color(0xFFEE9A4D)),
    CAT(R.string.halloween_line_cat, R.string.halloween_character_cat, Color(0xFFF2DFA6)),
    OWL(R.string.halloween_line_owl, R.string.halloween_character_owl, Color(0xFFEE9A4D)),
    GHOST(R.string.halloween_line_ghost, R.string.halloween_character_ghost, Color(0xFFC9B8E8)),
    BAT(R.string.halloween_line_bat, R.string.halloween_character_bat, Color(0xFFEE9A4D)),
    MOON(R.string.halloween_line_moon, R.string.halloween_character_moon, Color(0xFFF2DFA6)),
}

data class HalloweenEyes(
    val open: Float = 1f,
    val slit: Float = 1f,
    val look: Float = 0f,
)

private val pathCache = HashMap<String, Path>()

private fun svg(d: String): Path = pathCache.getOrPut(d) { PathParser().parsePathString(d).toPath() }

private val GLOW_YELLOW = Color(0xFFFFD27A)
private val NIGHT = Color(0xFF0B080E)
private val GHOST_INK = Color(0xFF2A2131)
private val MOON_INK = Color(0xFF5A4A3A)

fun DrawScope.drawHalloweenBody(character: HalloweenCharacter, faceLight: Float = 1f) {
    when (character) {
        HalloweenCharacter.PUMPKIN -> drawPumpkin(faceLight)
        HalloweenCharacter.CAT -> drawCat()
        HalloweenCharacter.OWL -> drawOwl()
        HalloweenCharacter.GHOST -> drawGhost()
        HalloweenCharacter.BAT -> drawBat()
        HalloweenCharacter.MOON -> drawMoon()
    }
}

fun DrawScope.drawHalloweenEyes(character: HalloweenCharacter, eyes: HalloweenEyes) {
    when (character) {
        HalloweenCharacter.PUMPKIN -> {
            blink(45f, 52f, eyes.open) { drawPath(svg("M34 58l12-12 10 12z"), GLOW_YELLOW) }
            blink(75f, 52f, eyes.open) { drawPath(svg("M64 58l10-12 12 12z"), GLOW_YELLOW) }
        }
        HalloweenCharacter.CAT -> {
            for (x in listOf(45f, 75f)) {
                blink(x, 64f, eyes.open) {
                    drawOval(Color(0xFFD9E46B), Offset(x - 9f, 64f - 6.5f), Size(18f, 13f))
                    val pupilWidth = 3.2f * (2.4f - 1.4f * eyes.slit)
                    drawOval(NIGHT, Offset(x - pupilWidth / 2f, 64f - 5.5f), Size(pupilWidth, 11f))
                }
            }
        }
        HalloweenCharacter.OWL -> {
            for (x in listOf(44f, 76f)) {
                blink(x, 52f, eyes.open) {
                    drawCircle(Color(0xFFF2B24A), 12f, Offset(x, 52f))
                    val px = x + 3f * eyes.look
                    drawCircle(NIGHT, 6f, Offset(px, 52f))
                    drawCircle(Color(0xFFFFF3D6), 1.6f, Offset(px + 2f, 50f))
                }
            }
        }
        HalloweenCharacter.GHOST -> {
            for (x in listOf(46f, 74f)) {
                blink(x, 50f, eyes.open) { drawOval(GHOST_INK, Offset(x - 6f, 50f - 8.5f), Size(12f, 17f)) }
            }
        }
        HalloweenCharacter.BAT -> {
            for (x in listOf(54f, 66f)) {
                blink(x, 56f, eyes.open) { drawCircle(Color(0xFFFFB561), 3.6f, Offset(x, 56f)) }
            }
        }
        HalloweenCharacter.MOON -> {
            for (x in listOf(46f, 74f)) {
                blink(x, 56f, eyes.open) { drawOval(MOON_INK, Offset(x - 5f, 56f - 4.5f), Size(10f, 9f)) }
            }
        }
    }
}

private inline fun DrawScope.blink(x: Float, y: Float, open: Float, block: DrawScope.() -> Unit) {
    if (open <= 0f) return
    scale(1f, open, pivot = Offset(x, y)) { block() }
}

private fun DrawScope.drawPumpkin(faceLight: Float) {
    drawPath(svg("M60 22c-2-8 2-14 8-16"), Color(0xFF6E8B4E), style = Stroke(width = 5f, cap = StrokeCap.Round))
    drawOval(Color(0xFFC9692A), Offset(12f, 28f), Size(52f, 80f))
    drawOval(Color(0xFFC9692A), Offset(56f, 28f), Size(52f, 80f))
    drawOval(Color(0xFFDD7A35), Offset(32f, 26f), Size(56f, 84f))
    drawLine(Color(0xFFB85E25).copy(alpha = 0.6f), Offset(60f, 28f), Offset(60f, 108f), strokeWidth = 2f)
    if (faceLight > 0f) {
        val face = GLOW_YELLOW.copy(alpha = faceLight)
        drawPath(svg("M56 70l4-7 4 7z"), face)
        drawPath(svg("M30 80c8 14 52 14 60 0l-7 3-5-5-6 6-6-6-6 6-6-6-5 5z"), face)
    }
}

private fun DrawScope.drawCat() {
    drawCircle(Color(0xFFF2DFA6), 54f, Offset(60f, 58f))
    drawPath(svg("M22 106 C18 80 24 58 30 48 L26 18 L46 38 C55 35 65 35 74 38 L94 18 L90 48 C96 58 102 80 98 106 Z"), NIGHT)
    drawPath(
        svg("M30 76 L12 72 M30 80 L12 82 M90 76 L108 72 M90 80 L108 82"),
        NIGHT,
        style = Stroke(width = 1.6f, cap = StrokeCap.Round),
    )
    drawPath(svg("M57 75 L63 75 L60 79 Z"), Color(0xFF5A4652))
}

private fun DrawScope.drawOwl() {
    val feathers = Color(0xFF6B4A3A)
    val face = Color(0xFF8A6650)
    drawPath(svg("M28 30 L24 12 L42 26 Z"), feathers)
    drawPath(svg("M92 30 L96 12 L78 26 Z"), feathers)
    drawOval(feathers, Offset(24f, 22f), Size(72f, 88f))
    drawOval(face, Offset(38f, 58f), Size(44f, 48f))
    drawPath(
        svg("M50 78 q4 4 8 0 M62 78 q4 4 8 0 M54 88 q4 4 8 0 M56 98 q4 4 8 0"),
        feathers,
        style = Stroke(width = 1.6f, cap = StrokeCap.Round),
    )
    drawCircle(face, 17f, Offset(44f, 52f))
    drawCircle(face, 17f, Offset(76f, 52f))
    drawPath(svg("M56 62 L64 62 L60 70 Z"), Color(0xFFEE9A4D))
    drawLine(Color(0xFF4A3428), Offset(8f, 110f), Offset(112f, 106f), strokeWidth = 7f, cap = StrokeCap.Round)
    drawPath(
        svg("M46 108 l-3 5 M52 108 l0 6 M68 107 l0 6 M74 107 l3 5"),
        Color(0xFFEE9A4D),
        style = Stroke(width = 2.2f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawGhost() {
    val body = svg("M60 8C34 8 20 28 20 52v58l13-10 13 10 14-10 14 10 13-10 13 10V52C100 28 86 8 60 8z")
    drawPath(body, Color(0xFFEDE4EF).copy(alpha = 0.92f))
    drawPath(body, Color(0xFF8E7A9C), style = Stroke(width = 2.5f))
    drawOval(GHOST_INK, Offset(55f, 64f), Size(10f, 12f))
    drawOval(Color(0xFFE7A6B5).copy(alpha = 0.6f), Offset(31f, 61f), Size(10f, 6f))
    drawOval(Color(0xFFE7A6B5).copy(alpha = 0.6f), Offset(79f, 61f), Size(10f, 6f))
}

private fun DrawScope.drawBat() {
    val rim = Color(0xFF76627A)
    val parts = listOf(
        svg("M52 60 C42 44 22 42 2 50 C10 54 12 60 12 66 C18 62 24 63 28 70 C32 65 38 65 42 72 C45 67 49 66 52 68 Z"),
        svg("M68 60 C78 44 98 42 118 50 C110 54 108 60 108 66 C102 62 96 63 92 70 C88 65 82 65 78 72 C75 67 71 66 68 68 Z"),
        svg("M48 44 L46 30 L54 40 Z M72 44 L74 30 L66 40 Z"),
    )
    for (part in parts) {
        drawPath(part, NIGHT)
        drawPath(part, rim, style = Stroke(width = 1.2f))
    }
    drawOval(NIGHT, Offset(46f, 42f), Size(28f, 40f))
    drawOval(rim, Offset(46f, 42f), Size(28f, 40f), style = Stroke(width = 1.2f))
    drawPath(svg("M56 66 L57.5 70 L59 66 Z M61 66 L62.5 70 L64 66 Z"), Color(0xFFF4ECE2))
}

private fun DrawScope.drawMoon() {
    drawCircle(Color(0xFFF2DFA6), 52f, Offset(60f, 60f))
    drawCircle(Color(0xFFB8934A), 52f, Offset(60f, 60f), style = Stroke(width = 2.5f))
    drawCircle(Color(0xFFE4CC8E), 7f, Offset(88f, 38f))
    drawCircle(Color(0xFFE4CC8E), 5f, Offset(30f, 84f))
    drawCircle(Color(0xFFE4CC8E), 4f, Offset(84f, 88f))
    drawPath(svg("M38 46 q8 -5 16 0 M66 46 q8 -5 16 0"), Color(0xFFB89A62), style = Stroke(width = 2f, cap = StrokeCap.Round))
    drawOval(Color(0xFFEBA88C).copy(alpha = 0.6f), Offset(32f, 64.5f), Size(12f, 7f))
    drawOval(Color(0xFFEBA88C).copy(alpha = 0.6f), Offset(76f, 64.5f), Size(12f, 7f))
    drawPath(svg("M52 74 q8 7 16 0"), MOON_INK, style = Stroke(width = 2.4f, cap = StrokeCap.Round))
}
