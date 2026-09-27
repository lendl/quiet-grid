package com.quietgrid.engine.themeclear

class ThemeClearDictionary(rawWords: Collection<String>) {
    val words: Set<String> = rawWords
        .map(::normalizeThemeClearWord)
        .filter { it.length >= THEMECLEAR_MIN_WORD_LENGTH }
        .toSet()

    private val prefixes: Set<String> = words.flatMapTo(HashSet()) { word -> (1..word.length).map { word.substring(0, it) } }

    fun contains(word: String): Boolean = word in words

    fun hasPrefix(prefix: String): Boolean = prefix in prefixes

    fun canExtend(selection: String, remainingLetters: String): Boolean {
        val counts = themeClearLetterCounts(remainingLetters)
        return words.any { word ->
            word.length > selection.length &&
                word.startsWith(selection) &&
                themeClearCanBuild(word.substring(selection.length), counts)
        }
    }

    fun buildableWords(letters: String): List<String> {
        val counts = themeClearLetterCounts(letters)
        return words.filter { themeClearCanBuild(it, counts) }.sorted()
    }
}
