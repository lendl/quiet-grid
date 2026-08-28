package com.quietgrid.cli.wordguess

import org.apache.lucene.analysis.hunspell.Dictionary
import org.apache.lucene.analysis.hunspell.WordFormGenerator
import org.apache.lucene.store.ByteBuffersDirectory

private val cache = mutableMapOf<String, List<String>>()

fun loadWordGuessHunspellWords(locale: String): List<String> {
    cache[locale]?.let { return it }
    val classLoader = object {}.javaClass.classLoader

    val dictionary = classLoader.getResourceAsStream("wordguess_hunspell_$locale.aff").use { affStream ->
        classLoader.getResourceAsStream("wordguess_hunspell_$locale.dic").use { dicStream ->
            requireNotNull(affStream) { "wordguess_hunspell_$locale.aff not found on classpath" }
            requireNotNull(dicStream) { "wordguess_hunspell_$locale.dic not found on classpath" }
            Dictionary(ByteBuffersDirectory(), "wordguess-$locale", affStream, dicStream)
        }
    }

    val words = mutableListOf<String>()
    WordFormGenerator(dictionary).generateAllSimpleWords(
        { affixedWord ->
            val word = affixedWord.word
            if (word.none { it.isUpperCase() }) {
                words.add(word)
            }
        },
        Runnable {},
    )

    val result = words.distinct()
    cache[locale] = result
    return result
}
