package com.quietgrid.engine.animaldoku

sealed interface AnimalDokuHint {
    data class WrongMark(val row: Int, val col: Int) : AnimalDokuHint
    data class Deduction(val step: AnimalDokuStep, val openCells: Set<Pair<Int, Int>>) : AnimalDokuHint
    data class Reveal(val row: Int, val col: Int) : AnimalDokuHint
}

val AnimalDokuStep.sourceUnits: List<AnimalDokuUnit>
    get() = when (this) {
        is AnimalDokuStep.Placement -> listOf(unit)
        is AnimalDokuStep.Elimination -> sources
    }

fun findAnimalDokuHint(
    puzzle: AnimalDokuPuzzleEntry,
    placed: Set<Pair<Int, Int>>,
    excluded: Set<Pair<Int, Int>>,
    maxPairingK: Int = 4,
): AnimalDokuHint? {
    val size = puzzle.size
    val wrongMark = (0 until size).map { row -> row to puzzle.solution[row] }.firstOrNull { it in excluded && it !in placed }
    if (wrongMark != null) return AnimalDokuHint.WrongMark(wrongMark.first, wrongMark.second)

    val correct = placed.filter { (row, col) -> puzzle.solution[row] == col }
    if (correct.size == size) return null

    val state = AnimalDokuSolverState(size, puzzle.regions)
    correct.forEach { (row, col) -> state.place(row, col) }
    excluded.forEach { (row, col) -> state.eliminated[row][col] = true }

    val step = nextAnimalDokuStep(state, maxPairingK)
    if (step != null) return AnimalDokuHint.Deduction(step, step.sourceUnits.flatMap { state.candidatesIn(it) }.toSet())

    val placedRows = correct.map { it.first }.toSet()
    val row = (0 until size).first { it !in placedRows }
    return AnimalDokuHint.Reveal(row, puzzle.solution[row])
}
