import {
  buildAvoidTriosNextMove,
} from '../content';
import {
  findAvoidTrioMoveInLine,
  getColumn,
} from '../../../core';
import type { Grid, TakuzuNextMoveHint } from '../../../types';
import {
  buildLineCells,
  buildLineTargets,
  toHint,
  type CandidateMove,
} from '../helpers';

function findAvoidTrioMove(board: Grid): CandidateMove | null {
  const size = board.length;

  for (let row = 0; row < size; row += 1) {
    const move = findAvoidTrioMoveInLine(board[row]);
    if (move) {
      return {
        row,
        col: move.index,
        value: move.value,
        ruleKey: 'avoid-trios',
        lineKind: 'row',
        lineIndex: row,
      };
    }
  }

  for (let col = 0; col < size; col += 1) {
    const move = findAvoidTrioMoveInLine(getColumn(board, col));
    if (move) {
      return {
        row: move.index,
        col,
        value: move.value,
        ruleKey: 'avoid-trios',
        lineKind: 'column',
        lineIndex: col,
      };
    }
  }

  return null;
}

function buildAvoidTriosHint(board: Grid, move: CandidateMove): TakuzuNextMoveHint {
  const line = move.lineKind === 'row' ? board[move.lineIndex] : getColumn(board, move.lineIndex);
  const targetIndex = move.lineKind === 'row' ? move.col : move.row;

  const repeatedIndexes = [targetIndex - 1, targetIndex + 1];
  const repeatedValue = line[repeatedIndexes[0]] as 0 | 1;
  const atr = buildAvoidTriosNextMove({
    lineKind: move.lineKind,
    lineIndex: move.lineIndex,
    repeatedValue,
    targetValue: move.value,
  });

  return toHint({
    ruleKey: move.ruleKey,
    title: atr.title,
    body: atr.body,
    evidenceCells: buildLineCells(move.lineKind, move.lineIndex, repeatedIndexes),
    targetCells: buildLineTargets(move.lineKind, move.lineIndex, [targetIndex], move.value),
    highlightRows: move.lineKind === 'row' ? [move.lineIndex] : [],
    highlightCols: move.lineKind === 'column' ? [move.lineIndex] : [],
  });
}

export function avoidTrios(board: Grid): TakuzuNextMoveHint | null {
  const move = findAvoidTrioMove(board);
  return move ? buildAvoidTriosHint(board, move) : null;
}
