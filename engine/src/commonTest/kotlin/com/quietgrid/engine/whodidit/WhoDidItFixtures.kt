package com.quietgrid.engine.whodidit

import com.quietgrid.engine.whodidit.WhoDidItTrait.*

private fun suspectRef(slot: Int) = WhoDidItRef(0, slot)
private fun placeRef(slot: Int) = WhoDidItRef(1, slot)
private fun itemRef(slot: Int) = WhoDidItRef(2, slot)

private val fixtureTraits: List<List<Set<WhoDidItTrait>>> = listOf(
    listOf(
        setOf(SUSPECT_TALL, SUSPECT_GLASSES),
        setOf(SUSPECT_SHORT, SUSPECT_HAT),
        setOf(SUSPECT_TALL, SUSPECT_SCARF),
    ),
    listOf(setOf(PLACE_INDOOR), setOf(PLACE_OUTDOOR), setOf(PLACE_INDOOR)),
    listOf(
        setOf(ITEM_WOOD, ITEM_HEAVY),
        setOf(ITEM_METAL, ITEM_LIGHT),
        setOf(ITEM_CLOTH, ITEM_LIGHT),
    ),
)

private val assignmentClues: List<WhoDidItClue> = listOf(
    WhoDidItClue.HasTrait(suspectRef(0), PLACE_OUTDOOR),
    WhoDidItClue.Same(suspectRef(1), placeRef(2)),
    WhoDidItClue.Same(suspectRef(0), itemRef(2)),
    WhoDidItClue.Same(suspectRef(1), itemRef(0)),
)

fun fixtureSolution() = WhoDidItSolution(
    slotOf = listOf(listOf(0, 1, 2), listOf(1, 2, 0), listOf(2, 0, 1)),
    culprit = 2,
)

fun easyFixture() = WhoDidItPuzzleSpec(
    size = 3,
    categoryCount = 3,
    traits = fixtureTraits,
    clues = assignmentClues + WhoDidItClue.Crime(itemRef(1)),
    statements = emptyList(),
)

fun ambiguousFixture() = easyFixture().let { it.copy(clues = it.clues.dropLast(1)) }

fun liarStatements(): List<WhoDidItStatement> = listOf(
    WhoDidItStatement(0, WhoDidItClue.Same(suspectRef(0), itemRef(2))),
    WhoDidItStatement(1, WhoDidItClue.NotSame(suspectRef(1), placeRef(0))),
    WhoDidItStatement(2, WhoDidItClue.Same(suspectRef(2), itemRef(0))),
)

fun liarFixture() = WhoDidItPuzzleSpec(
    size = 3,
    categoryCount = 3,
    traits = fixtureTraits,
    clues = assignmentClues,
    statements = liarStatements(),
)

fun expertSizedSolution() = WhoDidItSolution(
    slotOf = listOf(listOf(0, 1, 2, 3, 4), listOf(2, 0, 4, 1, 3), listOf(1, 3, 0, 4, 2), listOf(4, 2, 1, 0, 3)),
    culprit = 3,
)

fun expertSizedFixture(): WhoDidItPuzzleSpec {
    val ref = ::WhoDidItRef
    return WhoDidItPuzzleSpec(
        size = 5,
        categoryCount = 4,
        traits = listOf(
            listOf(
                setOf(SUSPECT_TALL, SUSPECT_GLASSES), setOf(SUSPECT_SHORT, SUSPECT_HAT),
                setOf(SUSPECT_TALL, SUSPECT_SCARF), setOf(SUSPECT_SHORT, SUSPECT_GLASSES),
                setOf(SUSPECT_TALL, SUSPECT_HAT),
            ),
            listOf(
                setOf(PLACE_INDOOR), setOf(PLACE_OUTDOOR), setOf(PLACE_INDOOR),
                setOf(PLACE_OUTDOOR), setOf(PLACE_INDOOR),
            ),
            listOf(
                setOf(ITEM_WOOD, ITEM_HEAVY), setOf(ITEM_METAL, ITEM_LIGHT), setOf(ITEM_CLOTH, ITEM_HEAVY),
                setOf(ITEM_METAL, ITEM_HEAVY), setOf(ITEM_WOOD, ITEM_LIGHT),
            ),
            List(5) { emptySet() },
        ),
        clues = listOf(
            WhoDidItClue.Same(ref(1, 1), ref(2, 4)),
            WhoDidItClue.Either(ref(2, 2), ref(0, 1), ref(0, 4)),
            WhoDidItClue.HasTrait(ref(1, 4), SUSPECT_SCARF),
            WhoDidItClue.LacksTrait(ref(0, 0), ITEM_HEAVY),
            WhoDidItClue.Either(ref(3, 0), ref(0, 3), ref(2, 0)),
            WhoDidItClue.HasTrait(ref(1, 2), SUSPECT_TALL),
            WhoDidItClue.Either(ref(2, 3), ref(0, 2), ref(0, 1)),
            WhoDidItClue.HasTrait(ref(3, 1), SUSPECT_SCARF),
            WhoDidItClue.Either(ref(2, 2), ref(1, 3), ref(3, 2)),
            WhoDidItClue.HasTrait(ref(1, 3), ITEM_CLOTH),
            WhoDidItClue.LacksTrait(ref(3, 4), ITEM_HEAVY),
            WhoDidItClue.NotSame(ref(0, 0), ref(2, 2)),
            WhoDidItClue.Crime(ref(2, 4)),
        ),
        statements = listOf(
            WhoDidItStatement(0, WhoDidItClue.NotSame(ref(0, 0), ref(1, 0))),
            WhoDidItStatement(1, WhoDidItClue.Same(ref(0, 1), ref(2, 3))),
            WhoDidItStatement(2, WhoDidItClue.NotSame(ref(0, 2), ref(2, 1))),
            WhoDidItStatement(3, WhoDidItClue.Same(ref(0, 3), ref(1, 0))),
            WhoDidItStatement(4, WhoDidItClue.NotSame(ref(0, 4), ref(1, 2))),
        ),
    )
}

fun contradictionSolution() = WhoDidItSolution(
    slotOf = listOf(listOf(0, 1, 2), listOf(2, 0, 1), listOf(1, 2, 0)),
    culprit = 1,
)

fun contradictionFixture(): WhoDidItPuzzleSpec {
    val ref = ::WhoDidItRef
    return WhoDidItPuzzleSpec(
        size = 3,
        categoryCount = 3,
        traits = fixtureTraits,
        clues = listOf(
            WhoDidItClue.Either(ref(0, 2), ref(1, 1), ref(1, 2)),
            WhoDidItClue.Either(ref(2, 0), ref(1, 0), ref(1, 1)),
            WhoDidItClue.NotSame(ref(0, 1), ref(2, 0)),
            WhoDidItClue.Either(ref(2, 1), ref(1, 1), ref(1, 2)),
            WhoDidItClue.NotSame(ref(0, 1), ref(1, 1)),
            WhoDidItClue.Either(ref(0, 0), ref(2, 0), ref(2, 1)),
            WhoDidItClue.NotSame(ref(0, 2), ref(2, 1)),
            WhoDidItClue.Either(ref(1, 1), ref(2, 0), ref(2, 1)),
            WhoDidItClue.Crime(ref(1, 0)),
        ),
        statements = emptyList(),
    )
}
