package com.quietgrid.app.games.whodidit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.quietgrid.engine.whodidit.WhoDidItPuzzleEntry
import com.quietgrid.engine.whodidit.WhoDidItRef
import com.quietgrid.engine.whodidit.whoDidItCategoryPairs

private val ROW_LABEL_WIDTH = 58.dp
private val COLUMN_LABEL_HEIGHT = 62.dp
private val GROUP_GAP = 4.dp
private val MAX_CELL = 44.dp
private const val LABEL_SHARE = 0.3f
private const val GAP_SHARE = 0.02f
private const val HINT_PREVIEW_ALPHA = 0.3f

data class WhoDidItGridMetrics(val cell: Int, val rowLabelWidth: Int, val columnLabelHeight: Int, val gap: Int)

fun whoDidItGridMetrics(
    maxWidth: Int,
    maxHeight: Int,
    density: Float,
    size: Int,
    columnGroups: Int,
    rowGroups: Int,
): WhoDidItGridMetrics {
    fun px(dp: Dp) = dp.value * density
    val rowLabelWidth = minOf(px(ROW_LABEL_WIDTH), maxWidth * LABEL_SHARE).toInt()
    val columnLabelHeight = minOf(px(COLUMN_LABEL_HEIGHT), maxHeight * LABEL_SHARE).toInt()
    val gap = minOf(px(GROUP_GAP), minOf(maxWidth, maxHeight) * GAP_SHARE).toInt()
    val widthFit = (maxWidth - rowLabelWidth - gap * columnGroups) / (columnGroups * size)
    val heightFit = (maxHeight - columnLabelHeight - gap * rowGroups) / (rowGroups * size)
    val cell = minOf(widthFit, heightFit, px(MAX_CELL).toInt()).coerceAtLeast(0)
    return WhoDidItGridMetrics(cell, rowLabelWidth, columnLabelHeight, gap)
}

