package com.quietgrid.app.games.__gameId__

// __GameName__Logic.kt template — rules, board validation, canonical moves, mistake checks.
// If the game is engine-backed, delegate to engine.__gameId__.* instead of reimplementing rules here
// (see reference/ai-docs/context/moves.md — do not duplicate engine move logic in the app layer).

fun isValidMove(state: __GameName__ActiveState, /* __MOVE_ARGS__ */): Boolean {
    TODO("__DEFINE_MOVE_VALIDATION__")
}

fun detectMistake(state: __GameName__ActiveState): Boolean {
    TODO("__DEFINE_MISTAKE_POLICY__ — see reference/ai-docs/context/mistake-policy.md")
}

fun isSolved(state: __GameName__ActiveState): Boolean {
    TODO("__DEFINE_WIN_CONDITION__")
}
