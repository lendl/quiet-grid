package com.quietgrid.app.games.links

import com.quietgrid.engine.links.LinksPuzzleEntry
import com.quietgrid.engine.links.emptyLinksPaths
import kotlinx.serialization.Serializable

enum class LinksStatus { PLAYING, WON }

data class LinksSession(
    val puzzle: LinksPuzzleEntry,
    val paths: List<List<Int>>,
    val moves: Int,
    val lastColor: Int?,
    val hintsUsed: Int,
    val status: LinksStatus,
)

@Serializable
data class LinksPersistedSession(
    val puzzle: LinksPuzzleEntry,
    val paths: List<List<Int>>,
    val moves: Int,
    val lastColor: Int? = null,
    val hintsUsed: Int,
    val status: String,
)

fun createLinksSession(puzzle: LinksPuzzleEntry): LinksSession = LinksSession(
    puzzle = puzzle,
    paths = emptyLinksPaths(puzzle),
    moves = 0,
    lastColor = null,
    hintsUsed = 0,
    status = LinksStatus.PLAYING,
)
