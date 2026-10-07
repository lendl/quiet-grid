package com.quietgrid.engine.whodidit

data class WhoDidItPuzzleSpec(
    val size: Int,
    val categoryCount: Int,
    val traits: List<List<Set<WhoDidItTrait>>>,
    val clues: List<WhoDidItClue>,
    val statements: List<WhoDidItStatement>,
)

data class WhoDidItSolution(val slotOf: List<List<Int>>, val culprit: Int)

fun WhoDidItSolution.ownerOf(ref: WhoDidItRef): Int =
    if (ref.category == 0) ref.slot else slotOf[ref.category].indexOf(ref.slot)

fun WhoDidItPuzzleEntry.toSpec(): WhoDidItPuzzleSpec {
    val pools = whoDidItTheme(theme).pools
    val traits = categories.indices.map { category ->
        val pool = pools.getValue(categories[category])
        entries[category].map { pool[it] }
    }
    return WhoDidItPuzzleSpec(size, categories.size, traits, clues, statements)
}

fun WhoDidItPuzzleEntry.solutionValue(): WhoDidItSolution = WhoDidItSolution(solution, culprit)
