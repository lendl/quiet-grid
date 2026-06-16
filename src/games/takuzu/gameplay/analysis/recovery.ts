import {
  buildAvoidTriosRepair,
  buildCompleteLinesRepair,
  buildEliminateFilledLinesRepair,
} from './content';
import { getColumn } from '../../core';
import type { Grid, TakuzuNextMoveHint } from '../../types';
import { buildLineCells, getLineIndexes } from './helpers';

function findTripleMismatch(board: Grid): TakuzuNextMoveHint | null {
  const size = board.length;

  for (let rowIndex = 0; rowIndex < size; rowIndex += 1) {
    for (let colIndex = 0; colIndex <= size - 3; colIndex += 1) {
      const first = board[rowIndex][colIndex];
      const second = board[rowIndex][colIndex + 1];
      const third = board[rowIndex][colIndex + 2];

      if (first === null || first !== second || second !== third) continue;

      const atr = buildAvoidTriosRepair({
        lineKind: 'row',
        lineIndex: rowIndex,
        repeatedValue: first,
      });

      return {
        kind: 'recovery',
        ruleKey: 'avoid-trios',
        title: atr.title,
        body: atr.body,
        evidenceCells: buildLineCells('row', rowIndex, [colIndex, colIndex + 1, colIndex + 2]),
        targetCells: [],
        highlightRows: [rowIndex],
        highlightCols: [],
      };
    }
  }

  for (let colIndex = 0; colIndex < size; colIndex += 1) {
    for (let rowIndex = 0; rowIndex <= size - 3; rowIndex += 1) {
      const first = board[rowIndex][colIndex];
      const second = board[rowIndex + 1][colIndex];
      const third = board[rowIndex + 2][colIndex];

      if (first === null || first !== second || second !== third) continue;

      const atr = buildAvoidTriosRepair({
        lineKind: 'column',
        lineIndex: colIndex,
        repeatedValue: first,
      });

      return {
        kind: 'recovery',
        ruleKey: 'avoid-trios',
        title: atr.title,
        body: atr.body,
        evidenceCells: buildLineCells('column', colIndex, [rowIndex, rowIndex + 1, rowIndex + 2]),
        targetCells: [],
        highlightRows: [],
        highlightCols: [colIndex],
      };
    }
  }

  return null;
}

function findBalanceMismatch(board: Grid): TakuzuNextMoveHint | null {
  const size = board.length;
  const limit = size / 2;

  for (let rowIndex = 0; rowIndex < size; rowIndex += 1) {
    const row = board[rowIndex];
    const zeroIndexes = row.flatMap((value, index) => (value === 0 ? [index] : []));
    const oneIndexes = row.flatMap((value, index) => (value === 1 ? [index] : []));

    if (zeroIndexes.length > limit) {
      const clr = buildCompleteLinesRepair({
        lineKind: 'row',
        lineIndex: rowIndex,
        filledValue: 0,
        filledCount: zeroIndexes.length,
        limit,
      });

      return {
        kind: 'recovery',
        ruleKey: 'complete-lines',
        title: clr.title,
        body: clr.body,
        evidenceCells: buildLineCells('row', rowIndex, zeroIndexes),
        targetCells: [],
        highlightRows: [rowIndex],
        highlightCols: [],
      };
    }

    if (oneIndexes.length > limit) {
      const clr = buildCompleteLinesRepair({
        lineKind: 'row',
        lineIndex: rowIndex,
        filledValue: 1,
        filledCount: oneIndexes.length,
        limit,
      });

      return {
        kind: 'recovery',
        ruleKey: 'complete-lines',
        title: clr.title,
        body: clr.body,
        evidenceCells: buildLineCells('row', rowIndex, oneIndexes),
        targetCells: [],
        highlightRows: [rowIndex],
        highlightCols: [],
      };
    }
  }

  for (let colIndex = 0; colIndex < size; colIndex += 1) {
    const column = getColumn(board, colIndex);
    const zeroIndexes = column.flatMap((value, index) => (value === 0 ? [index] : []));
    const oneIndexes = column.flatMap((value, index) => (value === 1 ? [index] : []));

    if (zeroIndexes.length > limit) {
      const clr = buildCompleteLinesRepair({
        lineKind: 'column',
        lineIndex: colIndex,
        filledValue: 0,
        filledCount: zeroIndexes.length,
        limit,
      });

      return {
        kind: 'recovery',
        ruleKey: 'complete-lines',
        title: clr.title,
        body: clr.body,
        evidenceCells: buildLineCells('column', colIndex, zeroIndexes),
        targetCells: [],
        highlightRows: [],
        highlightCols: [colIndex],
      };
    }

    if (oneIndexes.length > limit) {
      const clr = buildCompleteLinesRepair({
        lineKind: 'column',
        lineIndex: colIndex,
        filledValue: 1,
        filledCount: oneIndexes.length,
        limit,
      });

      return {
        kind: 'recovery',
        ruleKey: 'complete-lines',
        title: clr.title,
        body: clr.body,
        evidenceCells: buildLineCells('column', colIndex, oneIndexes),
        targetCells: [],
        highlightRows: [],
        highlightCols: [colIndex],
      };
    }
  }

  return null;
}

