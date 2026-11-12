package com.quietgrid.app.session

enum class LossReason(val key: String) {
    ABANDONED("abandoned"),
    RULE_FAILURE("rule-failure"),
    NO_MOVES_LEFT("no-moves-left"),
    OUT_OF_LIVES("out_of_lives"),
    HEARTS_EXHAUSTED("hearts_exhausted"),
    ;

    companion object {
        fun fromKey(key: String?): LossReason? = entries.firstOrNull { it.key == key }
    }
}
