package com.quietgrid.app.ui.screens

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.quietgrid.app.core.GameId
import com.quietgrid.app.nav.LocalAnimatedVisibilityScope
import com.quietgrid.app.nav.LocalSharedTransitionScope
import com.quietgrid.app.ui.components.SharedElementKeys
import com.quietgrid.app.ui.play.PlaySizeClass
import com.quietgrid.app.ui.play.playSizeClass

class ResultLink(val labelRes: Int, val onClick: () -> Unit)

fun isSidewaysPhoneResult(width: Dp, height: Dp): Boolean =
    playSizeClass(width, height) == PlaySizeClass.PhoneLandscape

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ResultMedallion(
    gameId: GameId,
    symbol: String,
    tint: Color,
    size: Dp,
    symbolSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val sharedModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = SharedElementKeys.gameIdentity(gameId)),
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    } else {
        Modifier
    }
    Box(
        sharedModifier
            .size(size)
            .then(modifier)
            .background(tint.copy(alpha = 0.12f), CircleShape)
            .border(1.dp, tint.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontSize = symbolSize)
    }
}

@Composable
fun CompactResultActions(
    links: List<ResultLink>,
    modifier: Modifier = Modifier,
    primary: @Composable () -> Unit,
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ResultLinkGroup(links.dropLast(1))
        primary()
        ResultLinkGroup(links.takeLast(1))
    }
}

@Composable
fun ResultLinkGroup(links: List<ResultLink>) {
    if (links.isEmpty()) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        links.forEachIndexed { index, link ->
            if (index > 0) {
                Box(Modifier.width(1.dp).height(16.dp).background(MaterialTheme.colorScheme.outlineVariant))
            }
            TextButton(onClick = link.onClick) {
                Text(stringResource(link.labelRes), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ResultMetaDivider(height: Dp = 28.dp) {
    Box(Modifier.width(1.dp).height(height).background(MaterialTheme.colorScheme.outlineVariant))
}
