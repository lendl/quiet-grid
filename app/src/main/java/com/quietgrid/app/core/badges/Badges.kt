package com.quietgrid.app.core.badges

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JOURNEY_GAMES
import com.quietgrid.app.core.journey.JourneyStopProgress
import com.quietgrid.app.core.mix.Mix
import com.quietgrid.app.core.mix.MixEntryMode
import com.quietgrid.app.data.PlayRecord
import com.quietgrid.engine.journey.JourneyFile
import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate

enum class BadgeGroup { GENERAL, JOURNEY, GAME }

enum class BadgeSection { ACROSS, JOURNEYS, PATIENCE, SEASONAL }

enum class JourneyStage(val suffix: String) {
    CHAPTER_ONE("chapter_one"),
    JOURNEY_END("journey_end"),
    EVERY_STAR("every_star"),
}

enum class BadgeGlyph {
    TOUR,
    SUMMITS,
    MIX,
    ROADS,
    STARRY_ROAD,
    HOURGLASS,
    BOOKMARK,
    PUMPKIN,
    SUMMIT,
    CAGE,
    SMALL_SUMMIT,
    PAW,
    ARROW,
    NIGHT,
    BALANCE,
    PICTURE,
    SWEEP,
    FLAG,
    OPEN_FIELD,
    HIDDEN_WORD,
    CORNER,
    EYE,
    LAST_LINE,
    THREE,
    TWO,
    LOCK_OPEN,
    CLUES,
    FEW_CLUES,
    SPARKLE,
    DEEP,
    DEEP_SPARKLE,
    TWO_LINES,
    THREE_LINES,
    CLEAN_BOARD,
    EXIT,
    BOLT,
    LINK,
    SWAP,
}

sealed interface BadgeIcon {
    data class Glyph(val glyph: BadgeGlyph) : BadgeIcon
    data class Number(val value: Int) : BadgeIcon
    data class Journey(val stage: JourneyStage) : BadgeIcon
}

data class BadgeDef(
    val id: String,
    val gameId: GameId?,
    val group: BadgeGroup,
    val icon: BadgeIcon,
    val section: BadgeSection? = null,
)

@Serializable
data class BadgeProgress(
    val solvedGames: Set<String> = emptySet(),
    val expertGames: Set<String> = emptySet(),
    val mixSolvedGames: Map<String, Set<String>> = emptyMap(),
)

data class JourneySnapshot(
    val chapterFinished: Map<GameId, Boolean>,
    val journeyFinished: Map<GameId, Boolean>,
    val allStars: Map<GameId, Boolean>,
)

data class BadgeLedger(
    val earned: Map<String, LocalDate> = emptyMap(),
    val queue: List<String> = emptyList(),
    val announcement: String? = null,
)

data class BadgeBackfill(val earned: Set<String>, val progress: BadgeProgress)

const val EXPERT_OF_MANY_GAMES = 5
const val SLOW_AND_STEADY_SECONDS = 1800
const val MIXED_BAG_MIN_GAMES = 2

private class BadgeCheck(
    val finish: FinishFacts,
    val progress: BadgeProgress,
    val journeys: JourneySnapshot,
    val mixGames: Set<String>?,
    val nonBetaGameIds: Set<String>,
) {
    val facts: BadgeFacts get() = finish.facts
    val solved: Boolean get() = finish.solved
    val expert: Boolean get() = finish.solved && finish.difficulty == Difficulty.EXPERT
}

private class BadgeEntry(val def: BadgeDef, val qualifies: (BadgeCheck) -> Boolean)

private fun glyph(glyph: BadgeGlyph) = BadgeIcon.Glyph(glyph)

private fun general(id: String, section: BadgeSection, glyph: BadgeGlyph, qualifies: (BadgeCheck) -> Boolean) =
    BadgeEntry(BadgeDef(id, null, BadgeGroup.GENERAL, glyph(glyph), section), qualifies)

private fun badgePrefix(gameId: GameId): String = if (gameId == GameId.GAME_2048) "game2048" else gameId.key

private fun game(gameId: GameId, suffix: String, icon: BadgeIcon, qualifies: (BadgeCheck) -> Boolean) =
    BadgeEntry(BadgeDef("${badgePrefix(gameId)}_$suffix", gameId, BadgeGroup.GAME, icon), qualifies)

