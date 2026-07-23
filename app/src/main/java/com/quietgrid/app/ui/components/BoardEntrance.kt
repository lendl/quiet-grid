package com.quietgrid.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Wraps a freshly-started puzzle board with a one-shot fade+scale entrance.
 * When [playFresh] is false (resuming an in-progress puzzle), the content
 * appears immediately with no animation so resuming feels instant.
 */
@Composable
fun BoardEntrance(playFresh: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val visibleState = remember { MutableTransitionState(!playFresh) }
    LaunchedEffect(Unit) {
        if (playFresh) visibleState.targetState = true
    }

    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300)),
    ) {
        content()
    }
}
