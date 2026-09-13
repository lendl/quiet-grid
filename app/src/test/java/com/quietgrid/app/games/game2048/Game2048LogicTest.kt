package com.quietgrid.app.games.game2048

import com.quietgrid.app.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Game2048LogicTest {

    @Test
    fun `createGame2048Session sizes the board per difficulty and seeds exactly two tiles`() {
        assertEquals(5, createGame2048Session(Difficulty.EASY).puzzle.size)
        assertEquals(4, createGame2048Session(Difficulty.MEDIUM).puzzle.size)
        assertEquals(4, createGame2048Session(Difficulty.HARD).puzzle.size)
        assertEquals(3, createGame2048Session(Difficulty.EXPERT).puzzle.size)

        val session = createGame2048Session(Difficulty.MEDIUM)
        val filled = session.board.tiles.flatten().count { it != null }
        assertEquals(2, filled)
        assertEquals(0, session.board.score)
        assertEquals(0, session.board.moveCount)
        assertEquals(Game2048Status.PLAYING, session.board.status)
    }

    @Test
    fun `sliding left compacts and merges a single row`() {
        val puzzle = Game2048Puzzle(difficulty = "medium", size = 4, fourSpawnChance = 0.0, targetTile = 2048)
        val tiles = listOf(
            listOf<Int?>(2, 2, 4, null),
            listOf<Int?>(null, null, null, null),
            listOf<Int?>(null, null, null, null),
            listOf<Int?>(null, null, null, null),
        )
        val board = Game2048Board(size = 4, tiles = tiles, score = 0, moveCount = 0, status = Game2048Status.PLAYING)

        val next = applyGame2048Move(board, puzzle, Game2048Direction.LEFT)

        assertEquals(4, next.tiles[0][0])
        assertEquals(4, next.tiles[0][1])
        assertEquals(4, next.score)
        assertEquals(1, next.moveCount)
    }

    @Test
    fun `a tile cannot merge twice in the same move`() {
        val puzzle = Game2048Puzzle(difficulty = "medium", size = 4, fourSpawnChance = 0.0, targetTile = 2048)
        val tiles = listOf(
            listOf<Int?>(2, 2, 2, 2),
            listOf<Int?>(null, null, null, null),
            listOf<Int?>(null, null, null, null),
            listOf<Int?>(null, null, null, null),
        )
        val board = Game2048Board(size = 4, tiles = tiles, score = 0, moveCount = 0, status = Game2048Status.PLAYING)

        val next = applyGame2048Move(board, puzzle, Game2048Direction.LEFT)

        assertEquals(listOf(4, 4), next.tiles[0].take(2))
        assertEquals(8, next.score)
    }

    @Test
    fun `sliding right compacts toward the far edge`() {
        val puzzle = Game2048Puzzle(difficulty = "medium", size = 4, fourSpawnChance = 0.0, targetTile = 2048)
        val tiles = listOf(
            listOf<Int?>(2, null, null, 2),
            listOf<Int?>(null, null, null, null),
            listOf<Int?>(null, null, null, null),
            listOf<Int?>(null, null, null, null),
        )
        val board = Game2048Board(size = 4, tiles = tiles, score = 0, moveCount = 0, status = Game2048Status.PLAYING)

        val next = applyGame2048Move(board, puzzle, Game2048Direction.RIGHT)

        assertEquals(4, next.tiles[0][3])
    }

    @Test
    fun `sliding up and down operate on columns`() {
        val puzzle = Game2048Puzzle(difficulty = "medium", size = 4, fourSpawnChance = 0.0, targetTile = 2048)
        val tiles = listOf(
            listOf<Int?>(2, null, null, null),
            listOf<Int?>(null, null, null, null),
            listOf<Int?>(null, null, null, null),
            listOf<Int?>(2, null, null, null),
        )
        val board = Game2048Board(size = 4, tiles = tiles, score = 0, moveCount = 0, status = Game2048Status.PLAYING)

        val up = applyGame2048Move(board, puzzle, Game2048Direction.UP)
        assertEquals(4, up.tiles[0][0])

        val down = applyGame2048Move(board, puzzle, Game2048Direction.DOWN)
        assertEquals(4, down.tiles[3][0])
    }

    @Test
    fun `a move that changes nothing leaves the board untouched and does not spawn`() {
        val puzzle = Game2048Puzzle(difficulty = "medium", size = 2, fourSpawnChance = 0.0, targetTile = 2048)
        val tiles = listOf(
            listOf<Int?>(2, 4),
            listOf<Int?>(4, 2),
        )
        val board = Game2048Board(size = 2, tiles = tiles, score = 0, moveCount = 0, status = Game2048Status.PLAYING)

        val next = applyGame2048Move(board, puzzle, Game2048Direction.LEFT)

        assertEquals(board, next)
    }

    @Test
    fun `reaching 2048 wins immediately`() {
        val puzzle = Game2048Puzzle(difficulty = "medium", size = 4, fourSpawnChance = 0.0, targetTile = 2048)
        val tiles = listOf(
            listOf<Int?>(1024, 1024, null, null),
            listOf<Int?>(null, null, null, null),
            listOf<Int?>(null, null, null, null),
            listOf<Int?>(null, null, null, null),
        )
        val board = Game2048Board(size = 4, tiles = tiles, score = 0, moveCount = 0, status = Game2048Status.PLAYING)

        val next = applyGame2048Move(board, puzzle, Game2048Direction.LEFT)

        assertEquals(Game2048Status.WON, next.status)
        assertEquals(2048, game2048BestTile(next))
    }

    @Test
    fun `a move that fills the last empty cell into a stuck arrangement is a loss`() {
        val puzzle = Game2048Puzzle(difficulty = "medium", size = 2, fourSpawnChance = 0.0, targetTile = 2048)
        val tiles = listOf(
            listOf<Int?>(4, 8),
            listOf<Int?>(16, null),
        )
        val board = Game2048Board(size = 2, tiles = tiles, score = 0, moveCount = 0, status = Game2048Status.PLAYING)

        val next = applyGame2048Move(board, puzzle, Game2048Direction.RIGHT)

        assertEquals(listOf(listOf<Int?>(4, 8), listOf<Int?>(2, 16)), next.tiles)
        assertEquals(Game2048Status.LOST, next.status)
    }

    @Test
    fun `a full board with an available merge is not a loss`() {
        val puzzle = Game2048Puzzle(difficulty = "medium", size = 2, fourSpawnChance = 0.0, targetTile = 2048)
        val tiles = listOf(
            listOf<Int?>(2, 8),
            listOf<Int?>(16, null),
        )
        val board = Game2048Board(size = 2, tiles = tiles, score = 0, moveCount = 0, status = Game2048Status.PLAYING)

        val next = applyGame2048Move(board, puzzle, Game2048Direction.RIGHT)

        assertEquals(listOf(listOf<Int?>(2, 8), listOf<Int?>(2, 16)), next.tiles)
        assertFalse(next.status == Game2048Status.LOST)
    }

    @Test
    fun `hasMeaningfulProgress is false for a freshly created session and true after one move`() {
        val session = createGame2048Session(Difficulty.MEDIUM)
        assertFalse(game2048HasMeaningfulProgress(session))

        val moved = session.copy(board = session.board.copy(moveCount = 1))
        assertTrue(game2048HasMeaningfulProgress(moved))
    }
}
