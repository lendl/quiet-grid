package com.quietgrid.app.ui.journey.map

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.quietgrid.app.games.animaldoku.rememberAnimalDokuRegionColors
import com.quietgrid.app.games.blockescape.rememberBlockEscapeColors
import com.quietgrid.app.games.links.rememberLinksPairColors
import com.quietgrid.app.ui.theme.LocalIsPencilTheme
import com.quietgrid.engine.arrowescape.ARROW_ESCAPE_MASK_ACTIVE
import com.quietgrid.engine.arrowescape.ArrowEscapePieceData
import com.quietgrid.engine.blockescape.BlockEscapeBlock
import com.quietgrid.engine.circuit.CIRCUIT_DOWN
import com.quietgrid.engine.circuit.CIRCUIT_LEFT
import com.quietgrid.engine.circuit.CIRCUIT_RIGHT
import com.quietgrid.engine.circuit.CIRCUIT_UP
import com.quietgrid.engine.links.LinksPuzzleEntry
import com.quietgrid.engine.wordswap.WORDSWAP_HOLE

sealed interface JourneyBoardPreviewData {
    data class Arrows(val rows: Int, val cols: Int, val pieces: List<ArrowEscapePieceData>, val mask: List<String>? = null) : JourneyBoardPreviewData

    data class Regions(val size: Int, val regions: List<List<Int>>) : JourneyBoardPreviewData

    data class Digits(val size: Int, val boxRows: Int, val boxCols: Int, val givens: List<List<Int?>>) : JourneyBoardPreviewData

    data class Cages(val size: Int, val boxSize: Int, val cageOf: List<Int>) : JourneyBoardPreviewData

    data class EmptyGrid(val rows: Int, val cols: Int, val guideEvery: Int) : JourneyBoardPreviewData

    data class Blocks(val size: Int, val blocks: List<BlockEscapeBlock>, val exitRow: Int) : JourneyBoardPreviewData

    data class Wires(val rows: Int, val cols: Int, val masks: List<Int>, val source: Int) : JourneyBoardPreviewData

    data class Pairs(val puzzle: LinksPuzzleEntry) : JourneyBoardPreviewData

    data class Letters(val rows: Int, val cols: Int, val letters: String) : JourneyBoardPreviewData
}

@Composable
fun JourneyBoardPreview(data: JourneyBoardPreviewData, modifier: Modifier = Modifier) {
    when (data) {
        is JourneyBoardPreviewData.Arrows -> ArrowsPreview(data, modifier)
        is JourneyBoardPreviewData.Regions -> RegionsPreview(data, modifier)
        is JourneyBoardPreviewData.Digits -> DigitsPreview(data, modifier)
        is JourneyBoardPreviewData.Cages -> CagesPreview(data, modifier)
        is JourneyBoardPreviewData.EmptyGrid -> EmptyGridPreview(data, modifier)
        is JourneyBoardPreviewData.Blocks -> BlocksPreview(data, modifier)
        is JourneyBoardPreviewData.Wires -> WiresPreview(data, modifier)
        is JourneyBoardPreviewData.Pairs -> PairsPreview(data, modifier)
        is JourneyBoardPreviewData.Letters -> LettersPreview(data, modifier)
    }
}

@Composable
private fun ArrowsPreview(data: JourneyBoardPreviewData.Arrows, modifier: Modifier) {
    val dot = MaterialTheme.colorScheme.outlineVariant
    val ink = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    Canvas(modifier) {
        if (data.rows <= 0 || data.cols <= 0) return@Canvas
        val cell = minOf(size.width / data.cols, size.height / data.rows)
        val left = (size.width - cell * data.cols) / 2
        val top = (size.height - cell * data.rows) / 2
        fun centre(row: Int, col: Int) = Offset(left + (col + 0.5f) * cell, top + (row + 0.5f) * cell)
        for (row in 0 until data.rows) {
            for (col in 0 until data.cols) {
                val active = data.mask?.getOrNull(row)?.getOrNull(col)?.let { it == ARROW_ESCAPE_MASK_ACTIVE } ?: true
                if (active) drawCircle(dot, radius = cell * 0.12f, center = centre(row, col))
            }
        }
        val stroke = Stroke(width = cell * 0.32f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        data.pieces.forEach { piece ->
            val points = piece.cells.map { centre(it[0], it[1]) }
            if (points.isEmpty()) return@forEach
            val (dx, dy) = when (piece.headDirection) {
                "up" -> 0f to -1f
                "down" -> 0f to 1f
                "left" -> -1f to 0f
                else -> 1f to 0f
            }
            val head = points.last()
            val tip = Offset(head.x + dx * cell * 0.4f, head.y + dy * cell * 0.4f)
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
                lineTo(tip.x, tip.y)
                val back = cell * 0.3f
                moveTo(tip.x - dx * back - dy * back, tip.y - dy * back + dx * back)
                lineTo(tip.x, tip.y)
                lineTo(tip.x - dx * back + dy * back, tip.y - dy * back - dx * back)
            }
            drawPath(path, ink, style = stroke)
        }
    }
}

