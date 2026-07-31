package com.quietgrid.engine.__game__

// __Game__Types.kt template — shared value types for the engine module. Pure Kotlin/JVM, no Android deps
// (engine/ must stay usable from both :app and :cli).

data class __Game__Grid(
    val size: Int,
    // __CELL_STORAGE__
)

enum class __Game__Difficulty { EASY, MEDIUM, HARD, EXPERT }
