package com.quietgrid.engine.sudoku

object SudokuGradingFixtures {
    const val S1_EASY = ".64..8..98.127.64...24635.818659....295....614....1.5.5289.71...13.2.4..7..3162.5"
    const val S1_FROM_MEDIUM_LABEL = "56.2.1..39.........7.4..8..341...5..6.......4725..396....3.8..5....5.741..6...389"
    const val S2_THREE_USES = "52.8.7.1.1.........7.6..9..261...4..3.......9795..812....7.5..4.4..9.785..7...291"
    const val S2_ONE_USE = "12.8.7.454...9.....8.6..2..254...3..........4637..2.8....1.3..6.7..2.138..8.6..92"
    const val S3_TWO_STUCK_POINTS = "58.9.26479.........7.5..2..193...8..6.......4247..831....8.9....2..6.981..9.2.576"
    const val S3_ONE_STUCK_POINT_MANY_ELIMINATIONS = "24.6.1.595.........1.7..8...82...5..1.......6..3..729....4.3..5.3..6.97...1.7.48."
    const val S4_ONE_STUCK_POINT = ".17....5.8......1...6.74.....5....42.4.8..7.17....9.......42..85..6........985.6."
    const val S4_TWO_STUCK_POINTS = "286..1.7.9......6...7..8..9..9....21.6..2...41....7........5.8332.7.....6..38..1."
    const val S5_ONE_STUCK_POINT = "..34.8....8....5.2..12...6.6....2....45.8...1....5...6...89.3...1...3..5..2....9."

    val ALL = listOf(
        S1_EASY, S1_FROM_MEDIUM_LABEL, S2_THREE_USES, S2_ONE_USE, S3_TWO_STUCK_POINTS,
        S3_ONE_STUCK_POINT_MANY_ELIMINATIONS, S4_ONE_STUCK_POINT, S4_TWO_STUCK_POINTS, S5_ONE_STUCK_POINT,
    )

    fun parse(cells: String): SudokuGrid {
        require(cells.length == 81) { "Expected 81 cells, got ${cells.length}" }
        return cells.chunked(9).map { row -> row.map { if (it == '.') null else it.digitToInt() } }
    }
}
