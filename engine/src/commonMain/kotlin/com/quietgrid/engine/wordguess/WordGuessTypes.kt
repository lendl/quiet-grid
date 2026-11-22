package com.quietgrid.engine.wordguess

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

@Serializable
data class WordGuessPuzzleEntry(
    val id: String,
    val locale: String,
    val difficulty: String,
    val word: String,
)

val wordGuessDictionarySerializer: KSerializer<Map<String, List<String>>> =
    MapSerializer(String.serializer(), ListSerializer(String.serializer()))
