package com.quietgrid.app.games.whodidit

import android.content.res.Resources
import androidx.annotation.StringRes
import com.quietgrid.app.R
import com.quietgrid.engine.whodidit.WHODIDIT_THEMES
import com.quietgrid.engine.whodidit.WhoDidItCategory
import com.quietgrid.engine.whodidit.WhoDidItClue
import com.quietgrid.engine.whodidit.WhoDidItMark
import com.quietgrid.engine.whodidit.WhoDidItPuzzleEntry
import com.quietgrid.engine.whodidit.WhoDidItRef
import com.quietgrid.engine.whodidit.WhoDidItSource
import com.quietgrid.engine.whodidit.WhoDidItStep
import com.quietgrid.engine.whodidit.WhoDidItTechnique
import com.quietgrid.engine.whodidit.WhoDidItTrait

private class RelationRes(@StringRes val plain: Int, @StringRes val negated: Int, @StringRes val either: Int)

private val SUSPECT_EITHERS: Map<WhoDidItCategory, Int> = mapOf(
    WhoDidItCategory.PLACE to R.string.whodidit_either_suspects_place,
    WhoDidItCategory.ITEM to R.string.whodidit_either_suspects_item,
    WhoDidItCategory.MOTIVE to R.string.whodidit_either_suspects_motive,
)

private val RELATIONS: Map<WhoDidItCategory, RelationRes> = mapOf(
    WhoDidItCategory.PLACE to RelationRes(R.string.whodidit_rel_place, R.string.whodidit_rel_place_not, R.string.whodidit_rel_place_either),
    WhoDidItCategory.ITEM to RelationRes(R.string.whodidit_rel_item, R.string.whodidit_rel_item_not, R.string.whodidit_rel_item_either),
    WhoDidItCategory.MOTIVE to RelationRes(R.string.whodidit_rel_motive, R.string.whodidit_rel_motive_not, R.string.whodidit_rel_motive_either),
)

private val FIRST_PERSON_RELATIONS: Map<WhoDidItCategory, RelationRes> = mapOf(
    WhoDidItCategory.PLACE to RelationRes(R.string.whodidit_rel1_place, R.string.whodidit_rel1_place_not, R.string.whodidit_rel1_place_either),
    WhoDidItCategory.ITEM to RelationRes(R.string.whodidit_rel1_item, R.string.whodidit_rel1_item_not, R.string.whodidit_rel1_item_either),
    WhoDidItCategory.MOTIVE to RelationRes(R.string.whodidit_rel1_motive, R.string.whodidit_rel1_motive_not, R.string.whodidit_rel1_motive_either),
)

private val SUBJECTS: Map<WhoDidItCategory, Int> = mapOf(
    WhoDidItCategory.PLACE to R.string.whodidit_subject_place,
    WhoDidItCategory.ITEM to R.string.whodidit_subject_item,
    WhoDidItCategory.MOTIVE to R.string.whodidit_subject_motive,
)

private val CATEGORY_LABELS: Map<WhoDidItCategory, Int> = mapOf(
    WhoDidItCategory.SUSPECT to R.string.whodidit_category_suspect,
    WhoDidItCategory.PLACE to R.string.whodidit_category_place,
    WhoDidItCategory.ITEM to R.string.whodidit_category_item,
    WhoDidItCategory.MOTIVE to R.string.whodidit_category_motive,
)

private val HINT_TITLES: Map<WhoDidItTechnique, Int> = mapOf(
    WhoDidItTechnique.CLUE_DIRECT to R.string.whodidit_hint_clue_direct_title,
    WhoDidItTechnique.TRAIT_DIRECT to R.string.whodidit_hint_trait_direct_title,
    WhoDidItTechnique.SINGLE to R.string.whodidit_hint_single_title,
    WhoDidItTechnique.TRANSITIVE to R.string.whodidit_hint_transitive_title,
    WhoDidItTechnique.CLUE_PROPAGATION to R.string.whodidit_hint_clue_propagation_title,
    WhoDidItTechnique.INCOMPATIBLE to R.string.whodidit_hint_incompatible_title,
    WhoDidItTechnique.STATEMENT_CHECK to R.string.whodidit_hint_statement_check_title,
    WhoDidItTechnique.LIAR_HYPOTHESIS to R.string.whodidit_hint_liar_hypothesis_title,
    WhoDidItTechnique.CONTRADICTION to R.string.whodidit_hint_contradiction_title,
)

class WhoDidItText(private val resources: Resources, private val puzzle: WhoDidItPuzzleEntry) {
    private val themeDef = WHODIDIT_THEMES.first { it.id == puzzle.theme }
    private val themeRes = WHODIDIT_THEME_RES.getValue(puzzle.theme)
    private val caseRes = themeRes.cases[puzzle.case]
    private val locale get() = resources.configuration.locales[0]

