package com.quietgrid.engine.core

enum class Difficulty {
    EASY, MEDIUM, HARD, EXPERT;

    val key: String get() = name.lowercase()
}
