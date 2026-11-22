package com.quietgrid.engine.themeclear

fun themeClearLetterCounts(letters: CharSequence): IntArray {
    val counts = IntArray(26)
    for (letter in letters) counts[letter - 'A']++
    return counts
}

fun themeClearCanBuild(word: CharSequence, counts: IntArray): Boolean {
    val needed = themeClearLetterCounts(word)
    return needed.indices.all { needed[it] <= counts[it] }
}
