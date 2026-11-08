package com.quietgrid.app.games.blockescape

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietgrid.app.R
import com.quietgrid.app.ui.play.PlaySizeClass

private val BLOCKESCAPE_TOOL_MAX_WIDTH = 120.dp

private class BlockEscapeDockMetrics(val toolHeight: Dp, val iconSize: Dp, val labelSize: TextUnit)

private fun blockEscapeDockMetrics(sizeClass: PlaySizeClass): BlockEscapeDockMetrics = when (sizeClass) {
    PlaySizeClass.Compact -> BlockEscapeDockMetrics(48.dp, 20.dp, 11.sp)
    PlaySizeClass.Medium -> BlockEscapeDockMetrics(56.dp, 22.dp, 11.sp)
    PlaySizeClass.PhoneLandscape -> BlockEscapeDockMetrics(56.dp, 20.dp, 12.sp)
    PlaySizeClass.TabletLandscape -> BlockEscapeDockMetrics(84.dp, 30.dp, 16.sp)
    PlaySizeClass.TabletPortrait -> BlockEscapeDockMetrics(84.dp, 30.dp, 16.sp)
}

internal fun blockEscapeDockHeight(sizeClass: PlaySizeClass): Dp = blockEscapeDockMetrics(sizeClass).toolHeight + 8.dp

@Composable
internal fun BlockEscapeDock(
    sizeClass: PlaySizeClass,
    canUndo: Boolean,
    onUndo: () -> Unit,
) {
    val metrics = blockEscapeDockMetrics(sizeClass)
    Row(
        Modifier.fillMaxWidth().height(metrics.toolHeight),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        BlockEscapeTool(Icons.AutoMirrored.Outlined.Undo, R.string.blockescape_undo, metrics, canUndo, onUndo)
    }
}

@Composable
private fun RowScope.BlockEscapeTool(
    icon: ImageVector,
    @StringRes labelRes: Int,
    metrics: BlockEscapeDockMetrics,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.weight(1f, fill = false).widthIn(max = BLOCKESCAPE_TOOL_MAX_WIDTH).fillMaxWidth().fillMaxHeight(),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        contentColor = contentColor,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(metrics.iconSize))
            Text(
                stringResource(labelRes),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = metrics.labelSize, fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
