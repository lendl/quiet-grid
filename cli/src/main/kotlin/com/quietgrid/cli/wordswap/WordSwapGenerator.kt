package com.quietgrid.cli.wordswap

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.wordswap.WORDSWAP_HOLE
import com.quietgrid.engine.wordswap.WordSwapDirection
import com.quietgrid.engine.wordswap.WordSwapPuzzleEntry
import com.quietgrid.engine.wordswap.WordSwapSlot
import com.quietgrid.engine.wordswap.cells
import com.quietgrid.engine.wordswap.countWordSwapArrangements
import com.quietgrid.engine.wordswap.gradeWordSwapPuzzle
import com.quietgrid.engine.wordswap.read
import com.quietgrid.engine.wordswap.wordSwapCoversAllTiles
import com.quietgrid.engine.wordswap.wordSwapDiagonalSlot
import com.quietgrid.engine.wordswap.wordSwapLineSlots
import com.quietgrid.engine.wordswap.wordSwapMinSwaps
import com.quietgrid.engine.wordswap.wordSwapTileCells
import kotlin.random.Random

data class WordSwapShape(val size: Int, val holes: IntRange, val diagonals: Int, val weight: Int = 1)

data class WordSwapTier(
    val shapes: List<WordSwapShape>,
    val vocabulary: Int,
    val inPlaceFraction: Double,
)

val WORDSWAP_TIERS: Map<Difficulty, WordSwapTier> = mapOf(
    Difficulty.EASY to WordSwapTier(
        listOf(WordSwapShape(3, 0..0, 0, weight = 1), WordSwapShape(4, 2..4, 0, weight = 3)),
        vocabulary = 2500,
        inPlaceFraction = 0.25,
    ),
    Difficulty.MEDIUM to WordSwapTier(
        listOf(WordSwapShape(5, 3..5, 0)),
        vocabulary = 5000,
        inPlaceFraction = 0.1,
    ),
    Difficulty.HARD to WordSwapTier(
        listOf(WordSwapShape(5, 1..3, 1, weight = 2), WordSwapShape(6, 5..8, 0)),
        vocabulary = 8000,
        inPlaceFraction = 0.0,
    ),
    Difficulty.EXPERT to WordSwapTier(
        listOf(WordSwapShape(6, 3..6, 1), WordSwapShape(6, 3..6, 0)),
        vocabulary = 10000,
        inPlaceFraction = 0.0,
    ),
)

private const val FILL_NODE_LIMIT = 40_000
private const val CANDIDATE_TRIES = 40

private val lexicons = HashMap<Int, WordSwapLexicon>()

private fun lexicon(vocabulary: Int): WordSwapLexicon = synchronized(lexicons) {
    lexicons.getOrPut(vocabulary) { WordSwapLexicon(loadWordSwapRankedWords().take(vocabulary)) }
}

fun generateWordSwapPuzzle(difficulty: Difficulty, random: Random = Random.Default): WordSwapPuzzleEntry? {
    val tier = WORDSWAP_TIERS.getValue(difficulty)
    val shape = pickShape(tier.shapes, random)
    val layout = randomLayout(shape, random) ?: return null
    val size = shape.size
    val slots = wordSwapLineSlots(layout, size, size) + diagonalSlots(layout, shape)
    if (!wordSwapCoversAllTiles(layout, size, slots)) return null
    val solution = fill(layout, size, slots, lexicon(tier.vocabulary), random) ?: return null
    val words = slots.map { it.read(solution, size) }
    if (words.toSet().size != words.size) return null
    if (countWordSwapArrangements(solution, size, slots, words) != 1) return null
    val start = scramble(solution, size, slots, tier.inPlaceFraction, random) ?: return null
    return WordSwapPuzzleEntry(
        id = "ws-${difficulty.key}-${Integer.toHexString(solution.hashCode())}",
        difficulty = difficulty.key,
        rows = size,
        cols = size,
        solution = solution,
        start = start,
        slots = slots,
        minSwaps = wordSwapMinSwaps(start, solution),
    ).takeIf { gradeWordSwapPuzzle(it) == difficulty }
}

private fun pickShape(shapes: List<WordSwapShape>, random: Random): WordSwapShape {
    var roll = random.nextInt(shapes.sumOf { it.weight })
    for (shape in shapes) {
        roll -= shape.weight
        if (roll < 0) return shape
    }
    return shapes.last()
}

private fun diagonalSlots(layout: String, shape: WordSwapShape): List<WordSwapSlot> = when (shape.diagonals) {
    0 -> emptyList()
    1 -> listOfNotNull(wordSwapDiagonalSlot(layout, shape.size, WordSwapDirection.DIAGONAL))
    else -> listOfNotNull(
        wordSwapDiagonalSlot(layout, shape.size, WordSwapDirection.DIAGONAL),
        wordSwapDiagonalSlot(layout, shape.size, WordSwapDirection.ANTI_DIAGONAL),
    )
}

