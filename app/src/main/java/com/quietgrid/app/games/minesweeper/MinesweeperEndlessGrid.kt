package com.quietgrid.app.games.minesweeper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.quietgrid.app.ui.components.rememberReduceMotion
import com.quietgrid.app.ui.theme.LocalIsPencilTheme
import com.quietgrid.engine.minesweeper.endless.ENDLESS_SECTION_SIZE
import com.quietgrid.engine.minesweeper.endless.ENDLESS_START_SECTION
import com.quietgrid.engine.minesweeper.endless.EndlessCellState
import com.quietgrid.engine.minesweeper.endless.EndlessWorld
import com.quietgrid.engine.minesweeper.endless.SectionPos
import com.quietgrid.engine.minesweeper.endless.SectionState
import com.quietgrid.engine.minesweeper.endless.WorldCell
import com.quietgrid.engine.minesweeper.endless.section
import kotlin.math.floor

private const val CLEARED_MUTE = 0.35f
private const val CLOSED_DIM = 0.55f
private const val OPEN_FADE_MS = 400
private const val CORNER_FRACTION = 0.2f
private const val NUMBER_SIZE_FRACTION = 0.42f

private val PENCIL_ENDLESS_NUMBER_COLORS = listOf(
    Color(0xFF666666), Color(0xFF595959), Color(0xFF4D4D4D), Color(0xFF404040),
    Color(0xFF333333), Color(0xFF262626), Color(0xFF000000), Color(0xFF0D0D0D),
)

internal class EndlessOpenFades(private val durationMillis: Long = OPEN_FADE_MS.toLong()) {
    private val knownStates = HashMap<SectionPos, SectionState>()
    private val openedAt = HashMap<SectionPos, Long>()

    fun observe(world: EndlessWorld, nowMillis: Long, animate: Boolean) {
        for (section in world.sections.values) {
            val previous = knownStates.put(section.pos, section.state)
            val wasClosed = previous == SectionState.BUILT || previous == SectionState.UNBUILT
            if (animate && wasClosed && section.state == SectionState.OPEN) openedAt[section.pos] = nowMillis
        }
    }

    fun dim(pos: SectionPos, nowMillis: Long): Float {
        val start = openedAt[pos] ?: return 0f
        val progress = (nowMillis - start).toFloat() / durationMillis
        return (1f - progress).coerceIn(0f, 1f)
    }

    fun hasActive(nowMillis: Long): Boolean {
        openedAt.values.removeAll { nowMillis - it >= durationMillis }
        return openedAt.isNotEmpty()
    }
}

internal fun endlessDisplayState(world: EndlessWorld, pos: SectionPos): SectionState =
    if (world.sections.isEmpty() && pos == ENDLESS_START_SECTION) SectionState.OPEN else world.stateOf(pos)