function findDuplicateMismatch(board: Grid): TakuzuNextMoveHint | null {
  const size = board.length;
  const allIndexes = getLineIndexes(size);
  const completedRows = board
    .map((row, rowIndex) => ({ row, rowIndex }))
    .filter(({ row }) => row.every((value) => value !== null));

  for (let firstIndex = 0; firstIndex < completedRows.length; firstIndex += 1) {
    for (let secondIndex = firstIndex + 1; secondIndex < completedRows.length; secondIndex += 1) {
      const firstRow = completedRows[firstIndex];
      const secondRow = completedRows[secondIndex];

      if (!firstRow.row.every((value, index) => value === secondRow.row[index])) continue;

      const efl = buildEliminateFilledLinesRepair({
        lineKind: 'row',
        firstLineIndex: firstRow.rowIndex,
        secondLineIndex: secondRow.rowIndex,
      });

      return {
        kind: 'recovery',
        ruleKey: 'eliminate-filled-lines',
        title: efl.title,
        body: efl.body,
        evidenceCells: [
          ...buildLineCells('row', firstRow.rowIndex, allIndexes),
          ...buildLineCells('row', secondRow.rowIndex, allIndexes),
        ],
        targetCells: [],
        highlightRows: [firstRow.rowIndex, secondRow.rowIndex],
        highlightCols: [],
      };
    }
  }

  const completedColumns = allIndexes
    .map((colIndex) => ({
      column: getColumn(board, colIndex),
      colIndex,
    }))
    .filter(({ column }) => column.every((value) => value !== null));

  for (let firstIndex = 0; firstIndex < completedColumns.length; firstIndex += 1) {
    for (let secondIndex = firstIndex + 1; secondIndex < completedColumns.length; secondIndex += 1) {
      const firstColumn = completedColumns[firstIndex];
      const secondColumn = completedColumns[secondIndex];

      if (!firstColumn.column.every((value, index) => value === secondColumn.column[index])) continue;

      const efl = buildEliminateFilledLinesRepair({
        lineKind: 'column',
        firstLineIndex: firstColumn.colIndex,
        secondLineIndex: secondColumn.colIndex,
      });

      return {
        kind: 'recovery',
        ruleKey: 'eliminate-filled-lines',
        title: efl.title,
        body: efl.body,
        evidenceCells: [
          ...buildLineCells('column', firstColumn.colIndex, allIndexes),
          ...buildLineCells('column', secondColumn.colIndex, allIndexes),
        ],
        targetCells: [],
        highlightRows: [],
        highlightCols: [firstColumn.colIndex, secondColumn.colIndex],
      };
    }
  }

  return null;
}

export function getTakuzuRecoveryHint(board: Grid): TakuzuNextMoveHint | null {
  return findTripleMismatch(board)
    ?? findBalanceMismatch(board)
    ?? findDuplicateMismatch(board);
}
