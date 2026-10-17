package com.quietgrid.app.ui.play

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.QuickRule
import com.quietgrid.app.ui.components.QuickStartContent
import com.quietgrid.app.ui.components.quickStartFor

private val OVERLAY_CARD_MAX_WIDTH = 560.dp
private val OVERLAY_CARD_SHAPE = RoundedCornerShape(18.dp)
private val HINT_CARD_SHAPE = RoundedCornerShape(14.dp)
internal val HINT_CARD_MIN_HEIGHT = 32.dp
private val HINT_CARD_VERTICAL_INSET = 2.dp
private val HINT_BODY_BOTTOM_PADDING = 8.dp
private val QUICK_RULE_PICTURE_HEIGHT = 40.dp
private const val RULES_SCRIM_ALPHA = 0.65f

@Composable
internal fun PlayHintCard(
    message: PlayHintMessage,
    onClose: () -> Unit,
    maxHeight: Dp,
    expandedMaxHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val metrics = LocalPlayMetrics.current
    var expanded by remember(message) { mutableStateOf(false) }
    var bodyCut by remember(message) { mutableStateOf(false) }
    val canExpand = expandedMaxHeight > maxHeight && (expanded || bodyCut)
    val heightLimit = (if (expanded) expandedMaxHeight else maxHeight).coerceAtLeast(HINT_CARD_MIN_HEIGHT)
    val bodyLineHeight = metrics.hintBodySize * 1.4f
    val bodyStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = metrics.hintBodySize, lineHeight = bodyLineHeight)
    val bodyModifier = Modifier.padding(start = 26.dp, end = 10.dp, bottom = HINT_BODY_BOTTOM_PADDING)
    Surface(
        modifier = modifier
            .widthIn(max = OVERLAY_CARD_MAX_WIDTH)
            .fillMaxWidth()
            .testTag(PLAY_HINT_TAG),
        shape = HINT_CARD_SHAPE,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 6.dp,
    ) {
        HintCardLayout(
            contentHeightLimit = heightLimit - HINT_CARD_VERTICAL_INSET * 2,
            bodyLineHeight = bodyLineHeight,
            modifier = (if (canExpand) Modifier.clickable { expanded = !expanded } else Modifier)
                .padding(start = 12.dp, end = 2.dp, top = HINT_CARD_VERTICAL_INSET, bottom = HINT_CARD_VERTICAL_INSET),
            header = {
                Row(Modifier.fillMaxWidth().heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        message.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = metrics.hintTitleSize, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                    )
                    if (canExpand) {
                        Icon(
                            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.common_close),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            body = { maxLines ->
                when {
                    expanded -> Text(
                        message.body,
                        style = bodyStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.verticalScroll(rememberScrollState()).then(bodyModifier),
                    )
                    maxLines > 0 -> Text(
                        message.body,
                        style = bodyStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = maxLines,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { bodyCut = it.hasVisualOverflow },
                        modifier = bodyModifier,
                    )
                    else -> SideEffect { bodyCut = message.body.isNotEmpty() }
                }
            },
        )
    }
}

@Composable
private fun HintCardLayout(
    contentHeightLimit: Dp,
    bodyLineHeight: TextUnit,
    modifier: Modifier,
    header: @Composable () -> Unit,
    body: @Composable (maxLines: Int) -> Unit,
) {
    SubcomposeLayout(modifier) { constraints ->
        val childConstraints = constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
        val headerPlaceables = subcompose(HintCardPart.Header, header).map { it.measure(childConstraints) }
        val headerHeight = headerPlaceables.maxOfOrNull { it.height } ?: 0
        val bodySpace = (contentHeightLimit.roundToPx() - headerHeight).coerceAtLeast(0)
        val bodyLines = ((bodySpace - HINT_BODY_BOTTOM_PADDING.roundToPx()) / bodyLineHeight.toPx()).toInt().coerceAtLeast(0)
        val bodyPlaceables = subcompose(HintCardPart.Body) { body(bodyLines) }
            .map { it.measure(childConstraints.copy(maxHeight = bodySpace)) }
        val bodyHeight = bodyPlaceables.maxOfOrNull { it.height } ?: 0
        val width = constraints.constrainWidth((headerPlaceables + bodyPlaceables).maxOfOrNull { it.width } ?: 0)
        val height = constraints.constrainHeight(headerHeight + bodyHeight)
        layout(width, height) {
            headerPlaceables.forEach { it.place(0, 0) }
            bodyPlaceables.forEach { it.place(0, headerHeight) }
        }
    }
}

private enum class HintCardPart { Header, Body }

@Composable
internal fun RulesPeekOverlay(
    gameId: GameId,
    cardAlignment: Alignment,
    onDismiss: () -> Unit,
    onFullRules: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = RULES_SCRIM_ALPHA))
            .pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        RulesPeekCard(quickStartFor(gameId), onFullRules, Modifier.align(cardAlignment))
    }
}

@Composable
private fun RulesPeekCard(content: QuickStartContent, onFullRules: (() -> Unit)?, modifier: Modifier) {
    Surface(
        modifier = modifier.widthIn(max = OVERLAY_CARD_MAX_WIDTH).fillMaxWidth().testTag(PLAY_RULES_TAG),
        shape = OVERLAY_CARD_SHAPE,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp)) {
            if (content.rules.isNotEmpty()) {
                QuickRulesRow(content.rules)
            } else {
                QuickRulesText(content)
            }
            if (onFullRules != null) {
                HorizontalDivider(Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                TextButton(onClick = onFullRules, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.play_full_rules), fontWeight = FontWeight.SemiBold)
                }
            } else {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun QuickRulesRow(rules: List<QuickRule>) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        rules.forEachIndexed { index, rule ->
            if (index > 0) VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                Modifier.weight(1f).padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.height(QUICK_RULE_PICTURE_HEIGHT), contentAlignment = Alignment.Center) {
                    rule.visual()
                }
                Text(
                    stringResource(rule.titleRes),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun QuickRulesText(content: QuickStartContent) {
    Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(content.goalRes), style = MaterialTheme.typography.titleSmall)
        content.bulletRes.forEach { bulletRes ->
            Row {
                Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(bulletRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        Text(
            stringResource(content.hookRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
