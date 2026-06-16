import {
  buildEliminateImpossibleCombinationsNextMove,
} from '../content';
import {
  getImpossibleCombinationInsight,
} from '../../../core';
import type { Grid, TakuzuNextMoveHint } from '../../../types';
import {
  buildLineCells,
  buildLineTargets,
  getLine,
  toHint,
  type CandidateMove,
} from '../helpers';

function findImpossibleCombinationMove(board: Grid): CandidateMove | null {
  const size = board.length;

  for (let row = 0; row < size; row += 1) {
    for (let col = 0; col < size; col += 1) {
      const insight = getImpossibleCombinationInsight(board, row, col);
      if (insight) {
        return {
          row: insight.row,
          col: insight.col,
          value: insight.forcedValue,
          ruleKey: 'eliminate-impossible-combinations',
          lineKind: insight.lineKind,
          lineIndex: insight.lineIndex,
          impossibleCombinationInsight: insight,
        };
      }
    }
  }

  return null;
}

function buildEliminateImpossibleCombinationsHint(board: Grid, move: CandidateMove): TakuzuNextMoveHint {
  const insight = move.impossibleCombinationInsight;
  if (!insight) {
    throw new Error('Missing impossible combination insight for progress hint.');
  }

  const eic = buildEliminateImpossibleCombinationsNextMove({
    lineKind: insight.lineKind,
    lineIndex: insight.lineIndex,
    validCompletionCount: insight.validCompletionCount,
    blockedValue: insight.blockedValue,
    targetValue: insight.forcedValue,
    proofStepCount: insight.proofStepCount,
    proofUsesRule: insight.proofUsesRule,
    contradictionKind: insight.contradictionKind,
    contradictionLineKind: insight.contradictionLineKind,
    contradictionLineIndex: insight.contradictionLineIndex,
  });

  return toHint({
    ruleKey: move.ruleKey,
    title: eic.title,
    body: eic.body,
    evidenceCells: buildLineCells(
      insight.lineKind,
      insight.lineIndex,
      getLine(board, insight.lineKind, insight.lineIndex)
        .flatMap((value, index) => (value !== null ? [index] : [])),
    ),
    targetCells: buildLineTargets(
      insight.lineKind,
      insight.lineIndex,
      [insight.lineKind === 'row' ? insight.col : insight.row],
      insight.forcedValue,
    ),
    highlightRows: insight.lineKind === 'row' ? [insight.lineIndex] : [],
    highlightCols: insight.lineKind === 'column' ? [insight.lineIndex] : [],
  });
}

export function eliminateImpossibleCombinations(board: Grid): TakuzuNextMoveHint | null {
  const move = findImpossibleCombinationMove(board);
  return move ? buildEliminateImpossibleCombinationsHint(board, move) : null;
}
