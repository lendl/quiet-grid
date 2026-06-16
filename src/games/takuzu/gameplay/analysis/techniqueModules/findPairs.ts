import {
  buildFindPairsNextMove,
} from '../content';
import {
  findPairMoveInLine,
  getColumn,
} from '../../../core';
import type { Grid, TakuzuNextMoveHint } from '../../../types';
import {
  buildLineCells,
  buildLineTargets,
  toHint,
  type CandidateMove,
} from '../helpers';

function findPairMove(board: Grid): CandidateMove | null {
  const size = board.length;

  for (let row = 0; row < size; row += 1) {
    const move = findPairMoveInLine(board[row]);
    if (move) {
      return {
        row,
        col: move.index,
        value: move.value,
        ruleKey: 'find-pairs',
        lineKind: 'row',
        lineIndex: row,
      };
    }
  }

  for (let col = 0; col < size; col += 1) {
    const move = findPairMoveInLine(getColumn(board, col));
    if (move) {
      return {
        row: move.index,
        col,
        value: move.value,
        ruleKey: 'find-pairs',
        lineKind: 'column',
        lineIndex: col,
      };
    }
  }

  return null;
}

function buildFindPairsHint(board: Grid, move: CandidateMove): TakuzuNextMoveHint {
  const line = move.lineKind === 'row' ? board[move.lineIndex] : getColumn(board, move.lineIndex);
  const targetIndex = move.lineKind === 'row' ? move.col : move.row;

  const repeatedIndexes =
    targetIndex + 2 < line.length
    && line[targetIndex + 1] !== null
    && line[targetIndex + 1] === line[targetIndex + 2]
      ? [targetIndex + 1, targetIndex + 2]
      : [targetIndex - 2, targetIndex - 1];
  const repeatedValue = line[repeatedIndexes[0]] as 0 | 1;
  const fp = buildFindPairsNextMove({
    lineKind: move.lineKind,
    lineIndex: move.lineIndex,
    repeatedValue,
    targetValue: move.value,
    targetCount: 1,
  });

  return toHint({
    ruleKey: move.ruleKey,
    title: fp.title,
    body: fp.body,
    evidenceCells: buildLineCells(move.lineKind, move.lineIndex, repeatedIndexes),
    targetCells: buildLineTargets(move.lineKind, move.lineIndex, [targetIndex], move.value),
    highlightRows: move.lineKind === 'row' ? [move.lineIndex] : [],
    highlightCols: move.lineKind === 'column' ? [move.lineIndex] : [],
  });
}

export function findPairs(board: Grid): TakuzuNextMoveHint | null {
  const move = findPairMove(board);
  return move ? buildFindPairsHint(board, move) : null;
}
