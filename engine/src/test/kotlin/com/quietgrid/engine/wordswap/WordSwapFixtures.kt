package com.quietgrid.engine.wordswap

internal fun wordSwapFixture(name: String, size: Int, solution: String, start: String): WordSwapPuzzleEntry =
    WordSwapPuzzleEntry(
        id = "ws-test-$name",
        difficulty = "easy",
        rows = size,
        cols = size,
        solution = solution,
        start = start,
        slots = wordSwapLineSlots(solution, size, size),
        minSwaps = wordSwapMinSwaps(start, solution),
    )

internal val GRID3 = wordSwapFixture("grid3", 3, "abcdefghi", "bcaefdhig")

internal val SPLIT4 = wordSwapFixture("split4", 4, "xxq." + "ac.y" + "bd.e" + "...f", "xxq." + "ac.y" + "bd.e" + "...f")

internal val LATIN3 = wordSwapFixture("latin3", 3, "abccabbca", "acbbaccba")
