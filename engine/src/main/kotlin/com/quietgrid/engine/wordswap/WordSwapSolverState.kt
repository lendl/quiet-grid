package com.quietgrid.engine.wordswap

private const val UNKNOWN = '\u0000'

data class WordSwapCrossing(val slot: Int, val position: Int)

class WordSwapSolverState private constructor(
    val puzzle: WordSwapPuzzleEntry,
    private val words: List<String>,
    private val slotCells: List<List<Int>>,
    private val cellCrossings: Map<Int, List<WordSwapCrossing>>,
    private val domains: List<MutableSet<Int>>,
    private val placed: IntArray,
    private val used: BooleanArray,
    private val letters: CharArray,
) {
    val slotCount: Int get() = placed.size

    fun word(index: Int): String = words[index]

    fun cellsOf(slot: Int): List<Int> = slotCells[slot]

    fun crossingsAt(cell: Int): List<WordSwapCrossing> = cellCrossings[cell].orEmpty()

    fun isPlaced(slot: Int): Boolean = placed[slot] >= 0

    fun openSlots(): List<Int> = placed.indices.filter { placed[it] < 0 }

    fun unplacedWords(): List<Int> = words.indices.filter { !used[it] }

    fun sameShape(slot: Int, word: Int): Boolean {
        val a = puzzle.slots[slot]
        val b = puzzle.slots[word]
        return a.direction == b.direction && a.length == b.length
    }

    fun fits(slot: Int, word: Int): Boolean {
        val text = words[word]
        val cells = slotCells[slot]
        return cells.indices.all { i -> letters[cells[i]] == UNKNOWN || letters[cells[i]] == text[i] }
    }

    fun fittingCandidates(slot: Int): List<Int> = domains[slot].filter { !used[it] && fits(slot, it) }

    fun place(slot: Int, word: Int) {
        placed[slot] = word
        used[word] = true
        val text = words[word]
        slotCells[slot].forEachIndexed { i, cell -> letters[cell] = text[i] }
        domains.forEachIndexed { other, domain -> if (other != slot) domain.remove(word) }
    }

    fun eliminate(slot: Int, word: Int) {
        domains[slot].remove(word)
    }

    fun isFeasible(): Boolean {
        val open = openSlots()
        val fitting = open.associateWith { fittingCandidates(it) }
        if (fitting.values.any { it.isEmpty() }) return false
        return unplacedWords().all { word -> fitting.values.any { word in it } }
    }

    fun isSolved(): Boolean = placed.all { it >= 0 }

    fun copy(): WordSwapSolverState = WordSwapSolverState(
        puzzle = puzzle,
        words = words,
        slotCells = slotCells,
        cellCrossings = cellCrossings,
        domains = domains.map { LinkedHashSet(it) },
        placed = placed.copyOf(),
        used = used.copyOf(),
        letters = letters.copyOf(),
    )

    companion object {
        fun of(puzzle: WordSwapPuzzleEntry): WordSwapSolverState {
            val slotCells = puzzle.slots.map { it.cells(puzzle.cols) }
            val crossings = HashMap<Int, MutableList<WordSwapCrossing>>()
            slotCells.forEachIndexed { slot, cells ->
                cells.forEachIndexed { position, cell -> crossings.getOrPut(cell) { mutableListOf() } += WordSwapCrossing(slot, position) }
            }
            val domains = puzzle.slots.indices.map { slot ->
                puzzle.slots.indices.filterTo(LinkedHashSet()) { word ->
                    puzzle.slots[word].direction == puzzle.slots[slot].direction && puzzle.slots[word].length == puzzle.slots[slot].length
                }
            }
            return WordSwapSolverState(
                puzzle = puzzle,
                words = puzzle.words(),
                slotCells = slotCells,
                cellCrossings = crossings,
                domains = domains,
                placed = IntArray(puzzle.slots.size) { -1 },
                used = BooleanArray(puzzle.slots.size),
                letters = CharArray(puzzle.solution.length) { UNKNOWN },
            )
        }
    }
}