private fun randomLayout(shape: WordSwapShape, random: Random): String? {
    val size = shape.size
    val cells = size * size
    val center = if (cells % 2 == 1) cells / 2 else null
    val picked = shape.holes.first + random.nextInt(shape.holes.last - shape.holes.first + 1)
    val target = if (picked % 2 == 1 && center == null) picked + 1 else picked
    repeat(200) {
        val holes = HashSet<Int>()
        if (target % 2 == 1) holes += center!!
        while (holes.size < target) {
            val cell = random.nextInt(cells)
            if (cell == center) continue
            holes += cell
            holes += cells - 1 - cell
        }
        if (holes.size != target) return@repeat
        val layout = String(CharArray(cells) { if (it in holes) WORDSWAP_HOLE else 'a' })
        if (!isConnected(layout, size)) return@repeat
        val slots = wordSwapLineSlots(layout, size, size)
        if (!wordSwapCoversAllTiles(layout, size, slots)) return@repeat
        if (diagonalSlots(layout, shape).size != shape.diagonals) return@repeat
        return layout
    }
    return null
}

private fun isConnected(layout: String, size: Int): Boolean {
    val tiles = wordSwapTileCells(layout)
    if (tiles.isEmpty()) return false
    val seen = HashSet<Int>()
    val stack = ArrayDeque(listOf(tiles.first()))
    while (stack.isNotEmpty()) {
        val cell = stack.removeLast()
        if (!seen.add(cell)) continue
        val r = cell / size
        val c = cell % size
        listOf(r - 1 to c, r + 1 to c, r to c - 1, r to c + 1).forEach { (nr, nc) ->
            if (nr in 0 until size && nc in 0 until size && layout[nr * size + nc] != WORDSWAP_HOLE) stack += nr * size + nc
        }
    }
    return seen.size == tiles.size
}

private fun fill(layout: String, cols: Int, slots: List<WordSwapSlot>, lexicon: WordSwapLexicon, random: Random): String? {
    val letters = arrayOfNulls<Char>(layout.length)
    val slotCells = slots.map { it.cells(cols) }
    val usedWords = HashSet<String>()
    var nodes = 0

    fun pattern(index: Int) = slotCells[index].map { letters[it] }

    fun solve(): Boolean {
        if (++nodes > FILL_NODE_LIMIT) return false
        var bestSlot = -1
        var bestCandidates: java.util.BitSet? = null
        for (i in slots.indices) {
            val p = pattern(i)
            val candidates = lexicon.candidates(p)
            if (candidates.isEmpty) return false
            if (p.all { it != null }) continue
            if (bestCandidates == null || candidates.cardinality() < bestCandidates.cardinality()) {
                bestSlot = i
                bestCandidates = candidates
            }
        }
        if (bestSlot == -1) {
            val filled = slots.indices.map { i -> pattern(i).joinToString("") }
            return filled.all { it in lexiconWords(lexicon, it.length) } && filled.toSet().size == filled.size
        }
        val words = lexicon.words(slots[bestSlot].length)
        val options = bestCandidates!!.stream().toArray().map { words[it] }.filter { it !in usedWords }.shuffled(random).take(CANDIDATE_TRIES)
        val cells = slotCells[bestSlot]
        for (word in options) {
            val added = cells.indices.filter { letters[cells[it]] == null }
            added.forEach { letters[cells[it]] = word[it] }
            usedWords += word
            if (solve()) return true
            usedWords -= word
            added.forEach { letters[cells[it]] = null }
            if (nodes > FILL_NODE_LIMIT) return false
        }
        return false
    }

    if (!solve()) return null
    return String(CharArray(layout.length) { i -> if (layout[i] == WORDSWAP_HOLE) WORDSWAP_HOLE else letters[i]!! })
}

private val lexiconSets = HashMap<Pair<WordSwapLexicon, Int>, Set<String>>()

private fun lexiconWords(lexicon: WordSwapLexicon, length: Int): Set<String> = synchronized(lexiconSets) {
    lexiconSets.getOrPut(lexicon to length) { lexicon.words(length).toHashSet() }
}

private fun scramble(solution: String, cols: Int, slots: List<WordSwapSlot>, inPlaceFraction: Double, random: Random): String? {
    val tiles = wordSwapTileCells(solution)
    val keep = (tiles.size * inPlaceFraction).toInt()
    repeat(500) {
        val fixed = tiles.shuffled(random).take(keep).toSet()
        val moving = tiles.filter { it !in fixed }
        val letters = moving.map { solution[it] }.shuffled(random)
        val chars = solution.toCharArray()
        moving.forEachIndexed { i, cell -> chars[cell] = letters[i] }
        val board = String(chars)
        val inPlace = tiles.count { board[it] == solution[it] }
        if (inPlace != keep) return@repeat
        if (slots.any { it.read(board, cols) == it.read(solution, cols) }) return@repeat
        return board
    }
    return null
}
