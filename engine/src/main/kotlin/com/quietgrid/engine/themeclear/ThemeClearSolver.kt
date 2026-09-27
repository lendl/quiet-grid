package com.quietgrid.engine.themeclear

import kotlin.random.Random

const val THEMECLEAR_BLIND_ROLLOUTS = 200

fun themeClearLetterSeed(letters: String): Int = letters.toCharArray().sorted().joinToString("").hashCode()

class ThemeClearSolver(val dictionary: ThemeClearDictionary) {
    private class WordNeed(val word: String, val counts: IntArray)

    private val needs: List<WordNeed> = dictionary.words.sorted().map { WordNeed(it, themeClearLetterCounts(it)) }
    private val needsByLetter: Array<List<WordNeed>> = Array(26) { letter -> needs.filter { it.counts[letter] > 0 } }

    fun findFinish(letters: String): List<String>? {
        val path = ArrayList<String>()
        return if (search(themeClearLetterCounts(letters), HashSet(), path)) path.toList() else null
    }

    fun hintWord(letters: String): String? =
        findFinish(letters)?.firstOrNull()
            ?: dictionary.buildableWords(letters).maxWithOrNull(compareBy<String> { it.length }.thenByDescending { it })

    fun countFinishes(letters: String, cap: Int): Int {
        val found = HashSet<List<String>>()
        collect(themeClearLetterCounts(letters), ArrayList(), found, HashSet(), cap)
        return found.size
    }

    fun analyze(letters: String, finishCap: Int = THEMECLEAR_FINISH_CAP): ThemeClearMetrics {
        val counts = themeClearLetterCounts(letters)
        val dead = HashSet<String>()
        val spellable = needs.filter { fits(it.counts, counts) }
        val traps = spellable.count { need ->
            val rest = counts.copyOf()
            subtract(rest, need.counts)
            !search(rest, dead, ArrayList())
        }
        return ThemeClearMetrics(
            finishCount = countFinishes(letters, finishCap),
            trapWords = traps,
            spellableWords = spellable.size,
            blindSuccessRate = blindSuccessRate(letters),
        )
    }

    fun blindSuccessRate(
        letters: String,
        rollouts: Int = THEMECLEAR_BLIND_ROLLOUTS,
        random: Random = Random(themeClearLetterSeed(letters)),
    ): Double {
        val start = themeClearLetterCounts(letters)
        var successes = 0
        repeat(rollouts) {
            val counts = start.copyOf()
            while (true) {
                if (counts.all { it == 0 }) {
                    successes++
                    break
                }
                val options = needs.filter { fits(it.counts, counts) }
                if (options.isEmpty()) break
                subtract(counts, options.random(random).counts)
            }
        }
        return successes.toDouble() / rollouts
    }

    private fun search(counts: IntArray, dead: MutableSet<String>, path: MutableList<String>): Boolean {
        val letter = pickLetter(counts) ?: return true
        val key = stateKey(counts)
        if (key in dead) return false
        for (need in needsByLetter[letter]) {
            if (!fits(need.counts, counts)) continue
            subtract(counts, need.counts)
            path.add(need.word)
            if (search(counts, dead, path)) return true
            path.removeAt(path.lastIndex)
            add(counts, need.counts)
        }
        dead.add(key)
        return false
    }

    private fun collect(
        counts: IntArray,
        path: MutableList<String>,
        found: MutableSet<List<String>>,
        dead: MutableSet<String>,
        cap: Int,
    ): Boolean {
        if (found.size >= cap) return true
        val letter = pickLetter(counts)
        if (letter == null) {
            found.add(path.sorted())
            return true
        }
        val key = stateKey(counts)
        if (key in dead) return false
        var reachable = false
        for (need in needsByLetter[letter]) {
            if (!fits(need.counts, counts)) continue
            subtract(counts, need.counts)
            path.add(need.word)
            if (collect(counts, path, found, dead, cap)) reachable = true
            path.removeAt(path.lastIndex)
            add(counts, need.counts)
            if (found.size >= cap) return true
        }
        if (!reachable) dead.add(key)
        return reachable
    }

    private fun pickLetter(counts: IntArray): Int? {
        var best: Int? = null
        var bestOptions = Int.MAX_VALUE
        for (letter in 0 until 26) {
            if (counts[letter] == 0) continue
            val options = needsByLetter[letter].count { fits(it.counts, counts) }
            if (options < bestOptions) {
                best = letter
                bestOptions = options
            }
            if (options == 0) break
        }
        return best
    }

    private fun fits(need: IntArray, counts: IntArray): Boolean {
        for (i in 0 until 26) if (need[i] > counts[i]) return false
        return true
    }

    private fun subtract(counts: IntArray, need: IntArray) {
        for (i in 0 until 26) counts[i] -= need[i]
    }

    private fun add(counts: IntArray, need: IntArray) {
        for (i in 0 until 26) counts[i] += need[i]
    }

    private fun stateKey(counts: IntArray): String = String(CharArray(26) { ('0' + counts[it]) })
}