    private val names: Map<WhoDidItCategory, Array<String>> = mapOf(
        WhoDidItCategory.SUSPECT to resources.getStringArray(themeRes.suspects),
        WhoDidItCategory.PLACE to resources.getStringArray(themeRes.places),
        WhoDidItCategory.ITEM to resources.getStringArray(themeRes.items),
        WhoDidItCategory.MOTIVE to resources.getStringArray(themeRes.motives),
    )

    private val placePreps: Array<String> = resources.getStringArray(themeRes.placePreps)

    private fun categoryOf(ref: WhoDidItRef) = WhoDidItCategory.entries[ref.category]

    private fun poolIndex(ref: WhoDidItRef) = puzzle.entries[ref.category][ref.slot]

    private fun cap(text: String) = text.replaceFirstChar { it.titlecase(locale) }

    private fun str(@StringRes id: Int, vararg args: Any) = resources.getString(id, *args)

    fun entryName(ref: WhoDidItRef): String = names.getValue(categoryOf(ref))[poolIndex(ref)]

    private fun inSentence(ref: WhoDidItRef): String {
        val name = entryName(ref)
        return if (categoryOf(ref) == WhoDidItCategory.MOTIVE) name.lowercase(locale) else name
    }

    private fun args(ref: WhoDidItRef): List<String> =
        if (categoryOf(ref) == WhoDidItCategory.PLACE) listOf(placePreps[poolIndex(ref)], inSentence(ref))
        else listOf(inSentence(ref))

    fun categoryLabel(category: WhoDidItCategory): String = str(CATEGORY_LABELS.getValue(category))

    private fun traitsOf(ref: WhoDidItRef): List<WhoDidItTrait> =
        themeDef.pools.getValue(categoryOf(ref))[poolIndex(ref)].sortedBy { it.ordinal }

    fun traitIcons(ref: WhoDidItRef): String =
        traitsOf(ref).joinToString("") { WHODIDIT_TRAIT_RES.getValue(it).icon }

    fun cardLines(ref: WhoDidItRef): List<String> =
        traitsOf(ref).map { trait ->
            val traitRes = WHODIDIT_TRAIT_RES.getValue(trait)
            "${traitRes.icon} ${str(traitRes.label)}"
        }

    fun themeTitle(): String = str(themeRes.title)

    fun caseTitle(): String = str(caseRes.title)

    fun caseQuestion(): String = str(caseRes.question)

    private fun culpritNoun() = str(caseRes.culpritNoun)

    private fun subject(ref: WhoDidItRef): String =
        if (ref.category == 0) entryName(ref) else str(SUBJECTS.getValue(categoryOf(ref)), *args(ref).toTypedArray())

    private fun sentence(subject: String, predicate: String) = cap(str(R.string.whodidit_sentence, subject, predicate))

    private fun relation(speakerFirstPerson: Boolean, other: WhoDidItRef, negated: Boolean): String {
        val table = if (speakerFirstPerson) FIRST_PERSON_RELATIONS else RELATIONS
        val rel = table.getValue(categoryOf(other))
        return str(if (negated) rel.negated else rel.plain, *args(other).toTypedArray())
    }

    private fun renderBody(clue: WhoDidItClue, speaker: Int?): String {
        fun firstPerson(ref: WhoDidItRef) = speaker != null && ref.category == 0 && ref.slot == speaker
        return when (clue) {
            is WhoDidItClue.Same -> twoRef(clue.a, clue.b, false, ::firstPerson)
            is WhoDidItClue.NotSame -> twoRef(clue.a, clue.b, true, ::firstPerson)
            is WhoDidItClue.Either -> {
                if (clue.b.category == 0) {
                    val optionArgs = listOf(entryName(clue.b), entryName(clue.c)) + args(clue.a)
                    cap(str(SUSPECT_EITHERS.getValue(categoryOf(clue.a)), *optionArgs.toTypedArray()))
                } else {
                    val rel = (if (firstPerson(clue.a)) FIRST_PERSON_RELATIONS else RELATIONS).getValue(categoryOf(clue.b))
                    val text = str(rel.either, *(args(clue.b) + args(clue.c)).toTypedArray())
                    if (firstPerson(clue.a)) cap(text) else sentence(subject(clue.a), text).removeSuffix(".")
                }
            }
            is WhoDidItClue.HasTrait -> traitBody(clue.a, clue.trait, true, firstPerson(clue.a))
            is WhoDidItClue.LacksTrait -> traitBody(clue.a, clue.trait, false, firstPerson(clue.a))
            is WhoDidItClue.Crime -> crimeBody(clue.e)
            is WhoDidItClue.CrimeTrait -> {
                val traitRes = WHODIDIT_TRAIT_RES.getValue(clue.trait)
                sentence(culpritNoun(), str(traitRes.predicate)).removeSuffix(".")
            }
        }
    }

