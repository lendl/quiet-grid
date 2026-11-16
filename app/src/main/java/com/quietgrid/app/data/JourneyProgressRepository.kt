package com.quietgrid.app.data

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneyMilestone
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.core.journey.JourneyStopProgress
import com.quietgrid.app.core.journey.applyJourneyLoss
import com.quietgrid.app.core.journey.applyJourneyWin
import com.quietgrid.app.core.journey.journeyLevelRanges
import com.quietgrid.app.core.journey.journeyMilestones
import com.quietgrid.app.core.journey.journeyRatedPool
import com.quietgrid.app.core.journey.journeyRun
import com.quietgrid.app.core.journey.journeyScope
import com.quietgrid.app.core.journey.loadJourneyFile
import com.quietgrid.app.core.journey.locate
import com.quietgrid.app.core.journey.pickJourneyReplay
import com.quietgrid.engine.journey.JourneyFile
import com.quietgrid.engine.journey.JourneyStop
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface JourneyProgressStore {
    fun progressFor(gameId: GameId): Flow<Map<String, JourneyStopProgress>>
    suspend fun storedProgress(gameId: GameId): Map<String, JourneyStopProgress>
    suspend fun recordWin(gameId: GameId, stopId: String, puzzleId: String, signals: JourneySignals, elapsedSeconds: Int, score: Int): Int
    suspend fun recordLoss(gameId: GameId, stopId: String, puzzleId: String)
    suspend fun recordAbandon(gameId: GameId, stopId: String, puzzleId: String)
    suspend fun skip(gameId: GameId, stopId: String)
    suspend fun pendingMilestones(gameId: GameId): List<JourneyMilestone>
    suspend fun markShown(gameId: GameId, milestone: JourneyMilestone)
    fun isMastered(gameId: GameId): Flow<Boolean>
}

