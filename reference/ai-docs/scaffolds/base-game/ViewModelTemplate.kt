package com.quietgrid.app.games.__gameId__

import kotlinx.serialization.Serializable

// __GameName__ViewModel.kt template — session state, persistence, win/loss transition.
// Follow an existing ViewModel (e.g. TakuzuViewModel.kt) for the real StateFlow/repository plumbing;
// this template only marks the required shape and responsibilities.

@Serializable
data class __GameName__PersistedSession(
    val puzzleId: String,
    val difficulty: String,
    val score: Int,
    val mistakes: Int,
    // __PERSISTED_BOARD_STATE__ — must stay decodable-or-fail-gracefully; SessionRepository treats
    // decode failures as "start fresh" (see reference/ai-docs/context/context-maps.md, Persistence section).
)

private const val FINISH_TRANSITION_DELAY_MS = 450L

class __GameName__ViewModel(/* __DI_ARGS__ e.g. SessionRepository, StatsRepository */) /* : ViewModel() */ {

    // __ACTIVE_STATE__ — expose __GameName__ActiveState as a StateFlow, restoreOrCreate() on init.

    fun onMove(/* __MOVE_ARGS__ */) {
        TODO("__APPLY_MOVE__ — validate via LogicTemplate.isValidMove, update mistakes via detectMistake")
    }

    suspend fun finishAsWin() {
        TODO("delay(FINISH_TRANSITION_DELAY_MS) before emitting the win result — see " +
            "reference/ai-docs/context/context-maps.md Feedback effects section")
    }

    suspend fun finishAsLoss() {
        TODO("same FINISH_TRANSITION_DELAY_MS pattern as finishAsWin(), separate loss condition from " +
            "mistake policy — see reference/ai-docs/context/mistake-policy.md")
    }
}
