package com.quietgrid.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.quietgrid.app.R
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.difficultyColor
import com.quietgrid.app.core.tips.QuietTip
import com.quietgrid.app.core.tips.TipKind
import com.quietgrid.app.data.ScreenRotation
import com.quietgrid.app.ui.components.rememberReduceMotion
import kotlinx.coroutines.flow.first

private const val RING_MS = 1600
private const val RING_GROWTH = 0.5f
private const val RING_START_ALPHA = 0.9f
private val TIP_ICON_SIZE = 26.dp
private val POPOVER_WIDTH = 288.dp
private val POPOVER_GAP = 2.dp
private val POPOVER_MARGIN = 12.dp
private val POPOVER_END_OVERHANG = 40.dp

class QuietTipActions(
    val onOpened: () -> Unit = {},
    val onAction: () -> Unit = {},
    val onUndo: () -> Unit = {},
    val onDoneClosed: () -> Unit = {},
    val onQuietThis: () -> Unit = {},
    val onQuietAll: () -> Unit = {},
    val onSnooze: () -> Unit = {},
)

@Composable
fun QuietTipButton(
    tip: QuietTip,
    actions: QuietTipActions,
    modifier: Modifier = Modifier,
    ringWhen: () -> Boolean = { true },
) {
    var open by remember { mutableStateOf(false) }
    var done by remember(tip) { mutableStateOf(false) }
    var sheetOpen by remember { mutableStateOf(false) }
    val ring = rememberTipRing(ringWhen)

    fun closePopover() {
        open = false
        if (done) actions.onDoneClosed()
    }

    Box(modifier) {
        IconButton(
            onClick = {
                open = true
                actions.onOpened()
            },
        ) {
            TipIcon(open = open, ringProgress = { ring.value })
        }
        if (open) {
            Popup(
                popupPositionProvider = rememberTipPopoverPosition(),
                onDismissRequest = ::closePopover,
                properties = PopupProperties(focusable = true),
            ) {
                TipPopover(
                    tip = tip,
                    done = done,
                    onAction = {
                        actions.onAction()
                        if (tip.kind == TipKind.SETTING) done = true else open = false
                    },
                    onUndo = {
                        actions.onUndo()
                        done = false
                    },
                    onBeQuiet = {
                        open = false
                        sheetOpen = true
                    },
                )
            }
        }
    }
    if (sheetOpen) {
        BeQuietSheet(
            onQuietThis = {
                sheetOpen = false
                actions.onQuietThis()
            },
            onQuietAll = {
                sheetOpen = false
                actions.onQuietAll()
            },
            onSnooze = {
                sheetOpen = false
                actions.onSnooze()
            },
            onDismiss = { sheetOpen = false },
        )
    }
}

@Composable
private fun rememberTipRing(ringWhen: () -> Boolean): Animatable<Float, *> {
    val reduceMotion = rememberReduceMotion()
    var ringPlayed by rememberSaveable { mutableStateOf(false) }
    val ring = remember { Animatable(if (ringPlayed || reduceMotion) 1f else 0f) }
    val currentRingWhen by rememberUpdatedState(ringWhen)
    LaunchedEffect(ring) {
        if (ring.value >= 1f) return@LaunchedEffect
        snapshotFlow { currentRingWhen() }.first { it }
        ring.animateTo(1f, tween(RING_MS, easing = LinearOutSlowInEasing))
        ringPlayed = true
    }
    return ring
}

@Composable
private fun TipIcon(open: Boolean, ringProgress: () -> Float) {
    val colors = MaterialTheme.colorScheme
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(TIP_ICON_SIZE)
                .graphicsLayer {
                    val progress = ringProgress()
                    val scale = 1f + RING_GROWTH * progress
                    scaleX = scale
                    scaleY = scale
                    alpha = if (progress > 0f && progress < 1f) RING_START_ALPHA * (1f - progress) else 0f
                }
                .border(1.dp, colors.primary, CircleShape),
        )
        Box(
            Modifier
                .size(TIP_ICON_SIZE)
                .border(1.dp, if (open) colors.primary else colors.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = stringResource(R.string.tip_open_content_description),
                tint = if (open) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun rememberTipPopoverPosition(): PopupPositionProvider {
    val density = LocalDensity.current
    return remember(density) {
        with(density) {
            TipPopoverPosition(POPOVER_GAP.roundToPx(), POPOVER_MARGIN.roundToPx(), POPOVER_END_OVERHANG.roundToPx())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TipPopover(tip: QuietTip, done: Boolean, onAction: () -> Unit, onUndo: () -> Unit, onBeQuiet: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val copy = tipCopy(tip)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outline),
        shadowElevation = 8.dp,
        modifier = Modifier.width(POPOVER_WIDTH),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    if (done) Icons.Outlined.Check else tip.eyebrowIcon(),
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    stringResource(if (done) R.string.tip_done_eyebrow else R.string.tip_eyebrow),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Text(
                if (done) copy.done.orEmpty() else copy.body,
                style = MaterialTheme.typography.bodyMedium,
                color = if (done) colors.onSurfaceVariant else colors.onSurface,
                modifier = Modifier.padding(top = 6.dp, end = 8.dp),
            )
            if (!done && tip is QuietTip.MakeMix) {
                FlowRow(
                    Modifier.padding(top = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tip.games.forEach { (gameId, difficulty) ->
                        MixGameChip(gameId, difficultyColor(difficulty))
                    }
                }
            }
            TipPopoverActions(done, copy.action, onAction, onUndo, onBeQuiet)
        }
    }
}

@Composable
private fun TipPopoverActions(done: Boolean, actionLabel: String, onAction: () -> Unit, onUndo: () -> Unit, onBeQuiet: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val buttonPadding = PaddingValues(horizontal = 8.dp)
        if (done) {
            TextButton(onClick = onUndo, contentPadding = buttonPadding, modifier = Modifier.offset(x = (-8).dp)) {
                Text(stringResource(R.string.tip_undo), fontWeight = FontWeight.Bold)
            }
        } else {
            TextButton(onClick = onAction, contentPadding = buttonPadding, modifier = Modifier.offset(x = (-8).dp)) {
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = onBeQuiet,
                contentPadding = buttonPadding,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            ) {
                Text(stringResource(R.string.tip_be_quiet))
            }
        }
    }
}

@Composable
private fun MixGameChip(gameId: GameId, dotColor: Color) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .border(1.dp, colors.outline, CircleShape)
            .background(colors.surfaceVariant, CircleShape)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(6.dp).background(dotColor, CircleShape))
        Text(stringResource(GameCatalog.get(gameId).titleRes), style = MaterialTheme.typography.labelSmall)
    }
}

