package com.quietgrid.cli.wordguess

import org.apache.lucene.analysis.hunspell.Dictionary
import org.apache.lucene.analysis.hunspell.WordFormGenerator
import org.apache.lucene.store.ByteBuffersDirectory

private val cache = mutableMapOf<String, List<String>>()

private val unescapedSlash = Regex("(?<!\\\\)/")

val WORDGUESS_COMMON_NOUN_FLAGS: Map<String, String> = mapOf("de" to "m")

private fun flagAliases(affText: String): List<String> =
    Regex("(?m)^AF\\s+(\\S+)").findAll(affText).map { it.groupValues[1] }.drop(1).toList()

private fun compoundPositionFlags(affText: String): Set<String> {
    val flagWidth = if (Regex("(?m)^FLAG\\s+long").containsMatchIn(affText)) 2 else 1
    return Regex("(?m)^COMPOUND(BEGIN|MIDDLE|END)\\s+(\\S+)")
        .findAll(affText)
        .map { it.groupValues[2] }
        .flatMap { it.chunked(flagWidth) }
        .toSet()
}

private fun forEachHunspellParadigm(locale: String, action: (flags: List<String>, forms: List<String>) -> Unit) {
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
    val flagAliases = flagAliases(affText)

    dicBytes.inputStream().bufferedReader(Charsets.UTF_8).useLines { lines ->
        lines.drop(1).forEach { rawLine ->
            val line = rawLine.substringBefore('\t')
            if (line.isBlank()) return@forEach

            val slash = unescapedSlash.find(line)
            val stem = if (slash == null) line else line.substring(0, slash.range.first)
            val rawFlags = if (slash == null) "" else line.substring(slash.range.last + 1).trim()
            val flags = if (flagAliases.isEmpty()) rawFlags else rawFlags.toIntOrNull()?.let { flagAliases[it - 1] } ?: rawFlags
            val strippedFlags = if (compoundFlags.isEmpty() || flags.isEmpty()) {
                flags
            } else {
                flags.chunked(flagWidth).filterNot { it in compoundFlags }.joinToString("")
            }

            val forms = generator.getAllWordForms(stem, strippedFlags, checkCanceled)
                .map { it.word.replace("\u0133", "ij").replace("\u0132", "IJ") }
            action(flags.chunked(flagWidth), forms)
        }
    }
}

fun loadWordGuessHunspellWords(locale: String, commonNounFlag: String? = WORDGUESS_COMMON_NOUN_FLAGS[locale]): List<String> {
    cache[locale]?.let { return it }
    val words = mutableListOf<String>()
    forEachHunspellParadigm(locale) { flags, forms ->
        val isCommonNoun = commonNounFlag != null && commonNounFlag in flags
        forms.forEach { word ->
            if (word.none { it.isUpperCase() }) {
                words.add(word)
            } else if (isCommonNoun && word.first().isUpperCase() && word.drop(1).none { it.isUpperCase() }) {
                words.add(word.lowercase())
            }
        }
    }
    val result = words.distinct()
    cache[locale] = result
    return result
}

fun hunspellParadigmsContaining(locale: String, words: Set<String>): Set<String> {
    val result = HashSet<String>()
    forEachHunspellParadigm(locale) { _, forms ->
        val lowered = forms.map { it.lowercase() }
        if (lowered.any { it in words }) result += lowered
    }
    return result
}
