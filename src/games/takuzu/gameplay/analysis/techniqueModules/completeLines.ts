import {
  buildCompleteLinesNextMove,
} from '../content';
import {
  countValue,
  getColumn,
} from '../../../core';
import type { Grid, TakuzuNextMoveHint } from '../../../types';
import {
  buildLineCells,
  buildLineTargets,
  toHint,
  type CandidateMove,
} from '../helpers';

function findCompleteLineMove(board: Grid): CandidateMove | null {
  const size = board.length;
  const half = size / 2;

  for (let row = 0; row < size; row += 1) {
    const line = board[row];
    const zeroes = countValue(line, 0);
    const ones = countValue(line, 1);
    if (zeroes === half || ones === half) {
      const fillValue = zeroes === half ? 1 : 0;
      const col = line.findIndex((cell) => cell === null);
      if (col !== -1) {
        return {
          row,
          col,
          value: fillValue,
          ruleKey: 'complete-lines',
          lineKind: 'row',
          lineIndex: row,
        };
      }
    }
  }

  for (let col = 0; col < size; col += 1) {
    const line = getColumn(board, col);
    const zeroes = countValue(line, 0);
    const ones = countValue(line, 1);
    if (zeroes === half || ones === half) {
      const fillValue = zeroes === half ? 1 : 0;
      const row = line.findIndex((cell) => cell === null);
      if (row !== -1) {
        return {
          row,
          col,
          value: fillValue,
          ruleKey: 'complete-lines',
          lineKind: 'column',
          lineIndex: col,
        };
      }
    }
  }

  return null;
}

function buildCompleteLinesHint(board: Grid, move: CandidateMove): TakuzuNextMoveHint {
  const line = move.lineKind === 'row' ? board[move.lineIndex] : getColumn(board, move.lineIndex);
  const targetIndex = move.lineKind === 'row' ? move.col : move.row;

  const filledValue = move.value === 0 ? 1 : 0;
  const evidenceIndexes = line.flatMap((value, index) => (value === filledValue ? [index] : []));
  const clp = buildCompleteLinesNextMove({
    lineKind: move.lineKind,
    lineIndex: move.lineIndex,
    filledValue,
    filledCount: evidenceIndexes.length,
    targetValue: move.value,
    targetCount: 1,
  });

  return toHint({
    ruleKey: move.ruleKey,
    title: clp.title,
    body: clp.body,
    evidenceCells: buildLineCells(move.lineKind, move.lineIndex, evidenceIndexes),
    targetCells: buildLineTargets(move.lineKind, move.lineIndex, [targetIndex], move.value),
    highlightRows: move.lineKind === 'row' ? [move.lineIndex] : [],
    highlightCols: move.lineKind === 'column' ? [move.lineIndex] : [],
  });
}

export function completeLines(board: Grid): TakuzuNextMoveHint | null {
  const move = findCompleteLineMove(board);
  return move ? buildCompleteLinesHint(board, move) : null;
}