private fun journey(gameId: GameId, stage: JourneyStage, qualifies: (BadgeCheck) -> Boolean) =
    BadgeEntry(BadgeDef(journeyBadgeId(gameId, stage), gameId, BadgeGroup.JOURNEY, BadgeIcon.Journey(stage)), qualifies)

fun journeyBadgeId(gameId: GameId, stage: JourneyStage): String = "${gameId.key}_${stage.suffix}"

private fun BadgeCheck.journeyDone(gameIds: Collection<GameId>, flags: Map<GameId, Boolean>): Boolean =
    solved && gameIds.isNotEmpty() && gameIds.all { flags[it] == true }

private val SKILL_BADGES: Map<GameId, Pair<String, BadgeGlyph>> = mapOf(
    GameId.SUDOKU to ("summit" to BadgeGlyph.SUMMIT),
    GameId.KILLERSUDOKU to ("cage_keeper" to BadgeGlyph.CAGE),
    GameId.MINISUDOKU to ("small_summit" to BadgeGlyph.SMALL_SUMMIT),
    GameId.ANIMALDOKU to ("top_of_the_herd" to BadgeGlyph.PAW),
    GameId.ARROWESCAPE to ("no_bumps" to BadgeGlyph.ARROW),
    GameId.STARBATTLE to ("night_owl" to BadgeGlyph.NIGHT),
    GameId.TAKUZU to ("even_keel" to BadgeGlyph.BALANCE),
    GameId.NONOGRAM to ("full_picture" to BadgeGlyph.PICTURE),
    GameId.BLOCKESCAPE to ("clear_way" to BadgeGlyph.EXIT),
    GameId.CIRCUIT to ("live_wire" to BadgeGlyph.BOLT),
    GameId.LINKS to ("unbroken" to BadgeGlyph.LINK),
    GameId.WORDSWAP to ("word_perfect" to BadgeGlyph.SWAP),
)

private fun isBeta(gameId: GameId): Boolean = GameCatalog.get(gameId).beta

val BADGE_JOURNEY_GAMES: List<GameId> =
    GameCatalog.games.map { it.id }.filter { it in JOURNEY_GAMES && !isBeta(it) }

private fun journeyGameEntries(gameId: GameId): List<BadgeEntry> {
    val (suffix, skillGlyph) = SKILL_BADGES.getValue(gameId)
    val skill = game(gameId, suffix, glyph(skillGlyph)) { check ->
        check.expert && check.facts.hintsUsed == 0 && (gameId != GameId.ARROWESCAPE || check.facts.mistakes == 0)
    }
    return listOf(
        skill,
        journey(gameId, JourneyStage.CHAPTER_ONE) { it.journeyDone(listOf(gameId), it.journeys.chapterFinished) },
        journey(gameId, JourneyStage.JOURNEY_END) { it.journeyDone(listOf(gameId), it.journeys.journeyFinished) },
        journey(gameId, JourneyStage.EVERY_STAR) { it.journeyDone(listOf(gameId), it.journeys.allStars) },
    )
}

