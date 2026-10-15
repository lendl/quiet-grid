package com.quietgrid.app.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.quietgrid.app.LaunchRequest

@Composable
fun LaunchRequestEffect(launchRequest: LaunchRequest?, onHandled: () -> Unit, handle: suspend (LaunchRequest) -> Unit) {
    LaunchedEffect(launchRequest) {
        val request = launchRequest ?: return@LaunchedEffect
        handle(request)
        onHandled()
    }
}
