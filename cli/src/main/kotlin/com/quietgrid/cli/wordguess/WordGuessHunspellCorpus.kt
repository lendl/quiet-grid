package com.quietgrid.cli.wordguess

import org.apache.lucene.analysis.hunspell.Dictionary
import org.apache.lucene.analysis.hunspell.WordFormGenerator
import org.apache.lucene.store.ByteBuffersDirectory

private val cache = mutableMapOf<String, List<String>>()

private val unescapedSlash = Regex("(?<!\\\\)/")

private fun compoundPositionFlags(affText: String): Set<String> {
    val flagWidth = if (Regex("(?m)^FLAG\\s+long").containsMatchIn(affText)) 2 else 1
    return Regex("(?m)^COMPOUND(BEGIN|MIDDLE|END)\\s+(\\S+)")
        .findAll(affText)
        .map { it.groupValues[2] }
        .flatMap { it.chunked(flagWidth) }
        .toSet()
}

fun loadWordGuessHunspellWords(locale: String): List<String> {
    cache[locale]?.let { return it }
    val classLoader = object {}.javaClass.classLoader

    val affBytes = classLoader.getResourceAsStream("wordguess_hunspell_$locale.aff")
        .use { requireNotNull(it) { "wordguess_hunspell_$locale.aff not found on classpath" }.readBytes() }
    val dicBytes = classLoader.getResourceAsStream("wordguess_hunspell_$locale.dic")
        .use { requireNotNull(it) { "wordguess_hunspell_$locale.dic not found on classpath" }.readBytes() }

    val dictionary = Dictionary(ByteBuffersDirectory(), "wordguess-$locale", affBytes.inputStream(), dicBytes.inputStream())
    val generator = WordFormGenerator(dictionary)
    val checkCanceled = Runnable {}

    val affText = affBytes.toString(Charsets.UTF_8)
    val flagWidth = if (Regex("(?m)^FLAG\\s+long").containsMatchIn(affText)) 2 else 1
    val compoundFlags = compoundPositionFlags(affText)

    val words = mutableListOf<String>()
    dicBytes.inputStream().bufferedReader(Charsets.UTF_8).useLines { lines ->
        lines.drop(1).forEach { rawLine ->
            val line = rawLine.substringBefore('\t')
            if (line.isBlank()) return@forEach

            val slash = unescapedSlash.find(line)
            val stem = if (slash == null) line else line.substring(0, slash.range.first)
            val flags = if (slash == null) "" else line.substring(slash.range.last + 1)
            val strippedFlags = if (compoundFlags.isEmpty() || flags.isEmpty()) {
                flags
            } else {
                flags.chunked(flagWidth).filterNot { it in compoundFlags }.joinToString("")
            }

            for (affixedWord in generator.getAllWordForms(stem, strippedFlags, checkCanceled)) {
                val word = affixedWord.word
                if (word.none { it.isUpperCase() }) {
                    words.add(word)
                }
            }
        }
    }

    val result = words.distinct()
    cache[locale] = result
    return result
}