private val OTHER_GAME_ENTRIES: Map<GameId, List<BadgeEntry>> = mapOf(
    GameId.MINESWEEPER to listOf(
        game(GameId.MINESWEEPER, "clean_sweep", glyph(BadgeGlyph.SWEEP)) { it.solved && it.finish.difficulty == Difficulty.HARD },
        game(GameId.MINESWEEPER, "every_flag", glyph(BadgeGlyph.FLAG)) { it.solved && it.facts.allMinesFlagged },
        game(GameId.MINESWEEPER, "wide_open", glyph(BadgeGlyph.OPEN_FIELD)) { it.expert },
    ),
    GameId.WORDSEARCH to listOf(
        game(GameId.WORDSEARCH, "hidden_word", glyph(BadgeGlyph.HIDDEN_WORD)) { it.solved && it.facts.hiddenWordSolved },
        game(GameId.WORDSEARCH, "every_corner", glyph(BadgeGlyph.CORNER)) { it.expert },
        game(GameId.WORDSEARCH, "bright_eyes", glyph(BadgeGlyph.EYE)) { it.expert && it.facts.hiddenWordSolved && it.facts.hintsUsed == 0 },
    ),
    GameId.WORDGUESS to listOf(
        game(GameId.WORDGUESS, "last_line", glyph(BadgeGlyph.LAST_LINE)) { it.solved && it.facts.guesses == 6 },
        game(GameId.WORDGUESS, "in_three", glyph(BadgeGlyph.THREE)) { it.solved && (it.facts.guesses ?: Int.MAX_VALUE) <= 3 },
        game(GameId.WORDGUESS, "in_two", glyph(BadgeGlyph.TWO)) { it.solved && (it.facts.guesses ?: Int.MAX_VALUE) <= 2 },
    ),
    GameId.GUESSBYNUMBERS to listOf(
        game(GameId.GUESSBYNUMBERS, "cracked_open", glyph(BadgeGlyph.LOCK_OPEN)) { it.expert },
        game(GameId.GUESSBYNUMBERS, "few_clues", glyph(BadgeGlyph.FEW_CLUES)) { it.solved && (it.facts.guesses ?: Int.MAX_VALUE) <= 4 },
        game(GameId.GUESSBYNUMBERS, "three_clues", glyph(BadgeGlyph.CLUES)) { it.solved && (it.facts.guesses ?: Int.MAX_VALUE) <= 3 },
    ),
    GameId.THEMECLEAR to listOf(
        game(GameId.THEMECLEAR, "spotless", glyph(BadgeGlyph.SPARKLE)) { it.solved && it.facts.hintsUsed == 0 },
        game(GameId.THEMECLEAR, "deep_theme", glyph(BadgeGlyph.DEEP)) { it.expert },
        game(GameId.THEMECLEAR, "deep_and_spotless", glyph(BadgeGlyph.DEEP_SPARKLE)) { it.expert && it.facts.hintsUsed == 0 },
    ),
    GameId.BLOCKFILL to listOf(
        game(GameId.BLOCKFILL, "two_at_once", glyph(BadgeGlyph.TWO_LINES)) { it.facts.maxLinesAtOnce >= 2 },
        game(GameId.BLOCKFILL, "three_at_once", glyph(BadgeGlyph.THREE_LINES)) { it.facts.maxLinesAtOnce >= 3 },
        game(GameId.BLOCKFILL, "clean_board", glyph(BadgeGlyph.CLEAN_BOARD)) { it.facts.boardEmptied },
    ),
    GameId.GAME_2048 to listOf(1024, 2048, 4096).map { tile ->
        game(GameId.GAME_2048, tile.toString(), BadgeIcon.Number(tile)) { it.facts.maxTile >= tile }
    },
    GameId.CHIMPTEST to reachBadges(GameId.CHIMPTEST),
    GameId.SEQUENCEMEMORY to reachBadges(GameId.SEQUENCEMEMORY),
)

private fun reachBadges(gameId: GameId): List<BadgeEntry> =
    listOf("twelve" to 12, "sixteen" to 16, "twenty" to 20).map { (suffix, target) ->
        game(gameId, suffix, BadgeIcon.Number(target)) { it.facts.reached >= target }
    }

private val GENERAL_ENTRIES: List<BadgeEntry> = listOf(
    general("full_tour", BadgeSection.ACROSS, BadgeGlyph.TOUR) { check ->
        check.solved && check.nonBetaGameIds.isNotEmpty() && check.progress.solvedGames.containsAll(check.nonBetaGameIds)
    },
    general("expert_of_many", BadgeSection.ACROSS, BadgeGlyph.SUMMITS) { check ->
        check.expert && check.progress.expertGames.size >= EXPERT_OF_MANY_GAMES
    },
    general("mixed_bag", BadgeSection.ACROSS, BadgeGlyph.MIX) { check ->
        val mixId = check.finish.mixId
        val games = check.mixGames
        check.solved && mixId != null && games != null && games.size >= MIXED_BAG_MIN_GAMES &&
            check.progress.mixSolvedGames[mixId].orEmpty().containsAll(games)
    },
    general("all_journeys", BadgeSection.JOURNEYS, BadgeGlyph.ROADS) { it.journeyDone(BADGE_JOURNEY_GAMES, it.journeys.journeyFinished) },
    general("starry_road", BadgeSection.JOURNEYS, BadgeGlyph.STARRY_ROAD) { it.journeyDone(BADGE_JOURNEY_GAMES, it.journeys.allStars) },
    general("slow_and_steady", BadgeSection.PATIENCE, BadgeGlyph.HOURGLASS) { it.solved && it.finish.elapsedSeconds >= SLOW_AND_STEADY_SECONDS },
    general("picked_back_up", BadgeSection.PATIENCE, BadgeGlyph.BOOKMARK) { check ->
        val started = check.finish.startedDate
        check.solved && started != null && started < check.finish.date
    },
    general("pumpkin_patch", BadgeSection.SEASONAL, BadgeGlyph.PUMPKIN) { it.solved && it.facts.seasonal == SEASON_HALLOWEEN },
)

