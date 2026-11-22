package com.quietgrid.engine.killersudoku

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.SudokuTechnique

enum class KillerTechnique(val classic: SudokuTechnique?, val tier: Difficulty, val baseScore: Int) {
    CAGE_LAST_CELL(null, Difficulty.EASY, 1),
    NAKED_SINGLE(SudokuTechnique.NAKED_SINGLE, Difficulty.EASY, 2),
    HIDDEN_SINGLE(SudokuTechnique.HIDDEN_SINGLE, Difficulty.EASY, 3),
    CAGE_COMBINATION(null, Difficulty.EASY, 4),
    INNIE_OUTIE(null, Difficulty.EASY, 5),
    NAKED_PAIR(SudokuTechnique.NAKED_PAIR, Difficulty.EASY, 6),
    CAGE_PERMUTATION(null, Difficulty.EASY, 8),
    HIDDEN_PAIR(SudokuTechnique.HIDDEN_PAIR, Difficulty.MEDIUM, 9),
    POINTING_PAIR_TRIPLE(SudokuTechnique.POINTING_PAIR_TRIPLE, Difficulty.MEDIUM, 10),
    BOX_LINE_REDUCTION(SudokuTechnique.BOX_LINE_REDUCTION, Difficulty.MEDIUM, 10),
    CAGE_HOUSE_INTERACTION(null, Difficulty.MEDIUM, 11),
    RULE_OF_45(null, Difficulty.MEDIUM, 12),
    RULE_OF_45_MULTI_HOUSE(null, Difficulty.HARD, 16),
    X_WING(SudokuTechnique.X_WING, Difficulty.HARD, 17),
    XY_WING(SudokuTechnique.XY_WING, Difficulty.HARD, 21),
    SWORDFISH(SudokuTechnique.SWORDFISH, Difficulty.HARD, 24),
    COLORING(SudokuTechnique.COLORING, Difficulty.EXPERT, 27),
    XYZ_WING(SudokuTechnique.XYZ_WING, Difficulty.EXPERT, 28),
    CHAINS(SudokuTechnique.CHAINS, Difficulty.EXPERT, 30);

    val cageBased: Boolean get() = classic == null

    companion object {
        fun fromClassic(technique: SudokuTechnique): KillerTechnique = entries.first { it.classic == technique }
    }
}

fun killerTechniquesUpTo(difficulty: Difficulty): Set<KillerTechnique> =
    KillerTechnique.entries.filter { it.tier.ordinal <= difficulty.ordinal }.toSet()

fun hardestKillerTechnique(techniques: Collection<KillerTechnique>): KillerTechnique? =
    techniques.maxWithOrNull(compareBy({ it.tier.ordinal }, { it.baseScore }))
