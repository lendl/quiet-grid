package com.quietgrid.app.core

import androidx.annotation.Keep

@Keep
enum class Difficulty(val key: String) {
    EASY("easy"),
    MEDIUM("medium"),
    HARD("hard"),
    EXPERT("expert");

    companion object {
        fun fromKey(key: String): Difficulty = entries.first { it.key == key }
    }
}
