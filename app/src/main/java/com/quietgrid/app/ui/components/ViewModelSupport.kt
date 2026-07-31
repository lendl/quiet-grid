package com.quietgrid.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharedFlow

@Composable
inline fun <reified VM : ViewModel> rememberPuzzleViewModel(crossinline create: () -> VM): VM =
    viewModel(factory = viewModelFactory { initializer { create() } })

@Composable
fun <T> CollectPuzzleResult(flow: SharedFlow<T>, onFinished: (T) -> Unit) {
    LaunchedEffect(flow) {
        flow.collect { result -> onFinished(result) }
    }
}