@Composable
fun MinesweeperEndlessGrid(
    world: EndlessWorld,
    camera: MinesweeperEndlessCamera,
    revealMines: Boolean,
    onCameraChanged: (MinesweeperEndlessCamera) -> Unit,
    onReveal: (WorldCell) -> Unit,
    onToggleFlag: (WorldCell) -> Unit,
    modifier: Modifier = Modifier,
    halloween: Boolean = false,
) {
    val halloweenColors = minesweeperHalloweenColors().takeIf { halloween }
    val colors = MaterialTheme.colorScheme
    val isPencil = LocalIsPencilTheme.current
    val palette = minesweeperPaletteFor(colors.surface)
    val hiddenBase = if (isPencil) colors.surfaceVariant else palette.hidden
    val hiddenAlt = if (isPencil) lerp(hiddenBase, colors.onSurfaceVariant, 0.08f) else lerp(hiddenBase, colors.primary, 0.08f)
    val openedColor = if (isPencil) colors.surface else palette.opened
    val numberColors = if (isPencil) PENCIL_ENDLESS_NUMBER_COLORS else palette.numbers
    val surface = colors.surface
    val knownMineColor = colors.errorContainer
    val mineBody = colors.onErrorContainer
    val mineFuse = colors.onSurfaceVariant
    val flagPole = colors.onSurface
    val flagBanner = colors.primary

    val textMeasurer = rememberTextMeasurer()
    val gapPx = with(LocalDensity.current) { 2.dp.toPx() }
    val glyphInsetPx = with(LocalDensity.current) { 3.dp.toPx() }
    val reduceMotion = rememberReduceMotion()

    var liveCamera by remember { mutableStateOf(camera) }
    LaunchedEffect(camera) { liveCamera = camera }
    val bounds by rememberUpdatedState(endlessCameraBounds(world))
    val currentOnReveal by rememberUpdatedState(onReveal)
    val currentOnToggleFlag by rememberUpdatedState(onToggleFlag)
    val currentOnCameraChanged by rememberUpdatedState(onCameraChanged)

    val openFades = remember { EndlessOpenFades() }
    var frameMillis by remember { mutableLongStateOf(0L) }
    LaunchedEffect(world) {
        var now = withFrameMillis { it }
        openFades.observe(world, now, animate = !reduceMotion)
        frameMillis = now
        while (openFades.hasActive(now)) {
            now = withFrameMillis { it }
            frameMillis = now
        }
    }

    Canvas(
        modifier
            .clipToBounds()
            .pointerInput(Unit) {
                val slop = viewConfiguration.touchSlop
                val longPressMillis = viewConfiguration.longPressTimeoutMillis
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    fun moveCamera(event: PointerEvent) {
                        val centroid = event.calculateCentroid()
                        val pan = event.calculatePan()
                        var next = liveCamera
                        if (centroid != Offset.Unspecified) next = next.zoomed(event.calculateZoom(), centroid.x, centroid.y, width, height)
                        liveCamera = next.panned(pan.x, pan.y, width, height).clampedTo(bounds)
                        event.changes.forEach { it.consume() }
                    }
                    when (val start = withTimeoutOrNull(longPressMillis) { awaitTapOrMove(slop) }) {
                        null -> {
                            endlessCellAt(liveCamera, width, height, down.position.x, down.position.y)?.let(currentOnToggleFlag)
                            awaitAllUp()
                        }
                        GestureStart.Tap -> endlessCellAt(liveCamera, width, height, down.position.x, down.position.y)?.let(currentOnReveal)
                        GestureStart.Cancel -> Unit
                        is GestureStart.Move -> {
                            moveCamera(start.event)
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.none { it.pressed }) break
                                moveCamera(event)
                            }
                            currentOnCameraChanged(liveCamera)
                        }
                    }
                }
            },
    ) {
        val cell = endlessCellPx(liveCamera, size.width, size.height)
        val left = liveCamera.centerX - size.width / 2 / cell
        val top = liveCamera.centerY - size.height / 2 / cell
        val tile = (cell - gapPx).coerceAtLeast(1f)
        val corner = CornerRadius(tile * CORNER_FRACTION)
        val glyphSize = Size((tile - glyphInsetPx * 2).coerceAtLeast(1f), (tile - glyphInsetPx * 2).coerceAtLeast(1f))
        val numberStyle = TextStyle(fontSize = (tile * NUMBER_SIZE_FRACTION).toSp(), fontWeight = FontWeight.Bold)
        val numberLayouts = (1..8).associateWith { count ->
            textMeasurer.measure(count.toString(), numberStyle.copy(color = numberColors.getOrElse(count - 1) { numberColors.last() }))
        }

        val firstRow = floor(top / ENDLESS_SECTION_SPAN).toInt() * ENDLESS_SECTION_SIZE
        val lastRow = (floor((top + size.height / cell) / ENDLESS_SECTION_SPAN).toInt() + 1) * ENDLESS_SECTION_SIZE - 1
        val firstCol = floor(left / ENDLESS_SECTION_SPAN).toInt() * ENDLESS_SECTION_SIZE
        val lastCol = (floor((left + size.width / cell) / ENDLESS_SECTION_SPAN).toInt() + 1) * ENDLESS_SECTION_SIZE - 1
        for (y in firstRow..lastRow) {
            for (x in firstCol..lastCol) {
                val worldCell = WorldCell(x, y)
                val pos = worldCell.section
                val sectionState = endlessDisplayState(world, pos)
                val cellState = world.cellStateAt(worldCell)
                val isMine = world.mineAt(worldCell) == true
                val showMine = cellState == EndlessCellState.KNOWN_MINE ||
                    (revealMines && isMine && sectionState == SectionState.OPEN && cellState != EndlessCellState.REVEALED)
                val base = when {
                    showMine -> knownMineColor
                    cellState == EndlessCellState.REVEALED -> openedColor
                    (x + y).mod(2) == 0 -> hiddenBase
                    else -> hiddenAlt
                }
                val tone = when (sectionState) {
                    SectionState.OPEN -> lerp(base, surface, CLOSED_DIM * openFades.dim(pos, frameMillis))
                    SectionState.CLEARED -> lerp(base, surface, CLEARED_MUTE)
                    SectionState.BUILT, SectionState.UNBUILT -> lerp(base, surface, CLOSED_DIM)
                }
                val origin = Offset((endlessLayoutOf(x) - left) * cell, (endlessLayoutOf(y) - top) * cell)
                drawRoundRect(tone, origin, Size(tile, tile), corner)
                val glyphOrigin = Offset(origin.x + glyphInsetPx, origin.y + glyphInsetPx)
                when {
                    showMine && halloweenColors != null -> drawMinesweeperPumpkin(glyphOrigin, glyphSize, halloweenColors)
                    showMine -> drawMinesweeperMine(glyphOrigin, glyphSize, mineBody, mineFuse)
                    cellState == EndlessCellState.FLAGGED && halloweenColors != null -> drawMinesweeperCandle(glyphOrigin, glyphSize, halloweenColors)
                    cellState == EndlessCellState.FLAGGED -> drawMinesweeperFlag(glyphOrigin, glyphSize, flagPole, flagBanner)
                    cellState == EndlessCellState.REVEALED -> {
                        val layout = numberLayouts[world.numberAt(worldCell)] ?: continue
                        drawText(
                            layout,
                            topLeft = Offset(
                                origin.x + (tile - layout.size.width) / 2,
                                origin.y + (tile - layout.size.height) / 2,
                            ),
                        )
                    }
                }
            }
        }
    }
}

private sealed interface GestureStart {
    data object Tap : GestureStart
    data object Cancel : GestureStart
    data class Move(val event: PointerEvent) : GestureStart
}

private suspend fun AwaitPointerEventScope.awaitTapOrMove(slop: Float): GestureStart {
    var travelled = 0f
    while (true) {
        val event = awaitPointerEvent()
        val pressed = event.changes.filter { it.pressed }
        if (pressed.isEmpty()) return if (event.changes.all { it.changedToUp() }) GestureStart.Tap else GestureStart.Cancel
        if (pressed.size >= 2) return GestureStart.Move(event)
        travelled += pressed.first().positionChange().getDistance()
        if (travelled > slop) return GestureStart.Move(event)
    }
}

private suspend fun AwaitPointerEventScope.awaitAllUp() {
    while (awaitPointerEvent().changes.any { it.pressed }) Unit
}
