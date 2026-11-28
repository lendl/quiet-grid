package com.quietgrid.app.data

import com.quietgrid.app.platform.BundledFiles
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneyMilestone
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.core.journey.JourneyStopProgress
import com.quietgrid.app.core.journey.applyJourneyLoss
import com.quietgrid.app.core.journey.applyJourneyWin
import com.quietgrid.app.core.journey.chapterIndex
import com.quietgrid.app.core.journey.journeyLevelRanges
import com.quietgrid.app.core.journey.journeyMilestones
import com.quietgrid.app.core.journey.journeyRatedPool
import com.quietgrid.app.core.journey.journeyRun
import com.quietgrid.app.core.journey.JourneyTrack
import com.quietgrid.app.core.journey.journeyTrack
import com.quietgrid.app.core.journey.journeyTrackOfStop
import com.quietgrid.app.core.journey.loadJourneyFile
import com.quietgrid.app.core.journey.locate
import com.quietgrid.app.core.journey.pickJourneyReplay
import com.quietgrid.app.core.journey.rank
import com.quietgrid.engine.journey.JourneyFile
import com.quietgrid.engine.journey.JourneyStop
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock

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

class JourneyProgressRepository(
    private val dao: JourneyProgressDao,
    private val historyStore: PlayHistoryStore,
    private val journeyFile: suspend (JourneyTrack) -> JourneyFile?,
    private val ratedPool: suspend (JourneyTrack, Difficulty) -> List<Pair<String, Int>>,
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val trackOf: (GameId) -> Flow<JourneyTrack> = { flowOf(journeyTrack(it, "en")) },
    private val stopTrackOf: (GameId, String) -> JourneyTrack? = ::journeyTrackOfStop,
) : JourneyProgressStore {

    constructor(
        files: BundledFiles,
        dao: JourneyProgressDao,
        historyStore: PlayHistoryStore,
        settingsRepository: SettingsRepository,
    ) : this(
        dao = dao,
        historyStore = historyStore,
        journeyFile = { loadJourneyFile(files, it) },
        ratedPool = { track, difficulty -> journeyRatedPool(files, track, difficulty) },
        trackOf = { settingsRepository.journeyTrack(it) },
    )

    private suspend fun track(gameId: GameId): JourneyTrack = trackOf(gameId).first()

    private suspend fun track(gameId: GameId, stopId: String): JourneyTrack = stopTrackOf(gameId, stopId) ?: track(gameId)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun progressFor(gameId: GameId): Flow<Map<String, JourneyStopProgress>> = trackOf(gameId).flatMapLatest { track ->
        flow {
            applyReplacements(track)
            adoptJourneyHistory(track)
            lineUpReplays(track)
            emitAll(dao.progressFor(track.scope).map { rows -> rows.associate { it.puzzleId to it.toProgress() } })
        }
    }.flowOn(workDispatcher)

    override suspend fun storedProgress(gameId: GameId): Map<String, JourneyStopProgress> = storedProgress(track(gameId))

    private suspend fun storedProgress(track: JourneyTrack): Map<String, JourneyStopProgress> =
        dao.progressFor(track.scope).first().associate { it.puzzleId to it.toProgress() }

    override suspend fun recordWin(gameId: GameId, stopId: String, puzzleId: String, signals: JourneySignals, elapsedSeconds: Int, score: Int): Int {
        val track = track(gameId, stopId)
        val old = current(track, stopId).retiring(puzzleId)
        val file = journeyFile(track)
        val located = file?.locate(stopId)
        val run = journeyRun(signals, elapsedSeconds, located?.second?.targetSeconds ?: 0)
        val replay = located?.let { (_, stop) -> nextReplay(track, stop, old) }
        val before = storedProgress(track)
        save(track, stopId, applyJourneyWin(old, run, elapsedSeconds, score, replay))
        if (file != null) {
            val now = clock()
            journeyMilestones(file, stopId, before, storedProgress(track)).forEach { milestone ->
                dao.upsertMilestone(milestone.toEntity(track.scope, now))
            }
        }
        return run.stars
    }

    override suspend fun pendingMilestones(gameId: GameId): List<JourneyMilestone> =
        dao.pendingMilestones(track(gameId).scope)
            .mapNotNull { it.toMilestone() }
            .sortedWith(compareBy({ it.rank }, { it.chapterIndex }))

    override suspend fun markShown(gameId: GameId, milestone: JourneyMilestone) {
        val now = clock()
        val row = milestone.toEntity(track(gameId).scope, now)
        dao.markMilestoneShown(row.scope, row.kind, row.chapterIndex, now)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun isMastered(gameId: GameId): Flow<Boolean> =
        trackOf(gameId).flatMapLatest { dao.hasMilestone(it.scope, JOURNEY_MILESTONE_MASTERED) }

    override suspend fun recordLoss(gameId: GameId, stopId: String, puzzleId: String) {
        val track = track(gameId, stopId)
        val old = current(track, stopId).retiring(puzzleId)
        val stop = journeyFile(track)?.locate(stopId)?.second ?: return save(track, stopId, applyJourneyLoss(old, stopId))
        save(track, stopId, applyJourneyLoss(old, nextReplay(track, stop, old) ?: stopId))
    }

    override suspend fun recordAbandon(gameId: GameId, stopId: String, puzzleId: String) {
        val track = track(gameId, stopId)
        val old = current(track, stopId).retiring(puzzleId)
        val stop = journeyFile(track)?.locate(stopId)?.second ?: return save(track, stopId, old)
        save(track, stopId, old.copy(nextPuzzleId = nextReplay(track, stop, old)))
    }

    private suspend fun adoptJourneyHistory(track: JourneyTrack) {
        val rows = dao.progressFor(track.scope).first()
        val unknown = rows.filter { it.retiredPuzzleIds.isEmpty() && (it.cleared || it.attempts > 0 || it.nextPuzzleId != null) }
        if (unknown.isEmpty()) return
        val played = historyStore.recordsFor(track.gameId).first()
            .filter { it.journeyStop != null && it.puzzleId != null }
            .sortedBy { it.timestampMillis }
            .groupBy({ it.journeyStop!! }, { it.puzzleId!! })
        unknown.forEach { row ->
            val retired = played[row.puzzleId] ?: listOf(row.puzzleId).takeIf { row.cleared } ?: return@forEach
            dao.upsert(row.copy(retiredPuzzleIds = encodeRetired(retired)))
        }
    }

    private suspend fun lineUpReplays(track: JourneyTrack) {
        val file = journeyFile(track) ?: return
        val rows = dao.progressFor(track.scope).first().associateByTo(LinkedHashMap()) { it.puzzleId }
        val progress = rows.mapValuesTo(LinkedHashMap()) { it.value.toProgress() }
        rows.values.toList().forEach { row ->
            val own = progress.getValue(row.puzzleId)
            if (own.retired.isEmpty()) return@forEach
            val next = own.nextPuzzleId
            val healthy = next != null && next != own.retired.last() &&
                progress.none { (id, other) -> id != row.puzzleId && (next == other.nextPuzzleId || next in other.retired) }
            if (healthy) return@forEach
            val (_, stop) = file.locate(row.puzzleId) ?: return@forEach
            val replay = nextReplay(file, track, stop, own, progress.filterKeys { it != row.puzzleId })
            if (replay == next) return@forEach
            dao.upsert(row.copy(nextPuzzleId = replay))
            progress[row.puzzleId] = own.copy(nextPuzzleId = replay)
        }
    }

    private suspend fun nextReplay(track: JourneyTrack, stop: JourneyStop, progress: JourneyStopProgress): String? {
        val file = journeyFile(track) ?: return null
        val others = dao.progressFor(track.scope).first()
            .filter { it.puzzleId != stop.puzzleId }
            .associate { it.puzzleId to it.toProgress() }
        return nextReplay(file, track, stop, progress, others)
    }

    private suspend fun nextReplay(
        file: JourneyFile,
        track: JourneyTrack,
        stop: JourneyStop,
        progress: JourneyStopProgress,
        others: Map<String, JourneyStopProgress>,
    ): String? {
        val pool = ratedPool(track, Difficulty.fromKey(stop.difficulty))
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
        val track = track(gameId, stopId)
        save(track, stopId, current(track, stopId).copy(skipped = true))
    }

    private suspend fun current(track: JourneyTrack, stopId: String): JourneyStopProgress =
        dao.get(track.scope, stopId)?.toProgress() ?: JourneyStopProgress()

    private suspend fun save(track: JourneyTrack, stopId: String, progress: JourneyStopProgress) {
        dao.upsert(progress.toEntity(track.scope, stopId, clock()))
    }

    private suspend fun applyReplacements(track: JourneyTrack) {
        val scope = track.scope
        val file = journeyFile(track) ?: return
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
    is JourneyMilestone.ChapterMastered -> JourneyMilestoneEntity(scope, JOURNEY_MILESTONE_CHAPTER_MASTERED, chapterIndex, achievedMillis = now, shownMillis = null)
    JourneyMilestone.JourneyComplete -> JourneyMilestoneEntity(scope, JOURNEY_MILESTONE_JOURNEY, 0, achievedMillis = now, shownMillis = null)
    JourneyMilestone.Mastered -> JourneyMilestoneEntity(scope, JOURNEY_MILESTONE_MASTERED, 0, achievedMillis = now, shownMillis = null)
}

private fun JourneyMilestoneEntity.toMilestone(): JourneyMilestone? = when (kind) {
    JOURNEY_MILESTONE_CHAPTER -> JourneyMilestone.ChapterComplete(chapterIndex)
    JOURNEY_MILESTONE_CHAPTER_MASTERED -> JourneyMilestone.ChapterMastered(chapterIndex)
    JOURNEY_MILESTONE_JOURNEY -> JourneyMilestone.JourneyComplete
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