@Composable
fun WhoDidItGrid(
    puzzle: WhoDidItPuzzleEntry,
    blocks: List<List<List<WhoDidItCellMark>>>,
    text: WhoDidItText,
    hintCells: Map<Triple<Int, Int, Int>, WhoDidItCellMark>,
    wrongPickEvent: WhoDidItWrongPickEvent?,
    onCellTap: (Int, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categoryCount = puzzle.categories.size
    val pairs = remember(categoryCount) { whoDidItCategoryPairs(categoryCount) }
    val rowCategories = remember(pairs) { pairs.map { it.first }.distinct() }
    val lastCategory = categoryCount - 1
    val size = puzzle.size
    var cardRef by remember { mutableStateOf<WhoDidItRef?>(null) }
    val density = LocalDensity.current

    BoxWithConstraints(modifier) {
        val metrics = remember(constraints.maxWidth, constraints.maxHeight, density.density, size, lastCategory, rowCategories.size) {
            whoDidItGridMetrics(constraints.maxWidth, constraints.maxHeight, density.density, size, lastCategory, rowCategories.size)
        }
        val cell = with(density) { metrics.cell.toDp() }
        val rowLabelWidth = with(density) { metrics.rowLabelWidth.toDp() }
        val columnLabelHeight = with(density) { metrics.columnLabelHeight.toDp() }
        val gap = with(density) { metrics.gap.toDp() }
        val labelScale = (cell.value / MAX_CELL.value).coerceIn(0.55f, 1f)

        Column {
            Row {
                Spacer(Modifier.width(rowLabelWidth))
                for (category in 1..lastCategory) {
                    Row(Modifier.padding(start = gap)) {
                        for (slot in 0 until size) {
                            val ref = WhoDidItRef(category, slot)
                            val tag = if (category in rowCategories) "whodidit-collabel-$category-$slot" else "whodidit-label-$category-$slot"
                            ColumnLabel(
                                name = text.entryName(ref),
                                icons = text.traitIcons(ref),
                                width = cell,
                                height = columnLabelHeight,
                                scale = labelScale,
                                modifier = Modifier.testTag(tag).clickable { cardRef = ref },
                            )
                        }
                    }
                }
            }
            for (rowCategory in rowCategories) {
                Spacer(Modifier.height(gap))
                for (slot in 0 until size) {
                    val ref = WhoDidItRef(rowCategory, slot)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RowLabel(
                            name = text.entryName(ref),
                            icons = text.traitIcons(ref),
                            width = rowLabelWidth,
                            height = cell,
                            scale = labelScale,
                            modifier = Modifier
                                .testTag("whodidit-label-$rowCategory-$slot")
                                .clickable { cardRef = ref },
                        )
                        for (category in 1..lastCategory) {
                            val block = pairs.indexOf(rowCategory to category)
                            if (block < 0) continue
                            Row(Modifier.padding(start = gap)) {
                                for (col in 0 until size) {
                                    val shakeId = wrongPickEvent
                                        ?.takeIf { it.block == block && it.row == slot && it.col == col }?.id ?: 0
                                    WhoDidItCell(
                                        mark = blocks[block][slot][col],
                                        hint = hintCells[Triple(block, slot, col)],
                                        shakeTrigger = shakeId,
                                        cell = cell,
                                        onTap = { onCellTap(block, slot, col) },
                                        modifier = Modifier.testTag("whodidit-cell-$block-$slot-$col"),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val ref = cardRef
    if (ref != null) {
        Dialog(onDismissRequest = { cardRef = null }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text.entryName(ref), style = MaterialTheme.typography.titleMedium)
                    text.cardLines(ref).forEach { line ->
                        Text(line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowLabel(name: String, icons: String, width: Dp, height: Dp, scale: Float, modifier: Modifier = Modifier) {
    Column(
        modifier.width(width).height(height).padding(end = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            name,
            fontSize = (11f * scale + 1f).sp,
            lineHeight = (12f * scale + 1f).sp,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(icons, fontSize = (9f * scale + 1f).sp, lineHeight = (10f * scale + 1f).sp, maxLines = 1)
    }
}

@Composable
private fun ColumnLabel(name: String, icons: String, width: Dp, height: Dp, scale: Float, modifier: Modifier = Modifier) {
    Box(modifier.size(width, height), contentAlignment = Alignment.Center) {
        Row(
            Modifier.requiredWidth(height).rotate(-90f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Text(
                name,
                fontSize = (11f * scale + 1f).sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(icons, fontSize = (9f * scale + 1f).sp, maxLines = 1, modifier = Modifier.padding(start = 2.dp))
        }
    }
}

private fun DrawScope.drawWhoDidItMark(mark: WhoDidItCellMark, color: Color) {
    val extent = size
    val inset = extent.minDimension * 0.27f
    val stroke = extent.minDimension * 0.09f
    when (mark) {
        WhoDidItCellMark.O -> drawCircle(color, radius = extent.minDimension * 0.28f)
        WhoDidItCellMark.X, WhoDidItCellMark.AUTO_X -> {
            drawLine(color, Offset(inset, inset), Offset(extent.width - inset, extent.height - inset), stroke, StrokeCap.Round)
            drawLine(color, Offset(extent.width - inset, inset), Offset(inset, extent.height - inset), stroke, StrokeCap.Round)
        }
        WhoDidItCellMark.EMPTY -> Unit
    }
}

@Composable
private fun WhoDidItCell(
    mark: WhoDidItCellMark,
    hint: WhoDidItCellMark?,
    shakeTrigger: Int,
    cell: Dp,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger > 0) {
            shake.snapTo(0f)
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    0f at 0
                    -6f at 60
                    6f at 120
                    -4f at 190
                    4f at 260
                    0f at 350
                },
            )
        }
    }
    val density = LocalDensity.current
    val markColor = MaterialTheme.colorScheme.onSurface
    val dotColor = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val hintColor = MaterialTheme.colorScheme.tertiary
    val hinted = hint != null
    Box(
        modifier
            .graphicsLayer(translationX = with(density) { shake.value.dp.toPx() })
            .size(cell)
            .padding(0.5.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(if (hinted) 2.dp else 0.5.dp, if (hinted) hintColor else outline)
            .clickable(onClick = onTap),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val color = when (mark) {
                WhoDidItCellMark.O -> dotColor
                WhoDidItCellMark.X -> markColor.copy(alpha = 0.8f)
                else -> markColor.copy(alpha = 0.28f)
            }
            drawWhoDidItMark(mark, color)
        }
        if (hint != null && mark == WhoDidItCellMark.EMPTY) {
            val tag = if (hint == WhoDidItCellMark.O) "whodidit-hint-preview-o" else "whodidit-hint-preview-x"
            Canvas(Modifier.fillMaxSize().testTag(tag)) {
                drawWhoDidItMark(hint, hintColor.copy(alpha = HINT_PREVIEW_ALPHA))
            }
        }
    }
}
