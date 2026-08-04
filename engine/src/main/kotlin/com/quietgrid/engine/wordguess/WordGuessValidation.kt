package com.quietgrid.engine.wordguess

fun isValidGuess(word: String, dictionary: Set<String>): Boolean = dictionary.contains(word.lowercase())