@Singleton
class JourneyProgressRepository(
    private val dao: JourneyProgressDao,
    private val historyStore: PlayHistoryStore,
    private val journeyFile: suspend (GameId) -> JourneyFile?,
    private val ratedPool: suspend (GameId, Difficulty) -> List<Pair<String, Int>>,
    private val clock: () -> Long = System::currentTimeMillis,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default,
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
        adoptJourneyHistory(gameId)
        lineUpReplays(gameId)
        emitAll(dao.progressFor(journeyScope(gameId)).map { rows -> rows.associate { it.puzzleId to it.toProgress() } })
    }.flowOn(workDispatcher)

    override suspend fun storedProgress(gameId: GameId): Map<String, JourneyStopProgress> =
        dao.progressFor(journeyScope(gameId)).first().associate { it.puzzleId to it.toProgress() }

    override suspend fun recordWin(gameId: GameId, stopId: String, puzzleId: String, signals: JourneySignals, elapsedSeconds: Int, score: Int): Int {
        val old = current(gameId, stopId).retiring(puzzleId)
        val file = journeyFile(gameId)
        val located = file?.locate(stopId)
        val run = journeyRun(signals, elapsedSeconds, located?.second?.targetSeconds ?: 0)
        val replay = located?.let { (_, stop) -> nextReplay(gameId, stop, old) }
        val before = storedProgress(gameId)
        save(gameId, stopId, applyJourneyWin(old, run, elapsedSeconds, score, replay))
        if (file != null) {
            val now = clock()
            journeyMilestones(file, stopId, before, storedProgress(gameId)).forEach { milestone ->
                dao.insertMilestone(milestone.toEntity(journeyScope(gameId), now))
            }
        }
        return run.stars
    }

    override suspend fun pendingMilestones(gameId: GameId): List<JourneyMilestone> =
        dao.pendingMilestones(journeyScope(gameId))
            .mapNotNull { it.toMilestone() }
            .sortedWith(compareBy<JourneyMilestone> { it is JourneyMilestone.Mastered }.thenBy { (it as? JourneyMilestone.ChapterComplete)?.chapterIndex ?: 0 })

    override suspend fun markShown(gameId: GameId, milestone: JourneyMilestone) {
        val now = clock()
        val row = milestone.toEntity(journeyScope(gameId), now)
        dao.markMilestoneShown(row.scope, row.kind, row.chapterIndex, now)
    }

    override fun isMastered(gameId: GameId): Flow<Boolean> = dao.hasMilestone(journeyScope(gameId), JOURNEY_MILESTONE_MASTERED)

    override suspend fun recordLoss(gameId: GameId, stopId: String, puzzleId: String) {
        val old = current(gameId, stopId).retiring(puzzleId)
        val stop = journeyFile(gameId)?.locate(stopId)?.second ?: return save(gameId, stopId, applyJourneyLoss(old, stopId))
        save(gameId, stopId, applyJourneyLoss(old, nextReplay(gameId, stop, old) ?: stopId))
    }

    override suspend fun recordAbandon(gameId: GameId, stopId: String, puzzleId: String) {
        val old = current(gameId, stopId).retiring(puzzleId)
        val stop = journeyFile(gameId)?.locate(stopId)?.second ?: return save(gameId, stopId, old)
        save(gameId, stopId, old.copy(nextPuzzleId = nextReplay(gameId, stop, old)))
    }

    private suspend fun adoptJourneyHistory(gameId: GameId) {
        val rows = dao.progressFor(journeyScope(gameId)).first()
        val unknown = rows.filter { it.retiredPuzzleIds.isEmpty() && (it.cleared || it.attempts > 0 || it.nextPuzzleId != null) }
        if (unknown.isEmpty()) return
        val played = historyStore.recordsFor(gameId).first()
            .filter { it.journeyStop != null && it.puzzleId != null }
            .sortedBy { it.timestampMillis }
            .groupBy({ it.journeyStop!! }, { it.puzzleId!! })
        unknown.forEach { row ->
            val retired = played[row.puzzleId] ?: listOf(row.puzzleId).takeIf { row.cleared } ?: return@forEach
            dao.upsert(row.copy(retiredPuzzleIds = encodeRetired(retired)))
        }
    }

    private suspend fun lineUpReplays(gameId: GameId) {
        val file = journeyFile(gameId) ?: return
        val rows = dao.progressFor(journeyScope(gameId)).first().associateByTo(LinkedHashMap()) { it.puzzleId }
        val progress = rows.mapValuesTo(LinkedHashMap()) { it.value.toProgress() }
        rows.values.toList().forEach { row ->
            val own = progress.getValue(row.puzzleId)
            if (own.retired.isEmpty()) return@forEach
            val next = own.nextPuzzleId
            val healthy = next != null && next != own.retired.last() &&
                progress.none { (id, other) -> id != row.puzzleId && (next == other.nextPuzzleId || next in other.retired) }
            if (healthy) return@forEach
            val (_, stop) = file.locate(row.puzzleId) ?: return@forEach
            val replay = nextReplay(file, gameId, stop, own, progress.filterKeys { it != row.puzzleId })
            if (replay == next) return@forEach
            dao.upsert(row.copy(nextPuzzleId = replay))
            progress[row.puzzleId] = own.copy(nextPuzzleId = replay)
        }
    }

    private suspend fun nextReplay(gameId: GameId, stop: JourneyStop, progress: JourneyStopProgress): String? {
        val file = journeyFile(gameId) ?: return null
        val others = dao.progressFor(journeyScope(gameId)).first()
            .filter { it.puzzleId != stop.puzzleId }
            .associate { it.puzzleId to it.toProgress() }
        return nextReplay(file, gameId, stop, progress, others)
    }

    private suspend fun nextReplay(
        file: JourneyFile,
        gameId: GameId,
        stop: JourneyStop,
        progress: JourneyStopProgress,
        others: Map<String, JourneyStopProgress>,
    ): String? {
        val pool = ratedPool(gameId, Difficulty.fromKey(stop.difficulty))
        return withContext(workDispatcher) {
            val sameTier = file.chapters.flatMap { it.stops }.filter { it.difficulty == stop.difficulty }
            pickJourneyReplay(
                stopId = stop.puzzleId,
                ranges = journeyLevelRanges(sameTier, pool),
                retiredByStop = others.mapValues { it.value.retired } + (stop.puzzleId to progress.retired),
                reservedElsewhere = others.values.mapNotNullTo(HashSet()) { it.nextPuzzleId },
            )
        }
    }

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

private fun JourneyMilestone.toEntity(scope: String, now: Long): JourneyMilestoneEntity = when (this) {
    is JourneyMilestone.ChapterComplete -> JourneyMilestoneEntity(scope, JOURNEY_MILESTONE_CHAPTER, chapterIndex, achievedMillis = now, shownMillis = null)
    JourneyMilestone.Mastered -> JourneyMilestoneEntity(scope, JOURNEY_MILESTONE_MASTERED, 0, achievedMillis = now, shownMillis = null)
}

private fun JourneyMilestoneEntity.toMilestone(): JourneyMilestone? = when (kind) {
    JOURNEY_MILESTONE_CHAPTER -> JourneyMilestone.ChapterComplete(chapterIndex)
    JOURNEY_MILESTONE_MASTERED -> JourneyMilestone.Mastered
    else -> null
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
    retired = decodeRetired(retiredPuzzleIds),
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
    retiredPuzzleIds = encodeRetired(retired),
)

private fun JourneyStopProgress.retiring(puzzleId: String): JourneyStopProgress = copy(retired = retired + puzzleId)

private const val RETIRED_SEPARATOR = "\n"

private fun encodeRetired(ids: List<String>): String = ids.joinToString(RETIRED_SEPARATOR)

private fun decodeRetired(text: String): List<String> = if (text.isEmpty()) emptyList() else text.split(RETIRED_SEPARATOR)
