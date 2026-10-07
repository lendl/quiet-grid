package com.quietgrid.engine.whodidit

private fun traitHeldBy(spec: WhoDidItPuzzleSpec, solution: WhoDidItSolution, suspect: Int, trait: WhoDidItTrait): Boolean {
    val category = trait.category.ordinal
    val slot = solution.slotOf.getOrNull(category)?.getOrNull(suspect) ?: return false
    return spec.traits.getOrNull(category)?.getOrNull(slot)?.contains(trait) == true
}

fun WhoDidItClue.isTrue(spec: WhoDidItPuzzleSpec, solution: WhoDidItSolution): Boolean = when (this) {
    is WhoDidItClue.Same -> solution.ownerOf(a) == solution.ownerOf(b)
    is WhoDidItClue.NotSame -> solution.ownerOf(a) != solution.ownerOf(b)
    is WhoDidItClue.Either -> (solution.ownerOf(a) == solution.ownerOf(b)) != (solution.ownerOf(a) == solution.ownerOf(c))
    is WhoDidItClue.HasTrait -> traitHeldBy(spec, solution, solution.ownerOf(a), trait)
    is WhoDidItClue.LacksTrait -> !traitHeldBy(spec, solution, solution.ownerOf(a), trait)
    is WhoDidItClue.Crime -> solution.ownerOf(e) == solution.culprit
    is WhoDidItClue.CrimeTrait -> traitHeldBy(spec, solution, solution.culprit, trait)
}

fun WhoDidItClue.negated(): WhoDidItClue = when (this) {
    is WhoDidItClue.Same -> WhoDidItClue.NotSame(a, b)
    is WhoDidItClue.NotSame -> WhoDidItClue.Same(a, b)
    is WhoDidItClue.HasTrait -> WhoDidItClue.LacksTrait(a, trait)
    is WhoDidItClue.LacksTrait -> WhoDidItClue.HasTrait(a, trait)
    else -> throw IllegalArgumentException("clue type $type cannot be negated")
}

fun isConsistent(spec: WhoDidItPuzzleSpec, solution: WhoDidItSolution): Boolean =
    spec.clues.all { it.isTrue(spec, solution) } &&
        spec.statements.all { it.clue.isTrue(spec, solution) == (it.s != solution.culprit) }
