package com.quietgrid.engine.circuit

import kotlin.random.Random

internal fun randomSpanningTree(board: CircuitBoard, random: Random): IntArray? {
    val masks = IntArray(board.size)
    val visited = BooleanArray(board.size)
    val stack = ArrayDeque(listOf(0))
    visited[0] = true
    while (stack.isNotEmpty()) {
        val cell = stack.last()
        val next = CIRCUIT_DIRECTIONS.filter { direction ->
            val other = board.neighbor(cell, direction)
            other >= 0 && !visited[other] && circuitArmCount(masks[cell]) < 3
        }
        if (next.isEmpty()) {
            stack.removeLast()
            continue
        }
        val direction = next[random.nextInt(next.size)]
        val other = board.neighbor(cell, direction)
        masks[cell] = masks[cell] or direction
        masks[other] = masks[other] or oppositeCircuitDirection(direction)
        visited[other] = true
        stack.addLast(other)
    }
    return masks.takeIf { visited.all { it } }
}

internal fun generateLogicCompleteTree(board: CircuitBoard, random: Random): IntArray {
    while (true) {
        val tree = randomSpanningTree(board, random) ?: continue
        if (solveCircuit(board, tree).solved) return tree
    }
}
