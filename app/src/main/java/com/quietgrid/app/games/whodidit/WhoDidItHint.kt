package com.quietgrid.app.games.whodidit

import com.quietgrid.engine.whodidit.WhoDidItMark
import com.quietgrid.engine.whodidit.WhoDidItStep
import com.quietgrid.engine.whodidit.WhoDidItTechnique
import com.quietgrid.engine.whodidit.nextWhoDidItHintPath
import com.quietgrid.engine.whodidit.toSpec

private val LEAD_TECHNIQUES = setOf(WhoDidItTechnique.STATEMENT_CHECK, WhoDidItTechnique.LIAR_HYPOTHESIS)

data class WhoDidItHint(val step: WhoDidItStep?, val hasWrongMarks: Boolean, val lead: WhoDidItStep? = null)

fun computeWhoDidItHint(session: WhoDidItSession): WhoDidItHint {
    val path = nextWhoDidItHintPath(session.puzzle.toSpec(), toWhoDidItGridState(session))
    return WhoDidItHint(
        step = path.step,
        hasWrongMarks = hasWhoDidItWrongMarks(session),
        lead = path.prelude.firstOrNull { it.technique in LEAD_TECHNIQUES },
    )
}

fun whoDidItHintCells(categoryCount: Int, step: WhoDidItStep?): Map<Triple<Int, Int, Int>, WhoDidItCellMark> =
    step?.changes.orEmpty().mapNotNull { change ->
        val preview = when (change.mark) {
            WhoDidItMark.YES -> WhoDidItCellMark.O
            WhoDidItMark.NO -> WhoDidItCellMark.X
            WhoDidItMark.UNKNOWN -> return@mapNotNull null
        }
        val cell = runCatching { whoDidItCellFor(categoryCount, change.a, change.b) }.getOrNull() ?: return@mapNotNull null
        cell to preview
    }.toMap()

fun whoDidItHintMark(step: WhoDidItStep): WhoDidItMark? =
    step.changes.map { it.mark }.distinct().singleOrNull()?.takeIf { it != WhoDidItMark.UNKNOWN }
