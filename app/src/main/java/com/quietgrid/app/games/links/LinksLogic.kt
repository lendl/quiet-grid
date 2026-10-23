package com.quietgrid.app.games.links

import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.links.LinksStroke
import com.quietgrid.engine.links.applyLinksSolutionPath
import com.quietgrid.engine.links.findLinksHint
import com.quietgrid.engine.links.finishLinksStroke
import com.quietgrid.engine.links.isLinksSolved
import com.quietgrid.engine.links.isLinksStrokeUnmoved
import com.quietgrid.engine.links.isValidLinksSolution
import com.quietgrid.engine.links.linksAccuracyPercent
import com.quietgrid.engine.links.linksAreAdjacent

private const val LINKS_BASE_SCORE = 1000
private const val LINKS_HINT_PENALTY = 100
private const val LINKS_EXTRA_MOVE_PENALTY = 20

private fun linksSecondsPerPoint(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> 1
    Difficulty.MEDIUM -> 2
    Difficulty.HARD -> 3
    Difficulty.EXPERT -> 4
}

fun commitLinksStroke(session: LinksSession, stroke: LinksStroke): LinksSession {
    if (session.status != LinksStatus.PLAYING) return session
    if (isLinksStrokeUnmoved(stroke)) return session
    val paths = finishLinksStroke(stroke)
    if (paths == session.paths) return session
    val newColor = stroke.color != session.lastColor
    return withPaths(
        session.copy(
            moves = session.moves + if (newColor) 1 else 0,
            lastColor = stroke.color,
        ),
        paths,
    )
}

fun applyLinksHint(session: LinksSession): LinksSession? {
    if (session.status != LinksStatus.PLAYING) return null
    val color = findLinksHint(session.puzzle, session.paths)?.color ?: return null
    return withPaths(
        session.copy(hintsUsed = session.hintsUsed + 1),
        applyLinksSolutionPath(session.puzzle, session.paths, color),
    )
}

private fun withPaths(session: LinksSession, paths: List<List<Int>>): LinksSession = session.copy(
    paths = paths,
    status = if (isLinksSolved(session.puzzle, paths)) LinksStatus.WON else LinksStatus.PLAYING,
)

fun linksAccuracy(session: LinksSession): Int =
    linksAccuracyPercent(session.puzzle.pairCount, session.moves, session.hintsUsed)

fun linksScore(session: LinksSession, elapsedSeconds: Int): Int {
    val extraMoves = maxOf(0, session.moves - session.puzzle.pairCount)
    val secondsPerPoint = linksSecondsPerPoint(Difficulty.fromKey(session.puzzle.difficulty))
    return maxOf(
        0,
        LINKS_BASE_SCORE -
            session.hintsUsed * LINKS_HINT_PENALTY -
            extraMoves * LINKS_EXTRA_MOVE_PENALTY -
            elapsedSeconds / secondsPerPoint,
    )
}

fun isRestorableLinksSession(session: LinksSession): Boolean {
    val puzzle = session.puzzle
    if (!isValidLinksSolution(puzzle.size, puzzle.endpoints, puzzle.solution)) return false
    if (session.paths.size != puzzle.pairCount) return false
    val seen = HashSet<Int>()
    return session.paths.all { path ->
        path.all { it in 0 until puzzle.cellCount && seen.add(it) } &&
            path.zipWithNext().all { (a, b) -> linksAreAdjacent(a, b, puzzle.size) }
    }
}
