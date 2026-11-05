package com.quietgrid.app.data

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.core.journey.JourneyStopProgress
import com.quietgrid.app.core.journey.applyJourneyLoss
import com.quietgrid.app.core.journey.applyJourneyWin
import com.quietgrid.app.core.journey.journeyRatedPool
import com.quietgrid.app.core.journey.journeyRun
import com.quietgrid.app.core.journey.journeyScope
import com.quietgrid.app.core.journey.journeySiblingSeed
import com.quietgrid.app.core.journey.loadJourneyFile
import com.quietgrid.app.core.journey.locate
import com.quietgrid.app.core.journey.pickJourneySibling
import com.quietgrid.engine.journey.JourneyChapter
import com.quietgrid.engine.journey.JourneyFile
import com.quietgrid.engine.journey.JourneyStop
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface JourneyProgressStore {
    fun progressFor(gameId: GameId): Flow<Map<String, JourneyStopProgress>>
    suspend fun recordWin(gameId: GameId, stopId: String, signals: JourneySignals, elapsedSeconds: Int, score: Int): Int
    suspend fun recordLoss(gameId: GameId, stopId: String)
    suspend fun recordAbandon(gameId: GameId, stopId: String)
    suspend fun skip(gameId: GameId, stopId: String)
}

@Singleton
class JourneyProgressRepository(
    private val dao: JourneyProgressDao,
    private val historyStore: PlayHistoryStore,
    private val journeyFile: suspend (GameId) -> JourneyFile?,
    private val ratedPool: suspend (GameId, Difficulty) -> List<Pair<String, Int>>,
    private val clock: () -> Long = System::currentTimeMillis,
) : JourneyProgressStore {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        dao: JourneyProgressDao,
        historyStore: PlayHistoryStore,
    ) : this(
        dao = dao,
        historyStore = historyStore,
        journeyFile = { loadJourneyFile(context, it) },
        ratedPool = { gameId, difficulty -> journeyRatedPool(context, gameId, difficulty) },
    )

    override fun progressFor(gameId: GameId): Flow<Map<String, JourneyStopProgress>> = flow {
        applyReplacements(gameId)
        lineUpMissingReplays(gameId)
        emitAll(dao.progressFor(journeyScope(gameId)).map { rows -> rows.associate { it.puzzleId to it.toProgress() } })
    }

    override suspend fun recordWin(gameId: GameId, stopId: String, signals: JourneySignals, elapsedSeconds: Int, score: Int): Int {
        val old = current(gameId, stopId)
        val located = journeyFile(gameId)?.locate(stopId)
        val run = journeyRun(signals, elapsedSeconds, located?.second?.targetSeconds ?: 0)
        val replay = located?.let { (chapter, stop) -> nextSibling(gameId, chapter, stop, old, playedIds(gameId)) }?.takeIf { it != stopId }
        save(gameId, stopId, applyJourneyWin(old, run, elapsedSeconds, score, replay))
        return run.stars
    }

    override suspend fun recordLoss(gameId: GameId, stopId: String) {
        val old = current(gameId, stopId)
        val (chapter, stop) = journeyFile(gameId)?.locate(stopId) ?: return save(gameId, stopId, applyJourneyLoss(old, stopId))
        save(gameId, stopId, applyJourneyLoss(old, nextSibling(gameId, chapter, stop, old, playedIds(gameId))))
    }

    override suspend fun recordAbandon(gameId: GameId, stopId: String) {
        val old = current(gameId, stopId)
        val (chapter, stop) = journeyFile(gameId)?.locate(stopId) ?: return
        val replay = nextSibling(gameId, chapter, stop, old, playedIds(gameId)).takeIf { it != stopId }
        save(gameId, stopId, old.copy(nextPuzzleId = replay))
    }

    private suspend fun lineUpMissingReplays(gameId: GameId) {
        val missing = dao.progressFor(journeyScope(gameId)).first().filter { it.cleared && it.nextPuzzleId == null }
        if (missing.isEmpty()) return
        val file = journeyFile(gameId) ?: return
        val played = playedIds(gameId)
        missing.forEach { row ->
            val (chapter, stop) = file.locate(row.puzzleId) ?: return@forEach
            val replay = nextSibling(gameId, chapter, stop, row.toProgress(), played).takeIf { it != stop.puzzleId } ?: return@forEach
            dao.upsert(row.copy(nextPuzzleId = replay))
        }
    }

    private suspend fun playedIds(gameId: GameId): Set<String> =
        historyStore.recordsFor(gameId).first().mapNotNullTo(HashSet()) { it.puzzleId }

    private suspend fun nextSibling(gameId: GameId, chapter: JourneyChapter, stop: JourneyStop, old: JourneyStopProgress, played: Set<String>): String =
        pickJourneySibling(
            stop = stop,
            chapter = chapter,
            pool = ratedPool(gameId, Difficulty.fromKey(stop.difficulty)),
            playedIds = played + listOfNotNull(old.nextPuzzleId),
            seed = journeySiblingSeed(journeyScope(gameId), stop.puzzleId, old.attempts + 1),
        )

    override suspend fun skip(gameId: GameId, stopId: String) {
        save(gameId, stopId, current(gameId, stopId).copy(skipped = true))
    }

    private suspend fun current(gameId: GameId, stopId: String): JourneyStopProgress =
        dao.get(journeyScope(gameId), stopId)?.toProgress() ?: JourneyStopProgress()

    private suspend fun save(gameId: GameId, stopId: String, progress: JourneyStopProgress) {
        dao.upsert(progress.toEntity(journeyScope(gameId), stopId, clock()))
    }

    private suspend fun applyReplacements(gameId: GameId) {
        val scope = journeyScope(gameId)
        val file = journeyFile(gameId) ?: return
        file.chapters.flatMap { it.stops }.forEach { stop ->
            val oldId = stop.replaces ?: return@forEach
            val old = dao.get(scope, oldId) ?: return@forEach
            if (dao.get(scope, stop.puzzleId) != null) return@forEach
            dao.upsert(old.copy(puzzleId = stop.puzzleId))
            dao.delete(scope, oldId)
        }
    }
}

private fun JourneyProgressEntity.toProgress(): JourneyStopProgress = JourneyStopProgress(
    stars = stars,
    cleared = cleared,
    clean = clean,
    underTarget = underTarget,
    bestSeconds = bestSeconds,
    bestScore = bestScore,
    skipped = skipped,
    attempts = attempts,
    nextPuzzleId = nextPuzzleId,
)

private fun JourneyStopProgress.toEntity(scope: String, puzzleId: String, now: Long): JourneyProgressEntity = JourneyProgressEntity(
    scope = scope,
    puzzleId = puzzleId,
    stars = stars,
    cleared = cleared,
    clean = clean,
    underTarget = underTarget,
    bestSeconds = bestSeconds,
    bestScore = bestScore,
    skipped = skipped,
    attempts = attempts,
    nextPuzzleId = nextPuzzleId,
    lastPlayedMillis = now,
)
