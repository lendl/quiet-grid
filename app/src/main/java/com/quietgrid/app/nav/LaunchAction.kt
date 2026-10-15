package com.quietgrid.app.nav

import com.quietgrid.app.LaunchRequest
import com.quietgrid.app.data.ActiveSessionSummary

sealed interface LaunchAction {
    data object OpenDailyTab : LaunchAction
    data object OpenMixesTab : LaunchAction
    data object OpenTabs : LaunchAction
    data object Stay : LaunchAction
    data class Navigate(val route: String, val endsMix: Boolean) : LaunchAction
    data class StartMix(val mixId: String) : LaunchAction
}

fun launchAction(
    request: LaunchRequest,
    activeSession: ActiveSessionSummary?,
    existingMixIds: Set<String>,
    onPlayScreen: Boolean,
): LaunchAction = when (request) {
    LaunchRequest.DailyTab -> LaunchAction.OpenDailyTab
    LaunchRequest.OpenMixes -> LaunchAction.OpenMixesTab
    LaunchRequest.Resume -> if (onPlayScreen) {
        LaunchAction.Stay
    } else {
        activeSession?.let { Routes.resume(it) }?.let { LaunchAction.Navigate(it, endsMix = false) } ?: LaunchAction.OpenTabs
    }
    is LaunchRequest.StartGame -> LaunchAction.Navigate(Routes.play(request.gameId, request.difficulty, resume = false), endsMix = true)
    is LaunchRequest.StartMix -> if (request.mixId in existingMixIds) LaunchAction.StartMix(request.mixId) else LaunchAction.OpenTabs
}
