package com.quietgrid.engine.whodidit

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class WhoDidItTechniquesTest {
    private fun s(slot: Int) = WhoDidItRef(0, slot)
    private fun p(slot: Int) = WhoDidItRef(1, slot)
    private fun i(slot: Int) = WhoDidItRef(2, slot)

    private fun specWith(vararg clues: WhoDidItClue) = easyFixture().copy(clues = clues.toList())
    private fun fresh() = WhoDidItGridState(3, 3)

    @Test
    fun clueDirectSameMarksYes() {
        val step = findClueDirect(easyFixture(), fresh())
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.CLUE_DIRECT,
                sources = listOf(WhoDidItSource.Clue(1)),
                changes = listOf(WhoDidItCellChange(s(1), p(2), WhoDidItMark.YES)),
            ),
            step,
        )
    }

    @Test
    fun clueDirectNotSameMarksNo() {
        val step = findClueDirect(specWith(WhoDidItClue.NotSame(s(0), i(2))), fresh())
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.CLUE_DIRECT,
                sources = listOf(WhoDidItSource.Clue(0)),
                changes = listOf(WhoDidItCellChange(s(0), i(2), WhoDidItMark.NO)),
            ),
            step,
        )
    }

    @Test
    fun clueDirectSkipsAlreadyMarked() {
        val state = fresh()
        state.set(s(1), p(2), WhoDidItMark.YES)
        assertNull(findClueDirect(specWith(WhoDidItClue.Same(s(1), p(2))), state))
        assertNull(findClueDirect(specWith(WhoDidItClue.HasTrait(s(0), WhoDidItTrait.PLACE_OUTDOOR)), fresh()))
    }

    @Test
    fun clueDirectCrimeOnSuspectSetsCandidates() {
        val step = findClueDirect(specWith(WhoDidItClue.Crime(s(2))), fresh())
        assertEquals(
            WhoDidItStep(WhoDidItTechnique.CLUE_DIRECT, sources = listOf(WhoDidItSource.Clue(0)), culpritRemovals = setOf(0, 1)),
            step,
        )
        val state = fresh()
        state.culpritCandidates.retainAll(setOf(2))
        assertNull(findClueDirect(specWith(WhoDidItClue.Crime(s(2))), state))
    }

    @Test
    fun clueDirectCrimeOnEntryRemovesNoCandidatesAndMarksSole() {
        val state = fresh()
        state.set(s(0), i(1), WhoDidItMark.NO)
        state.set(s(1), i(1), WhoDidItMark.NO)
        val step = findClueDirect(specWith(WhoDidItClue.Crime(i(1))), state)
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.CLUE_DIRECT,
                sources = listOf(WhoDidItSource.Clue(0)),
                changes = listOf(WhoDidItCellChange(s(2), i(1), WhoDidItMark.YES)),
                culpritRemovals = setOf(0, 1),
            ),
            step,
        )
        assertNull(findClueDirect(specWith(WhoDidItClue.Crime(i(1))), fresh()))
    }

    @Test
    fun clueDirectUsesStatementOfRemovedCandidate() {
        val spec = liarFixture().copy(clues = emptyList())
        val state = fresh()
        state.culpritCandidates.remove(1)
        val step = findClueDirect(spec, state)
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.CLUE_DIRECT,
                sources = listOf(WhoDidItSource.Statement(1)),
                changes = listOf(WhoDidItCellChange(s(1), p(0), WhoDidItMark.NO)),
            ),
            step,
        )
    }

    @Test
    fun traitDirectHasTraitMarksNoOnLackingSlots() {
        val step = findTraitDirect(easyFixture(), fresh())
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.TRAIT_DIRECT,
                sources = listOf(WhoDidItSource.Clue(0)),
                changes = listOf(
                    WhoDidItCellChange(s(0), p(0), WhoDidItMark.NO),
                    WhoDidItCellChange(s(0), p(2), WhoDidItMark.NO),
                ),
            ),
            step,
        )
    }

    @Test
    fun traitDirectLacksTraitMarksNoOnHavingSlots() {
        val step = findTraitDirect(specWith(WhoDidItClue.LacksTrait(s(0), WhoDidItTrait.PLACE_OUTDOOR)), fresh())
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.TRAIT_DIRECT,
                sources = listOf(WhoDidItSource.Clue(0)),
                changes = listOf(WhoDidItCellChange(s(0), p(1), WhoDidItMark.NO)),
            ),
            step,
        )
    }

    @Test
    fun traitDirectReturnsNullWhenAlreadyApplied() {
        val state = fresh()
        state.set(s(0), p(0), WhoDidItMark.NO)
        state.set(s(0), p(2), WhoDidItMark.NO)
        assertNull(findTraitDirect(specWith(WhoDidItClue.HasTrait(s(0), WhoDidItTrait.PLACE_OUTDOOR)), state))
        assertNull(findTraitDirect(specWith(WhoDidItClue.Same(s(1), p(2))), fresh()))
    }

    @Test
    fun singleFindsRowWithOneOpenCell() {
        val state = fresh()
        state.set(s(0), p(0), WhoDidItMark.NO)
        state.set(s(0), p(2), WhoDidItMark.NO)
        val step = findSingle(easyFixture(), state)
        assertEquals(
            WhoDidItStep(WhoDidItTechnique.SINGLE, changes = listOf(WhoDidItCellChange(s(0), p(1), WhoDidItMark.YES))),
            step,
        )
    }

    @Test
    fun singleFindsColumnWithOneOpenCell() {
        val state = fresh()
        state.set(s(0), p(2), WhoDidItMark.NO)
        state.set(s(1), p(2), WhoDidItMark.NO)
        val step = findSingle(easyFixture(), state)
        assertEquals(
            WhoDidItStep(WhoDidItTechnique.SINGLE, changes = listOf(WhoDidItCellChange(s(2), p(2), WhoDidItMark.YES))),
            step,
        )
    }

    @Test
    fun singleReturnsNullWhenNothingForced() {
        assertNull(findSingle(easyFixture(), fresh()))
        val state = fresh()
        state.set(s(0), p(1), WhoDidItMark.YES)
        state.autoFill()
        assertNull(findSingle(easyFixture(), state))
    }

    @Test
    fun transitiveYesYesGivesYes() {
        val state = fresh()
        state.set(s(1), p(2), WhoDidItMark.YES)
        state.set(s(1), i(0), WhoDidItMark.YES)
        val step = findTransitive(easyFixture(), state)
        assertEquals(
            WhoDidItStep(WhoDidItTechnique.TRANSITIVE, changes = listOf(WhoDidItCellChange(p(2), i(0), WhoDidItMark.YES))),
            step,
        )
    }

    @Test
    fun transitiveYesNoGivesNo() {
        val state = fresh()
        state.set(s(1), p(2), WhoDidItMark.YES)
        state.set(s(1), i(1), WhoDidItMark.NO)
        val step = findTransitive(easyFixture(), state)
        assertEquals(
            WhoDidItStep(WhoDidItTechnique.TRANSITIVE, changes = listOf(WhoDidItCellChange(p(2), i(1), WhoDidItMark.NO))),
            step,
        )
    }

    @Test
    fun transitiveReturnsNullWhenNothingNew() {
        assertNull(findTransitive(easyFixture(), fresh()))
        val state = fresh()
        state.set(s(1), p(2), WhoDidItMark.YES)
        assertNull(findTransitive(easyFixture(), state))
        state.set(s(1), i(0), WhoDidItMark.YES)
        state.set(p(2), i(0), WhoDidItMark.YES)
        assertNull(findTransitive(easyFixture(), state))
    }

    @Test
    fun cluePropagationEitherYesesOtherWhenOneRuledOut() {
        val spec = specWith(WhoDidItClue.Either(s(0), p(0), p(1)))
        val state = fresh()
        state.set(s(0), p(0), WhoDidItMark.NO)
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.CLUE_PROPAGATION,
                sources = listOf(WhoDidItSource.Clue(0)),
                changes = listOf(
                    WhoDidItCellChange(s(0), p(1), WhoDidItMark.YES),
                    WhoDidItCellChange(s(0), p(2), WhoDidItMark.NO),
                ),
            ),
            findCluePropagation(spec, state),
        )
        val other = fresh()
        other.set(s(0), p(1), WhoDidItMark.NO)
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.CLUE_PROPAGATION,
                sources = listOf(WhoDidItSource.Clue(0)),
                changes = listOf(
                    WhoDidItCellChange(s(0), p(0), WhoDidItMark.YES),
                    WhoDidItCellChange(s(0), p(2), WhoDidItMark.NO),
                ),
            ),
            findCluePropagation(spec, other),
        )
    }

    @Test
    fun cluePropagationEitherRulesOutOtherEntriesAndHasNullCase() {
        val spec = specWith(WhoDidItClue.Either(s(0), p(0), p(1)))
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.CLUE_PROPAGATION,
                sources = listOf(WhoDidItSource.Clue(0)),
                changes = listOf(WhoDidItCellChange(s(0), p(2), WhoDidItMark.NO)),
            ),
            findCluePropagation(spec, fresh()),
        )
        val state = fresh()
        state.set(s(0), p(2), WhoDidItMark.NO)
        assertNull(findCluePropagation(spec, state))
    }

    @Test
    fun cluePropagationHasTraitRulesOutThirdCategoryEntry() {
        val spec = specWith(WhoDidItClue.HasTrait(s(0), WhoDidItTrait.PLACE_OUTDOOR))
        val state = fresh()
        state.set(i(1), p(1), WhoDidItMark.NO)
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.CLUE_PROPAGATION,
                sources = listOf(WhoDidItSource.Clue(0)),
                changes = listOf(WhoDidItCellChange(s(0), i(1), WhoDidItMark.NO)),
            ),
            findCluePropagation(spec, state),
        )
        assertNull(findCluePropagation(spec, fresh()))
    }

    @Test
    fun cluePropagationLacksTraitRulesOutThirdCategoryEntry() {
        val spec = specWith(WhoDidItClue.LacksTrait(s(0), WhoDidItTrait.PLACE_OUTDOOR))
        val state = fresh()
        state.set(i(0), p(0), WhoDidItMark.NO)
        state.set(i(0), p(2), WhoDidItMark.NO)
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.CLUE_PROPAGATION,
                sources = listOf(WhoDidItSource.Clue(0)),
                changes = listOf(WhoDidItCellChange(s(0), i(0), WhoDidItMark.NO)),
            ),
            findCluePropagation(spec, state),
        )
        assertNull(findCluePropagation(spec, fresh()))
    }

    @Test
    fun cluePropagationCrimeTraitRemovesCandidates() {
        val spec = specWith(WhoDidItClue.CrimeTrait(WhoDidItTrait.PLACE_OUTDOOR))
        val state = fresh()
        state.set(s(0), p(1), WhoDidItMark.NO)
        state.set(s(2), p(1), WhoDidItMark.NO)
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.CLUE_PROPAGATION,
                sources = listOf(WhoDidItSource.Clue(0)),
                culpritRemovals = setOf(0, 2),
            ),
            findCluePropagation(spec, state),
        )
        assertNull(findCluePropagation(spec, fresh()))
    }

    @Test
    fun cluePropagationCrimeOnEntryMarksNoForClearedSuspects() {
        val spec = specWith(WhoDidItClue.Same(s(0), p(1)), WhoDidItClue.Crime(i(1)))
        val state = fresh()
        state.culpritCandidates.removeAll(setOf(0, 1))
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.CLUE_PROPAGATION,
                sources = listOf(WhoDidItSource.Clue(1)),
                changes = listOf(
                    WhoDidItCellChange(s(0), i(1), WhoDidItMark.NO),
                    WhoDidItCellChange(s(1), i(1), WhoDidItMark.NO),
                ),
            ),
            findCluePropagation(spec, state),
        )
        val partly = fresh()
        partly.culpritCandidates.removeAll(setOf(0, 1))
        partly.set(s(0), i(1), WhoDidItMark.NO)
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.CLUE_PROPAGATION,
                sources = listOf(WhoDidItSource.Clue(1)),
                changes = listOf(WhoDidItCellChange(s(1), i(1), WhoDidItMark.NO)),
            ),
            findCluePropagation(spec, partly),
        )
    }

    @Test
    fun cluePropagationCrimeOnEntryHasNullCases() {
        assertNull(findCluePropagation(specWith(WhoDidItClue.Crime(i(1))), fresh()))
        val done = fresh()
        done.culpritCandidates.remove(0)
        done.set(s(0), i(1), WhoDidItMark.NO)
        assertNull(findCluePropagation(specWith(WhoDidItClue.Crime(i(1))), done))
        val onSuspect = fresh()
        onSuspect.culpritCandidates.retainAll(setOf(2))
        assertNull(findCluePropagation(specWith(WhoDidItClue.Crime(s(2))), onSuspect))
    }

    @Test
    fun cluePropagationCrimeTraitWithSoleCandidateMarksNoOnLackingEntries() {
        val spec = specWith(WhoDidItClue.CrimeTrait(WhoDidItTrait.ITEM_METAL))
        val state = fresh()
        state.culpritCandidates.retainAll(setOf(2))
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.CLUE_PROPAGATION,
                sources = listOf(WhoDidItSource.Clue(0)),
                changes = listOf(
                    WhoDidItCellChange(s(2), i(0), WhoDidItMark.NO),
                    WhoDidItCellChange(s(2), i(2), WhoDidItMark.NO),
                ),
            ),
            findCluePropagation(spec, state),
        )
        val place = fresh()
        place.culpritCandidates.retainAll(setOf(2))
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.CLUE_PROPAGATION,
                sources = listOf(WhoDidItSource.Clue(0)),
                changes = listOf(WhoDidItCellChange(s(2), p(1), WhoDidItMark.NO)),
            ),
            findCluePropagation(specWith(WhoDidItClue.CrimeTrait(WhoDidItTrait.PLACE_INDOOR)), place),
        )
    }

    @Test
    fun cluePropagationCrimeTraitWithSoleCandidateHasNullCases() {
        val spec = specWith(WhoDidItClue.CrimeTrait(WhoDidItTrait.ITEM_METAL))
        val two = fresh()
        two.culpritCandidates.remove(0)
        assertNull(findCluePropagation(spec, two))
        val done = fresh()
        done.culpritCandidates.retainAll(setOf(2))
        done.set(s(2), i(0), WhoDidItMark.NO)
        done.set(s(2), i(2), WhoDidItMark.NO)
        assertNull(findCluePropagation(spec, done))
        val suspectTrait = fresh()
        suspectTrait.culpritCandidates.retainAll(setOf(2))
        assertNull(findCluePropagation(specWith(WhoDidItClue.CrimeTrait(WhoDidItTrait.SUSPECT_TALL)), suspectTrait))
    }

    private fun statementSpec() = liarFixture().copy(clues = emptyList())

    @Test
    fun statementCheckRefutedStatementLeavesSpeakerAsSoleCandidate() {
        val state = fresh()
        state.set(s(2), i(0), WhoDidItMark.NO)
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.STATEMENT_CHECK,
                sources = listOf(WhoDidItSource.Statement(2)),
                culpritRemovals = setOf(0, 1),
            ),
            findStatementCheck(statementSpec(), state),
        )
    }

    @Test
    fun statementCheckConfirmedStatementClearsSpeaker() {
        val state = fresh()
        state.set(s(0), i(2), WhoDidItMark.YES)
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.STATEMENT_CHECK,
                sources = listOf(WhoDidItSource.Statement(0)),
                culpritRemovals = setOf(0),
            ),
            findStatementCheck(statementSpec(), state),
        )
    }

    @Test
    fun statementCheckUsesTraitCardsAndAscendingSpeakerOrder() {
        val spec = statementSpec().copy(
            statements = listOf(
                WhoDidItStatement(2, WhoDidItClue.Same(s(2), i(0))),
                WhoDidItStatement(1, WhoDidItClue.HasTrait(s(1), WhoDidItTrait.PLACE_INDOOR)),
            ),
        )
        val state = fresh()
        state.set(s(2), i(0), WhoDidItMark.NO)
        state.set(s(1), p(1), WhoDidItMark.NO)
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.STATEMENT_CHECK,
                sources = listOf(WhoDidItSource.Statement(1)),
                culpritRemovals = setOf(1),
            ),
            findStatementCheck(spec, state),
        )
    }

    @Test
    fun statementCheckHasNullCases() {
        assertNull(findStatementCheck(statementSpec(), fresh()))
        assertNull(findStatementCheck(easyFixture(), fresh()))
        val single = fresh()
        single.set(s(2), i(0), WhoDidItMark.NO)
        single.culpritCandidates.retainAll(setOf(2))
        assertNull(findStatementCheck(statementSpec(), single))
        val cleared = fresh()
        cleared.set(s(0), i(2), WhoDidItMark.YES)
        cleared.culpritCandidates.remove(0)
        assertNull(findStatementCheck(statementSpec(), cleared))
    }

    @Test
    fun propagationTierIncludesStatementCheck() {
        val state = fresh()
        state.set(s(2), i(0), WhoDidItMark.NO)
        val step = findPropagationStep(statementSpec(), state)
        assertEquals(WhoDidItTechnique.STATEMENT_CHECK, step?.technique)
        assertEquals(findStatementCheck(statementSpec(), state), step)
        assertEquals(WhoDidItTechnique.INCOMPATIBLE.ordinal + 1, WhoDidItTechnique.STATEMENT_CHECK.ordinal)
        assertEquals(WhoDidItTechnique.STATEMENT_CHECK.ordinal + 1, WhoDidItTechnique.LIAR_HYPOTHESIS.ordinal)
    }

    @Test
    fun incompatibleMarksNoWhenThirdCategoryOptionsDisjoint() {
        val state = fresh()
        state.set(s(0), p(1), WhoDidItMark.NO)
        state.set(s(0), p(2), WhoDidItMark.NO)
        state.set(i(0), p(0), WhoDidItMark.NO)
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.INCOMPATIBLE,
                changes = listOf(WhoDidItCellChange(s(0), i(0), WhoDidItMark.NO)),
            ),
            findIncompatible(easyFixture(), state),
        )
        assertNull(findIncompatible(easyFixture(), fresh()))
    }
}