@Composable
private fun RegionsPreview(data: JourneyBoardPreviewData.Regions, modifier: Modifier) {
    val pencil = LocalIsPencilTheme.current
    val regionColors = rememberAnimalDokuRegionColors(data.regions)
    val surface = MaterialTheme.colorScheme.surface
    val line = MaterialTheme.colorScheme.onSurface
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        if (data.size <= 0) return@Canvas
        val cell = minOf(size.width, size.height) / data.size
        val left = (size.width - cell * data.size) / 2
        val top = (size.height - cell * data.size) / 2
        for (row in 0 until data.size) {
            for (col in 0 until data.size) {
                val region = data.regions.getOrNull(row)?.getOrNull(col) ?: continue
                val colour = if (pencil) surface else regionColors.getOrElse(region) { surface }
                drawRect(colour, Offset(left + col * cell, top + row * cell), Size(cell, cell))
            }
        }
        val thin = cell * 0.04f
        val thick = cell * 0.12f
        for (row in 0 until data.size) {
            for (col in 0 until data.size) {
                val region = data.regions.getOrNull(row)?.getOrNull(col) ?: continue
                val x = left + col * cell
                val y = top + row * cell
                if (col + 1 < data.size) {
                    val split = data.regions[row][col + 1] != region
                    drawLine(if (split && pencil) line else grid, Offset(x + cell, y), Offset(x + cell, y + cell), if (split && pencil) thick else thin)
                }
                if (row + 1 < data.size) {
                    val split = data.regions[row + 1][col] != region
                    drawLine(if (split && pencil) line else grid, Offset(x, y + cell), Offset(x + cell, y + cell), if (split && pencil) thick else thin)
                }
            }
        }
    }
}

