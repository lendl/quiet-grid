package com.quietgrid.engine.__game__

// __Game__Moves.kt template — canonical move detection. This is the single source of truth reused by:
//   - difficulty classification (__Game__Difficulty.kt)
//   - the CLI generator's solve-path check (cli/.../__game__/__Game__Generator.kt)
//   - the app's optional next-move hint surface (app/.../games/__game__/__Game__NextMove.kt), if any
// Do not let those three reimplement move detection separately — see reference/ai-docs/context/moves.md.

sealed interface __Game__Move {
    // __CANONICAL_MOVE_VARIANTS__ e.g. data class FindPair(...), data class AvoidTrio(...)
}

fun findNextMove(grid: __Game__Grid): __Game__Move? {
    TODO("__DEFINE_MOVE_DETECTION__ — try recovery/repair techniques before progress techniques")
}