private val GAME_ENTRIES: Map<GameId, List<BadgeEntry>> =
    GameCatalog.games.map { it.id }.filterNot(::isBeta).associateWith { gameId ->
        if (gameId in SKILL_BADGES) journeyGameEntries(gameId) else OTHER_GAME_ENTRIES[gameId].orEmpty()
    }.filterValues { it.isNotEmpty() }

private val ALL_ENTRIES: List<BadgeEntry> = GENERAL_ENTRIES + GAME_ENTRIES.values.flatten()

object BadgeCatalog {
    val general: List<BadgeDef> = GENERAL_ENTRIES.map { it.def }
    val all: List<BadgeDef> = ALL_ENTRIES.map { it.def }
    private val byId: Map<String, BadgeDef> = all.associateBy { it.id }

    fun forGame(gameId: GameId): List<BadgeDef> = GAME_ENTRIES[gameId].orEmpty().map { it.def }

    fun byId(id: String): BadgeDef? = byId[id]

    val gameBadges: List<BadgeDef> get() = all.filter { it.gameId != null }

    fun section(section: BadgeSection): List<BadgeDef> = general.filter { it.section == section }
}

val NON_BETA_GAME_KEYS: Set<String> = GameCatalog.games.filterNot { it.beta }.map { it.id.key }.toSet()

fun updatedProgress(progress: BadgeProgress, finish: FinishFacts, nonBetaGameIds: Set<String> = NON_BETA_GAME_KEYS): BadgeProgress {
    val key = finish.gameId.key
    if (!finish.solved || key !in nonBetaGameIds) return progress
    val mixId = finish.mixId
    return progress.copy(
        solvedGames = progress.solvedGames + key,
        expertGames = if (finish.difficulty == Difficulty.EXPERT) progress.expertGames + key else progress.expertGames,
        mixSolvedGames = if (mixId == null) progress.mixSolvedGames else progress.mixSolvedGames + (mixId to progress.mixSolvedGames[mixId].orEmpty() + key),
    )
}

fun qualifyingBadges(
    finish: FinishFacts,
    progress: BadgeProgress,
    journeys: JourneySnapshot,
    mixGames: Set<String>?,
    nonBetaGameIds: Set<String>,
): List<String> {
    if (finish.gameId.key !in nonBetaGameIds) return emptyList()
    val check = BadgeCheck(finish, updatedProgress(progress, finish, nonBetaGameIds), journeys, mixGames, nonBetaGameIds)
    val own = GAME_ENTRIES[finish.gameId].orEmpty()
    val ordered = own.filter { it.def.group == BadgeGroup.GAME } + own.filter { it.def.group == BadgeGroup.JOURNEY } + GENERAL_ENTRIES
    return ordered.filter { it.qualifies(check) }.map { it.def.id }
}

fun BadgeLedger.afterFinish(qualifying: List<String>, today: LocalDate): BadgeLedger {
    val fresh = qualifying.distinct().filter { it !in earned && it !in queue }
    val next = queue.firstOrNull()
    return when {
        next != null -> BadgeLedger(earned + (next to today), queue.drop(1) + fresh, next)
        fresh.isNotEmpty() -> BadgeLedger(earned + (fresh.first() to today), fresh.drop(1), fresh.first())
        else -> copy(announcement = null)
    }
}