@Composable
private fun DigitsPreview(data: JourneyBoardPreviewData.Digits, modifier: Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val surface = MaterialTheme.colorScheme.surface
    val ink = MaterialTheme.colorScheme.onSurface
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        if (data.size <= 0) return@Canvas
        val cell = minOf(size.width, size.height) / data.size
        val left = (size.width - cell * data.size) / 2
        val top = (size.height - cell * data.size) / 2
        drawRect(surface, Offset(left, top), Size(cell * data.size, cell * data.size))
        for (i in 0..data.size) {
            val vertical = if (i % data.boxCols == 0) ink else grid
            val horizontal = if (i % data.boxRows == 0) ink else grid
            val verticalWidth = if (i % data.boxCols == 0) cell * 0.06f else cell * 0.03f
            val horizontalWidth = if (i % data.boxRows == 0) cell * 0.06f else cell * 0.03f
            drawLine(vertical, Offset(left + i * cell, top), Offset(left + i * cell, top + cell * data.size), verticalWidth)
            drawLine(horizontal, Offset(left, top + i * cell), Offset(left + cell * data.size, top + i * cell), horizontalWidth)
        }
        val style = TextStyle(color = ink, fontSize = (cell * 0.6f).toSp(), fontWeight = FontWeight.SemiBold)
        data.givens.forEachIndexed { row, values ->
            values.forEachIndexed { col, value ->
                if (value == null) return@forEachIndexed
                val layout = textMeasurer.measure(value.toString(), style)
                drawText(
                    layout,
                    topLeft = Offset(
                        left + col * cell + (cell - layout.size.width) / 2,
                        top + row * cell + (cell - layout.size.height) / 2,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CagesPreview(data: JourneyBoardPreviewData.Cages, modifier: Modifier) {
    val surface = MaterialTheme.colorScheme.surface
    val ink = MaterialTheme.colorScheme.onSurface
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        if (data.size <= 0 || data.cageOf.size != data.size * data.size) return@Canvas
        val cell = minOf(size.width, size.height) / data.size
        val left = (size.width - cell * data.size) / 2
        val top = (size.height - cell * data.size) / 2
        drawRect(surface, Offset(left, top), Size(cell * data.size, cell * data.size))
        for (i in 0..data.size) {
            val width = if (i % data.boxSize == 0) cell * 0.05f else cell * 0.02f
            drawLine(grid, Offset(left + i * cell, top), Offset(left + i * cell, top + cell * data.size), width)
            drawLine(grid, Offset(left, top + i * cell), Offset(left + cell * data.size, top + i * cell), width)
        }
        val cageLine = cell * 0.07f
        fun cage(row: Int, col: Int): Int = data.cageOf[row * data.size + col]
        for (row in 0 until data.size) {
            for (col in 0 until data.size) {
                val x = left + col * cell
                val y = top + row * cell
                val own = cage(row, col)
                if (col == 0 || cage(row, col - 1) != own) drawLine(ink, Offset(x, y), Offset(x, y + cell), cageLine)
                if (row == 0 || cage(row - 1, col) != own) drawLine(ink, Offset(x, y), Offset(x + cell, y), cageLine)
                if (col == data.size - 1) drawLine(ink, Offset(x + cell, y), Offset(x + cell, y + cell), cageLine)
                if (row == data.size - 1) drawLine(ink, Offset(x, y + cell), Offset(x + cell, y + cell), cageLine)
            }
        }
    }
}

@Composable
private fun EmptyGridPreview(data: JourneyBoardPreviewData.EmptyGrid, modifier: Modifier) {
    val surface = MaterialTheme.colorScheme.surface
    val ink = MaterialTheme.colorScheme.onSurface
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        if (data.rows <= 0 || data.cols <= 0) return@Canvas
        val cell = minOf(size.width / data.cols, size.height / data.rows)
        val left = (size.width - cell * data.cols) / 2
        val top = (size.height - cell * data.rows) / 2
        drawRect(surface, Offset(left, top), Size(cell * data.cols, cell * data.rows))
        for (col in 0..data.cols) {
            val edge = col == 0 || col == data.cols
            val guide = col % data.guideEvery == 0
            drawLine(if (edge) ink else grid, Offset(left + col * cell, top), Offset(left + col * cell, top + cell * data.rows), if (edge || guide) cell * 0.06f else cell * 0.03f)
        }
        for (row in 0..data.rows) {
            val edge = row == 0 || row == data.rows
            val guide = row % data.guideEvery == 0
            drawLine(if (edge) ink else grid, Offset(left, top + row * cell), Offset(left + cell * data.cols, top + row * cell), if (edge || guide) cell * 0.06f else cell * 0.03f)
        }
    }
}

@Composable
private fun BlocksPreview(data: JourneyBoardPreviewData.Blocks, modifier: Modifier) {
    val colors = rememberBlockEscapeColors()
    val surface = MaterialTheme.colorScheme.surface
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        if (data.size <= 0) return@Canvas
        val cell = minOf(size.width, size.height) / data.size
        val left = (size.width - cell * data.size) / 2
        val top = (size.height - cell * data.size) / 2
        drawRect(surface, Offset(left, top), Size(cell * data.size, cell * data.size))
        drawRect(grid, Offset(left, top), Size(cell * data.size, cell * data.size), style = Stroke(cell * 0.05f))
        val gap = cell * 0.08f
        data.blocks.forEachIndexed { index, block ->
            val width = if (block.horizontal) block.length * cell else cell
            val height = if (block.horizontal) cell else block.length * cell
            drawRoundRect(
                color = if (index == 0) colors.key else colors.block,
                topLeft = Offset(left + block.col * cell + gap, top + block.row * cell + gap),
                size = Size(width - gap * 2, height - gap * 2),
                cornerRadius = CornerRadius(cell * 0.18f),
            )
        }
        val exitY = top + data.exitRow * cell
        drawLine(colors.key, Offset(left + cell * data.size, exitY + gap), Offset(left + cell * data.size, exitY + cell - gap), cell * 0.12f)
    }
}

@Composable
private fun WiresPreview(data: JourneyBoardPreviewData.Wires, modifier: Modifier) {
    val surface = MaterialTheme.colorScheme.surface
    val grid = MaterialTheme.colorScheme.outlineVariant
    val ink = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    val power = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        if (data.rows <= 0 || data.cols <= 0 || data.masks.size != data.rows * data.cols) return@Canvas
        val cell = minOf(size.width / data.cols, size.height / data.rows)
        val left = (size.width - cell * data.cols) / 2
        val top = (size.height - cell * data.rows) / 2
        drawRect(surface, Offset(left, top), Size(cell * data.cols, cell * data.rows))
        for (col in 0..data.cols) drawLine(grid, Offset(left + col * cell, top), Offset(left + col * cell, top + cell * data.rows), cell * 0.03f)
        for (row in 0..data.rows) drawLine(grid, Offset(left, top + row * cell), Offset(left + cell * data.cols, top + row * cell), cell * 0.03f)
        val wire = cell * 0.16f
        data.masks.forEachIndexed { index, mask ->
            val centre = Offset(left + (index % data.cols + 0.5f) * cell, top + (index / data.cols + 0.5f) * cell)
            val half = cell / 2
            if (mask and CIRCUIT_UP != 0) drawLine(ink, centre, Offset(centre.x, centre.y - half), wire, StrokeCap.Round)
            if (mask and CIRCUIT_DOWN != 0) drawLine(ink, centre, Offset(centre.x, centre.y + half), wire, StrokeCap.Round)
            if (mask and CIRCUIT_LEFT != 0) drawLine(ink, centre, Offset(centre.x - half, centre.y), wire, StrokeCap.Round)
            if (mask and CIRCUIT_RIGHT != 0) drawLine(ink, centre, Offset(centre.x + half, centre.y), wire, StrokeCap.Round)
            if (index == data.source) drawCircle(power, radius = cell * 0.26f, center = centre)
        }
    }
}

@Composable
private fun PairsPreview(data: JourneyBoardPreviewData.Pairs, modifier: Modifier) {
    val pairColors = rememberLinksPairColors(data.puzzle)
    val surface = MaterialTheme.colorScheme.surface
    val grid = MaterialTheme.colorScheme.outlineVariant
    val size = data.puzzle.size
    Canvas(modifier) {
        if (size <= 0) return@Canvas
        val cell = minOf(this.size.width, this.size.height) / size
        val left = (this.size.width - cell * size) / 2
        val top = (this.size.height - cell * size) / 2
        drawRect(surface, Offset(left, top), Size(cell * size, cell * size))
        for (i in 0..size) {
            drawLine(grid, Offset(left + i * cell, top), Offset(left + i * cell, top + cell * size), cell * 0.04f)
            drawLine(grid, Offset(left, top + i * cell), Offset(left + cell * size, top + i * cell), cell * 0.04f)
        }
        data.puzzle.endpoints.forEachIndexed { pair, ends ->
            val colour = pairColors.getOrNull(pair) ?: return@forEachIndexed
            ends.forEach { endCell ->
                val centre = Offset(left + (endCell % size + 0.5f) * cell, top + (endCell / size + 0.5f) * cell)
                drawCircle(colour, radius = cell * 0.36f, center = centre)
            }
        }
    }
}

@Composable
private fun LettersPreview(data: JourneyBoardPreviewData.Letters, modifier: Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val tile = MaterialTheme.colorScheme.surfaceVariant
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier) {
        if (data.rows <= 0 || data.cols <= 0 || data.letters.length != data.rows * data.cols) return@Canvas
        val cell = minOf(size.width / data.cols, size.height / data.rows)
        val left = (size.width - cell * data.cols) / 2
        val top = (size.height - cell * data.rows) / 2
        val gap = cell * 0.06f
        val style = TextStyle(color = ink, fontSize = (cell * 0.55f).toSp(), fontWeight = FontWeight.SemiBold)
        data.letters.forEachIndexed { index, letter ->
            if (letter == WORDSWAP_HOLE) return@forEachIndexed
            val x = left + (index % data.cols) * cell
            val y = top + (index / data.cols) * cell
            drawRoundRect(tile, Offset(x + gap, y + gap), Size(cell - gap * 2, cell - gap * 2), CornerRadius(cell * 0.14f))
            val layout = textMeasurer.measure(letter.uppercaseChar().toString(), style)
            drawText(layout, topLeft = Offset(x + (cell - layout.size.width) / 2, y + (cell - layout.size.height) / 2))
        }
    }
}