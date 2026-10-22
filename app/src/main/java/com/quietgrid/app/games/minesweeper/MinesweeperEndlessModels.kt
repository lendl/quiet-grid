package com.quietgrid.app.games.minesweeper

import com.quietgrid.engine.minesweeper.endless.EndlessWorld
import com.quietgrid.engine.minesweeper.endless.decodeEndlessSections
import com.quietgrid.engine.minesweeper.endless.encodeEndlessSections
import kotlinx.serialization.Serializable

const val MINESWEEPER_ENDLESS_HEARTS = 3
const val MINESWEEPER_ENDLESS_REASON_OUT_OF_HEARTS = "out-of-hearts"
const val MINESWEEPER_ENDLESS_REASON_ENDED = "ended"

@Serializable
data class MinesweeperEndlessCamera(val centerX: Float = 4.5f, val centerY: Float = 4.5f, val zoom: Float = 1f)

data class MinesweeperEndlessSession(
    val world: EndlessWorld,
    val hearts: Int,
    val bestAtStart: Int,
    val camera: MinesweeperEndlessCamera,
    val heartShake: Int = 0,
) {
    val score: Int get() = world.clearedCount
    val started: Boolean get() = world.sections.isNotEmpty()
}

@Serializable
data class MinesweeperEndlessPersistedSession(
    val sections: List<String>,
    val hearts: Int,
    val bestAtStart: Int,
    val camera: MinesweeperEndlessCamera = MinesweeperEndlessCamera(),
)

fun MinesweeperEndlessSession.toPersisted() =
    MinesweeperEndlessPersistedSession(encodeEndlessSections(world), hearts, bestAtStart, camera)

fun MinesweeperEndlessPersistedSession.toSession(): MinesweeperEndlessSession? =
    decodeEndlessSections(sections)?.let { MinesweeperEndlessSession(it, hearts, bestAtStart, camera) }

data class MinesweeperEndlessResult(
    val score: Int,
    val ring: Int,
    val elapsedSeconds: Int,
    val isNewBest: Boolean,
    val previousBest: Int,
    val reason: String,
)