private fun JourneyFile.stopProgress(progress: Map<String, JourneyStopProgress>) =
    chapters.map { chapter -> chapter.stops.map { stop -> progress[stop.puzzleId] ?: stop.replaces?.let { progress[it] } } }

fun journeySnapshot(
    files: Map<GameId, JourneyFile>,
    progress: Map<GameId, Map<String, JourneyStopProgress>>,
): JourneySnapshot {
    val chapters = files.mapValues { (gameId, file) -> file.stopProgress(progress[gameId].orEmpty()).filter { it.isNotEmpty() } }
    return JourneySnapshot(
        chapterFinished = chapters.mapValues { (_, stops) -> stops.any { chapter -> chapter.all { it?.cleared == true } } },
        journeyFinished = chapters.mapValues { (_, stops) -> stops.isNotEmpty() && stops.all { chapter -> chapter.all { it?.cleared == true } } },
        allStars = chapters.mapValues { (_, stops) -> stops.isNotEmpty() && stops.flatten().all { (it?.stars ?: 0) >= 3 } },
    )
}

fun backfillBadges(records: List<PlayRecord>, journeys: JourneySnapshot, nonBetaGameIds: Set<String> = NON_BETA_GAME_KEYS): BadgeBackfill {
    val solved = records.filter { it.solved && !it.isChallenger && it.gameId in nonBetaGameIds }
    val progress = BadgeProgress(
        solvedGames = solved.map { it.gameId }.toSet(),
        expertGames = solved.filter { it.difficulty == Difficulty.EXPERT.key }.map { it.gameId }.toSet(),
    )
    val earned = buildSet {
        if (nonBetaGameIds.isNotEmpty() && progress.solvedGames.containsAll(nonBetaGameIds)) add("full_tour")
        if (progress.expertGames.size >= EXPERT_OF_MANY_GAMES) add("expert_of_many")
        if (solved.any { it.elapsedSeconds >= SLOW_AND_STEADY_SECONDS }) add("slow_and_steady")
        BADGE_JOURNEY_GAMES.forEach { gameId ->
            if (journeys.chapterFinished[gameId] == true) add(journeyBadgeId(gameId, JourneyStage.CHAPTER_ONE))
            if (journeys.journeyFinished[gameId] == true) add(journeyBadgeId(gameId, JourneyStage.JOURNEY_END))
            if (journeys.allStars[gameId] == true) add(journeyBadgeId(gameId, JourneyStage.EVERY_STAR))
        }
        if (BADGE_JOURNEY_GAMES.all { journeys.journeyFinished[it] == true }) add("all_journeys")
        if (BADGE_JOURNEY_GAMES.all { journeys.allStars[it] == true }) add("starry_road")
    }
    return BadgeBackfill(earned.filter { BadgeCatalog.byId(it) != null }.toSet(), progress)
}

data class BadgeCount(val done: Int, val total: Int)

fun badgeCount(id: String, progress: BadgeProgress, journeys: JourneySnapshot, nonBetaGameIds: Set<String> = NON_BETA_GAME_KEYS): BadgeCount? = when (id) {
    "full_tour" -> BadgeCount(progress.solvedGames.count { it in nonBetaGameIds }, nonBetaGameIds.size)
    "expert_of_many" -> BadgeCount(progress.expertGames.count { it in nonBetaGameIds }.coerceAtMost(EXPERT_OF_MANY_GAMES), EXPERT_OF_MANY_GAMES)
    "all_journeys" -> BadgeCount(BADGE_JOURNEY_GAMES.count { journeys.journeyFinished[it] == true }, BADGE_JOURNEY_GAMES.size)
    "starry_road" -> BadgeCount(BADGE_JOURNEY_GAMES.count { journeys.allStars[it] == true }, BADGE_JOURNEY_GAMES.size)
    else -> null
}

fun mixBadgeGames(mix: Mix, nonBetaGameIds: Set<String> = NON_BETA_GAME_KEYS): Set<String> =
    mix.entries.filter { it.mode == MixEntryMode.PUZZLE && it.gameId in nonBetaGameIds }.map { it.gameId }.toSet()
