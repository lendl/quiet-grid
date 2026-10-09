package com.quietgrid.app.games.sequencememory

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.quietgrid.app.R
import com.quietgrid.app.ui.theme.LocalIsPencilTheme

private val SEQUENCE_MEMORY_PAD_GAP = 12.dp
private const val SEQUENCE_MEMORY_IDLE_BLEND = 0.38f
private const val SEQUENCE_MEMORY_LIT_SCALE = 1.05f
private const val SEQUENCE_MEMORY_LIGHT_ANIMATION_MS = 70
private val SEQUENCE_MEMORY_LIT_COLOR_PENCIL = Color(0xFF141414)
private val SEQUENCE_MEMORY_IDLE_COLOR_PENCIL = Color(0xFFE0E0E0)
private val SEQUENCE_MEMORY_PAD_COLORS = listOf(
    Color(0xFFE69F00),
    Color(0xFF56B4E9),
    Color(0xFFCC79A7),
    Color(0xFF009E73),
    Color(0xFF0072B2),
    Color(0xFFD55E00),
    Color(0xFFF0E442),
    Color(0xFF882255),
    Color(0xFF999999),
)

@Composable
fun SequenceMemoryGrid(
    padCount: Int,
    columns: Int,
    litPad: Int?,
    wrongPad: Int?,
    enabled: Boolean,
    onPadTap: (Int) -> Unit,
) {
    val rows = (padCount + columns - 1) / columns
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val padSize = min(
            (maxWidth - SEQUENCE_MEMORY_PAD_GAP * (columns - 1)) / columns,
            (maxHeight - SEQUENCE_MEMORY_PAD_GAP * (rows - 1)) / rows,
        )
        Column(verticalArrangement = Arrangement.spacedBy(SEQUENCE_MEMORY_PAD_GAP)) {
            for (row in 0 until rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(SEQUENCE_MEMORY_PAD_GAP)) {
                    for (col in 0 until columns) {
                        val pad = row * columns + col
                        if (pad < padCount) {
                            SequenceMemoryPad(
                                pad = pad,
                                size = padSize,
                                lit = pad == litPad,
                                wrong = pad == wrongPad,
                                enabled = enabled,
                                onTap = { onPadTap(pad) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SequenceMemoryPad(
    pad: Int,
    size: Dp,
    lit: Boolean,
    wrong: Boolean,
    enabled: Boolean,
    onTap: () -> Unit,
) {
    val pencil = LocalIsPencilTheme.current
    val baseColor = SEQUENCE_MEMORY_PAD_COLORS[pad % SEQUENCE_MEMORY_PAD_COLORS.size]
    val idleColor = if (pencil) SEQUENCE_MEMORY_IDLE_COLOR_PENCIL else lerp(MaterialTheme.colorScheme.surfaceVariant, baseColor, SEQUENCE_MEMORY_IDLE_BLEND)
    val litColor = if (pencil) SEQUENCE_MEMORY_LIT_COLOR_PENCIL else baseColor
    val color by animateColorAsState(
        targetValue = if (lit) litColor else idleColor,
        animationSpec = tween(SEQUENCE_MEMORY_LIGHT_ANIMATION_MS),
        label = "sequenceMemoryPadColor",
    )
    val scale by animateFloatAsState(
        targetValue = if (lit) SEQUENCE_MEMORY_LIT_SCALE else 1f,
        animationSpec = tween(SEQUENCE_MEMORY_LIGHT_ANIMATION_MS),
        label = "sequenceMemoryPadScale",
    )
    val shape = RoundedCornerShape(size * 0.2f)
    val description = stringResource(R.string.sequencememory_pad_content_description, pad + 1)
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(color)
            .then(if (wrong) Modifier.border(4.dp, MaterialTheme.colorScheme.error, shape) else Modifier)
            .semantics { contentDescription = description }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onTap,
            ),
    )
}
