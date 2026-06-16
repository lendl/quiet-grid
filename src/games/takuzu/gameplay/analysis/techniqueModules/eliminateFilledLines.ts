import {
  buildEliminateFilledLinesNextMove,
} from '../content';
import {
  findEmptyIndexes,
  getColumn,
  otherValue,
} from '../../../core';
import type { Grid, TakuzuNextMoveHint } from '../../../types';
import {
  buildLineCells,
  buildLineTargets,
  getLineIndexes,
  toHint,
  type CandidateMove,
} from '../helpers';

function findEliminateFilledLinesRow(board: Grid): CandidateMove | null {
  const size = board.length;
  const completeRows = board
    .map((line, index) => ({ line, index }))
    .filter(({ line }) => line.every((cell) => cell !== null));

  for (let row = 0; row < size; row += 1) {
    const line = board[row];
    const emptyCols = line.flatMap((cell, col) => (cell === null ? [col] : []));
    if (emptyCols.length !== 2) {
      continue;
    }

    for (const complete of completeRows) {
      if (complete.index === row) {
        continue;
      }

      const matches = line.every((cell, col) => cell === null || cell === complete.line[col]);
      if (!matches) {
        continue;
      }

      const col = emptyCols[0];
      return {
        row,
        col,
        value: otherValue(complete.line[col] as 0 | 1),
        ruleKey: 'eliminate-filled-lines',
        lineKind: 'row',
        lineIndex: row,
        matchingLineIndex: complete.index,
      };
    }
  }

  return null;
}

function findEliminateFilledLinesColumn(board: Grid): CandidateMove | null {
  const size = board.length;
  const completeCols = Array.from({ length: size }, (_, index) => ({
    line: getColumn(board, index),
    index,
  })).filter(({ line }) => line.every((cell) => cell !== null));

  for (let col = 0; col < size; col += 1) {
    const line = getColumn(board, col);
    const emptyRows = line.flatMap((cell, row) => (cell === null ? [row] : []));
    if (emptyRows.length !== 2) {
      continue;
    }

    for (const complete of completeCols) {
      if (complete.index === col) {
        continue;
      }

      const matches = line.every((cell, row) => cell === null || cell === complete.line[row]);
      if (!matches) {
        continue;
      }

      const row = emptyRows[0];
      return {
        row,
        col,
        value: otherValue(complete.line[row] as 0 | 1),
        ruleKey: 'eliminate-filled-lines',
        lineKind: 'column',
        lineIndex: col,
        matchingLineIndex: complete.index,
      };
    }
  }

  return null;
}

function buildEliminateFilledLinesHint(board: Grid, move: CandidateMove): TakuzuNextMoveHint {
  const line = move.lineKind === 'row' ? board[move.lineIndex] : getColumn(board, move.lineIndex);
  const targetIndex = move.lineKind === 'row' ? move.col : move.row;
  const size = board.length;
  const allIndexes = getLineIndexes(size);
  const emptyIndexes = findEmptyIndexes(line);

  const efl = buildEliminateFilledLinesNextMove({
    lineKind: move.lineKind,
    lineIndex: move.lineIndex,
    matchingLineIndex: move.matchingLineIndex as number,
    targetValue: move.value,
    targetCount: 1,
  });

  return toHint({
    ruleKey: move.ruleKey,
    title: efl.title,
    body: efl.body,
    evidenceCells: [
      ...buildLineCells(
        move.lineKind,
        move.lineIndex,
        allIndexes.filter((index) => line[index] !== null && !emptyIndexes.includes(index)),
      ),
      ...buildLineCells(move.lineKind, move.matchingLineIndex as number, allIndexes),
    ],
    targetCells: buildLineTargets(move.lineKind, move.lineIndex, [targetIndex], move.value),
    highlightRows: move.lineKind === 'row' ? [move.lineIndex, move.matchingLineIndex as number] : [],
    highlightCols: move.lineKind === 'column' ? [move.lineIndex, move.matchingLineIndex as number] : [],
  });
}

export function eliminateFilledLines(board: Grid): TakuzuNextMoveHint | null {
  const move = findEliminateFilledLinesRow(board) ?? findEliminateFilledLinesColumn(board);
  return move ? buildEliminateFilledLinesHint(board, move) : null;
}