    private fun twoRef(a: WhoDidItRef, b: WhoDidItRef, negated: Boolean, firstPerson: (WhoDidItRef) -> Boolean): String {
        val (subjectRef, other) = if (b.category < a.category) b to a else a to b
        val relationText = relation(firstPerson(subjectRef), other, negated)
        return if (firstPerson(subjectRef)) cap(relationText)
        else sentence(subject(subjectRef), relationText).removeSuffix(".")
    }

    private fun oppositeOf(trait: WhoDidItTrait): WhoDidItTrait? {
        val siblings = WhoDidItTrait.entries.filter { it.category == trait.category && it.dimension == trait.dimension }
        return if (siblings.size == 2) siblings.first { it != trait } else null
    }

    private fun traitBody(ref: WhoDidItRef, trait: WhoDidItTrait, holds: Boolean, firstPerson: Boolean): String {
        val opposite = if (holds) null else oppositeOf(trait)
        val traitRes = WHODIDIT_TRAIT_RES.getValue(opposite ?: trait)
        val positive = holds || opposite != null
        return if (firstPerson) {
            cap(str(if (positive) traitRes.firstPerson else traitRes.firstPersonNeg))
        } else {
            sentence(subject(ref), str(if (positive) traitRes.predicate else traitRes.negPredicate)).removeSuffix(".")
        }
    }

    private fun crimeBody(ref: WhoDidItRef): String = cap(
        when (categoryOf(ref)) {
            WhoDidItCategory.SUSPECT -> str(R.string.whodidit_crime_suspect, entryName(ref), culpritNoun())
            WhoDidItCategory.PLACE -> str(caseRes.crimePlace, *args(ref).toTypedArray())
            WhoDidItCategory.ITEM -> str(caseRes.crimeItem, inSentence(ref))
            WhoDidItCategory.MOTIVE -> str(caseRes.crimeMotive, inSentence(ref))
        },
    )

    fun clue(index: Int): String = renderBody(puzzle.clues[index], null) + "."

    fun statement(index: Int): String {
        val statement = puzzle.statements[index]
        val speaker = entryName(WhoDidItRef(0, statement.s))
        return str(R.string.whodidit_statement, speaker, renderBody(statement.clue, statement.s))
    }

    fun reveal(): String {
        val culprit = WhoDidItRef(0, puzzle.culprit)
        fun owned(category: Int) = WhoDidItRef(category, puzzle.solution[category][puzzle.culprit])
        val suspectName = entryName(culprit)
        val placeArgs = args(owned(1))
        val item = inSentence(owned(2))
        return if (puzzle.categories.size > 3) {
            cap(str(caseRes.revealMotive, suspectName, placeArgs[0], placeArgs[1], item, inSentence(owned(3))))
        } else {
            cap(str(caseRes.reveal, suspectName, placeArgs[0], placeArgs[1], item))
        }
    }

    fun hintTitle(step: WhoDidItStep): String = str(HINT_TITLES.getValue(step.technique))

    fun hintBody(step: WhoDidItStep): String {
        val clueNumbers = step.sources.filterIsInstance<WhoDidItSource.Clue>()
            .map { it.index + 1 }.distinct().sorted()
        val speakers = step.sources.filterIsInstance<WhoDidItSource.Statement>()
            .map { it.suspect }.distinct().map { entryName(WhoDidItRef(0, it)) }
        val parts = buildList {
            if (clueNumbers.isNotEmpty()) add(str(R.string.whodidit_hint_body_clues, clueNumbers.joinToString(", ")))
            if (speakers.isNotEmpty()) add(str(R.string.whodidit_hint_body_statements, speakers.joinToString(", ")))
        }
        return if (parts.isEmpty()) str(R.string.whodidit_hint_body_grid) else parts.joinToString(" ")
    }

    fun hintLead(step: WhoDidItStep): String = str(R.string.whodidit_hint_first, hintTitle(step), hintBody(step))

    fun hintAction(step: WhoDidItStep): String? = when (whoDidItHintMark(step)) {
        WhoDidItMark.YES -> str(R.string.whodidit_hint_action_yes)
        WhoDidItMark.NO -> str(R.string.whodidit_hint_action_no)
        else -> null
    }
}
