package com.quietgrid.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun <T> CollectPuzzleResult(flow: SharedFlow<T>, onFinished: (T) -> Unit) {
    LaunchedEffect(flow) {
        flow.collect { result -> onFinished(result) }
    }
}
