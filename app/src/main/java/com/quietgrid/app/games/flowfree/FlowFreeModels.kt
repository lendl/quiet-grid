package com.quietgrid.app.games.flowfree

import com.quietgrid.engine.flowfree.FlowFreePuzzleEntry
import kotlinx.serialization.Serializable

data class FlowFreeSession(
    val puzzle: FlowFreePuzzleEntry,
    val paths: Map<Int, List<Pair<Int, Int>>>,
    val activeColor: Int? = null,
)

@Serializable
data class FlowFreePersistedSession(
    val puzzle: FlowFreePuzzleEntry,
    val paths: Map<Int, List<Pair<Int, Int>>>,
)

fun createFlowFreeSession(puzzle: FlowFreePuzzleEntry): FlowFreeSession =
    FlowFreeSession(puzzle = puzzle, paths = emptyMap())

fun flowFreeHasMeaningfulProgress(session: FlowFreeSession): Boolean =
    session.paths.values.any { it.size > 1 }