private class TipCopy(val body: String, val action: String, val done: String?)

@Composable
private fun tipCopy(tip: QuietTip): TipCopy = when (tip) {
    is QuietTip.LockRotation -> {
        val title = stringResource(GameCatalog.get(tip.gameId).titleRes)
        if (tip.target == ScreenRotation.LANDSCAPE) {
            TipCopy(
                stringResource(R.string.tip_rotation_body_landscape, title),
                stringResource(R.string.tip_rotation_action_landscape, title),
                stringResource(R.string.tip_rotation_done_landscape, title),
            )
        } else {
            TipCopy(
                stringResource(R.string.tip_rotation_body_portrait, title),
                stringResource(R.string.tip_rotation_action_portrait, title),
                stringResource(R.string.tip_rotation_done_portrait, title),
            )
        }
    }
    is QuietTip.KeepAwake -> {
        val title = stringResource(GameCatalog.get(tip.gameId).titleRes)
        TipCopy(
            stringResource(R.string.tip_keep_awake_body, title),
            stringResource(R.string.tip_keep_awake_action),
            stringResource(R.string.tip_keep_awake_done, title),
        )
    }
    QuietTip.ReduceMotion -> TipCopy(
        stringResource(R.string.tip_reduce_motion_body),
        stringResource(R.string.tip_reduce_motion_action),
        stringResource(R.string.tip_reduce_motion_done),
    )
    is QuietTip.MakeMix -> TipCopy(stringResource(R.string.tip_mix_body), stringResource(R.string.tip_mix_action), null)
    QuietTip.TryDaily -> TipCopy(stringResource(R.string.tip_daily_body), stringResource(R.string.tip_daily_action), null)
}

private fun QuietTip.eyebrowIcon(): ImageVector = when (this) {
    is QuietTip.LockRotation -> Icons.Outlined.ScreenRotation
    is QuietTip.KeepAwake -> Icons.Outlined.LightMode
    QuietTip.ReduceMotion -> Icons.Outlined.Speed
    is QuietTip.MakeMix -> Icons.Outlined.Shuffle
    QuietTip.TryDaily -> Icons.Outlined.Today
}

private class TipPopoverPosition(
    private val gapPx: Int,
    private val marginPx: Int,
    private val endOverhangPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val preferredX = if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.right + endOverhangPx - popupContentSize.width
        } else {
            anchorBounds.left - endOverhangPx
        }
        val maxX = (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx)
        val below = anchorBounds.bottom + gapPx
        val fitsBelow = below + popupContentSize.height <= windowSize.height - marginPx
        val y = if (fitsBelow) below else (anchorBounds.top - gapPx - popupContentSize.height).coerceAtLeast(marginPx)
        return IntOffset(preferredX.coerceIn(marginPx, maxX), y)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BeQuietSheet(onQuietThis: () -> Unit, onQuietAll: () -> Unit, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
            Text(stringResource(R.string.tip_sheet_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.tip_sheet_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            BeQuietRow("🤫", R.string.tip_sheet_this_label, R.string.tip_sheet_this_detail, onQuietThis)
            BeQuietRow("🌙", R.string.tip_sheet_all_label, R.string.tip_sheet_all_detail, onQuietAll)
            BeQuietRow("⏳", R.string.tip_sheet_later_label, R.string.tip_sheet_later_detail, onSnooze)
        }
    }
}

@Composable
private fun BeQuietRow(emoji: String, labelRes: Int, detailRes: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, style = MaterialTheme.typography.titleSmall)
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(labelRes), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(detailRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
