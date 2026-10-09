package com.quietgrid.app.nav

import com.quietgrid.app.LaunchRequest
import com.quietgrid.app.data.ActiveSessionSummary

sealed interface LaunchAction {
    data object OpenDailyTab : LaunchAction
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
): LaunchAction {
    if (request == LaunchRequest.DailyTab) return LaunchAction.OpenDailyTab
    if (onPlayScreen) return LaunchAction.Stay
    val resumeRoute = activeSession?.let { Routes.resume(it) }
    if (resumeRoute != null) return LaunchAction.Navigate(resumeRoute, endsMix = false)
    return when (request) {
        LaunchRequest.DailyTab, LaunchRequest.Resume -> LaunchAction.OpenTabs
        is LaunchRequest.StartGame -> LaunchAction.Navigate(Routes.play(request.gameId, request.difficulty, resume = false), endsMix = true)
        is LaunchRequest.StartMix -> if (request.mixId in existingMixIds) LaunchAction.StartMix(request.mixId) else LaunchAction.OpenTabs
    }
}
