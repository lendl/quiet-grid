package com.quietgrid.app.games

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.games.animaldoku.AnimalDokuDefinition
import com.quietgrid.app.games.arrowescape.ArrowEscapeDefinition
import com.quietgrid.app.games.blockescape.BlockEscapeDefinition
import com.quietgrid.app.games.blockfill.BlockFillDefinition
import com.quietgrid.app.games.chimptest.ChimpTestDefinition
import com.quietgrid.app.games.circuit.CircuitDefinition
import com.quietgrid.app.games.game2048.Game2048Definition
import com.quietgrid.app.games.guessbynumbers.GuessByNumbersDefinition
import com.quietgrid.app.games.hashi.HashiDefinition
import com.quietgrid.app.games.killersudoku.KillerSudokuDefinition
import com.quietgrid.app.games.links.LinksDefinition
import com.quietgrid.app.games.minesweeper.MinesweeperDefinition
import com.quietgrid.app.games.minisudoku.MiniSudokuDefinition
import com.quietgrid.app.games.nback.NBackDefinition
import com.quietgrid.app.games.nonogram.NonogramDefinition
import com.quietgrid.app.games.sequencememory.SequenceMemoryDefinition
import com.quietgrid.app.games.starbattle.StarBattleDefinition
import com.quietgrid.app.games.sudoku.SudokuDefinition
import com.quietgrid.app.games.takuzu.TakuzuDefinition
import com.quietgrid.app.games.themeclear.ThemeClearDefinition
import com.quietgrid.app.games.whodidit.WhoDidItDefinition
import com.quietgrid.app.games.wordguess.WordGuessDefinition
import com.quietgrid.app.games.wordsearch.WordSearchDefinition
import com.quietgrid.app.games.wordswap.WordSwapDefinition

object GameRegistry {
    val all: List<GameDefinition> = listOf(
        ChimpTestDefinition,
        TakuzuDefinition,
        NonogramDefinition,
        MinesweeperDefinition,
        SudokuDefinition,
        WordSearchDefinition,
        BlockFillDefinition,
        WordGuessDefinition,
        AnimalDokuDefinition,
        ArrowEscapeDefinition,
        Game2048Definition,
        StarBattleDefinition,
        GuessByNumbersDefinition,
        NBackDefinition,
        ThemeClearDefinition,
        WhoDidItDefinition,
        SequenceMemoryDefinition,
        HashiDefinition,
        KillerSudokuDefinition,
        MiniSudokuDefinition,
        LinksDefinition,
        BlockEscapeDefinition,
        WordSwapDefinition,
        CircuitDefinition,
    )

    private val byId: Map<GameId, GameDefinition> = all.associateBy { it.id }

    fun of(id: GameId): GameDefinition = byId.getValue(id)
}

fun gameDifficultyLabelRes(gameId: GameId, difficulty: Difficulty): Int =
    GameRegistry.of(gameId).difficultyLabelRes(difficulty)
