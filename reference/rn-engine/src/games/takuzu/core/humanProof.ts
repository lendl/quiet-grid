import type { CellValue, Grid } from './types';

export type HumanProofRule =
  | 'find-pairs'
  | 'avoid-trios'
  | 'complete-lines'
  | 'eliminate-filled-lines';

export type HumanProofContradictionKind =
  | 'triple'
  | 'balance'
  | 'duplicate-line';

export interface HumanProofStep {
  rule: HumanProofRule;
  row: number;
  col: number;
  value: 0 | 1;
}

export interface HumanProofContradiction {
  kind: HumanProofContradictionKind;
  lineKind: 'row' | 'column';
  lineIndex: number;
}

export interface HumanBranchProof {
  kind: 'contradiction' | 'stalled';
  steps: HumanProofStep[];
  contradiction?: HumanProofContradiction;
}

function otherValue(value: 0 | 1): 0 | 1 {
  return value === 0 ? 1 : 0;
}

function countValue(line: readonly CellValue[], value: 0 | 1): number {
  return line.filter((cell) => cell === value).length;
}

function getColumn(board: Grid, colIndex: number): CellValue[] {
  return board.map((row) => row[colIndex]);
}

function lineWouldDuplicateCompleteLine(
  line: readonly CellValue[],
  completeLines: readonly (readonly CellValue[])[],
): boolean {
  return completeLines.some((otherLine) => otherLine.every((cell, index) => cell === line[index]));
}

function findPairMoveInLine(line: readonly CellValue[]): { index: number; value: 0 | 1 } | null {
  for (let index = 0; index <= line.length - 3; index += 1) {
    const [first, second, third] = [line[index], line[index + 1], line[index + 2]];
    if (first !== null && first === second && third === null) {
      return { index: index + 2, value: otherValue(first) };
    }

    if (first === null && second !== null && second === third) {
      return { index, value: otherValue(second) };
    }
  }

  return null;
}

function findAvoidTrioMoveInLine(line: readonly CellValue[]): { index: number; value: 0 | 1 } | null {
  for (let index = 0; index <= line.length - 3; index += 1) {
    const [first, second, third] = [line[index], line[index + 1], line[index + 2]];
    if (first !== null && first === third && second === null) {
      return { index: index + 1, value: otherValue(first) };
    }
  }

  return null;
}

function findEliminateFilledLinesRow(board: Grid): HumanProofStep | null {
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
        rule: 'eliminate-filled-lines',
        row,
        col,
        value: otherValue(complete.line[col] as 0 | 1),
      };
    }
  }

  return null;
}

function findEliminateFilledLinesColumn(board: Grid): HumanProofStep | null {
  const size = board.length;
  const completeColumns = Array.from({ length: size }, (_, index) => ({
    line: getColumn(board, index),
    index,
  })).filter(({ line }) => line.every((cell) => cell !== null));

  for (let col = 0; col < size; col += 1) {
    const line = getColumn(board, col);
    const emptyRows = line.flatMap((cell, row) => (cell === null ? [row] : []));
    if (emptyRows.length !== 2) {
      continue;
    }

    for (const complete of completeColumns) {
      if (complete.index === col) {
        continue;
      }

      const matches = line.every((cell, row) => cell === null || cell === complete.line[row]);
      if (!matches) {
        continue;
      }

      const row = emptyRows[0];
      return {
        rule: 'eliminate-filled-lines',
        row,
        col,
        value: otherValue(complete.line[row] as 0 | 1),
      };
    }
  }

  return null;
}

function detectTriple(board: Grid): HumanProofContradiction | null {
  const size = board.length;

  for (let row = 0; row < size; row += 1) {
    for (let col = 0; col <= size - 3; col += 1) {
      const first = board[row][col];
      const second = board[row][col + 1];
      const third = board[row][col + 2];
      if (first !== null && first === second && second === third) {
        return { kind: 'triple', lineKind: 'row', lineIndex: row };
      }
    }
  }

  for (let col = 0; col < size; col += 1) {
    for (let row = 0; row <= size - 3; row += 1) {
      const first = board[row][col];
      const second = board[row + 1][col];
      const third = board[row + 2][col];
      if (first !== null && first === second && second === third) {
        return { kind: 'triple', lineKind: 'column', lineIndex: col };
      }
    }
  }

  return null;
}

function detectBalanceOverflow(board: Grid): HumanProofContradiction | null {
  const size = board.length;
  const half = size / 2;

  for (let row = 0; row < size; row += 1) {
    const line = board[row];
    if (countValue(line, 0) > half || countValue(line, 1) > half) {
      return { kind: 'balance', lineKind: 'row', lineIndex: row };
    }
  }

  for (let col = 0; col < size; col += 1) {
    const line = getColumn(board, col);
    if (countValue(line, 0) > half || countValue(line, 1) > half) {
      return { kind: 'balance', lineKind: 'column', lineIndex: col };
    }
  }

  return null;
}

function detectDuplicateCompletedLine(board: Grid): HumanProofContradiction | null {
  const size = board.length;

  const completeRows = board
    .map((line, index) => ({ line, index }))
    .filter(({ line }) => line.every((cell) => cell !== null));

  for (const completeRow of completeRows) {
    const otherRows = completeRows
      .filter(({ index }) => index !== completeRow.index)
      .map(({ line }) => line);

    if (lineWouldDuplicateCompleteLine(completeRow.line, otherRows)) {
      return { kind: 'duplicate-line', lineKind: 'row', lineIndex: completeRow.index };
    }
  }

  const completeColumns = Array.from({ length: size }, (_, index) => ({
    line: getColumn(board, index),
    index,
  })).filter(({ line }) => line.every((cell) => cell !== null));

  for (const completeColumn of completeColumns) {
    const otherColumns = completeColumns
      .filter(({ index }) => index !== completeColumn.index)
      .map(({ line }) => line);

    if (lineWouldDuplicateCompleteLine(completeColumn.line, otherColumns)) {
      return { kind: 'duplicate-line', lineKind: 'column', lineIndex: completeColumn.index };
    }
  }

  return null;
}

function findHumanProofStep(board: Grid): HumanProofStep | null {
  const size = board.length;
  const half = size / 2;

  for (let row = 0; row < size; row += 1) {
    const move = findPairMoveInLine(board[row]);
    if (move) {
      return { rule: 'find-pairs', row, col: move.index, value: move.value };
    }
  }

  for (let col = 0; col < size; col += 1) {
    const move = findPairMoveInLine(getColumn(board, col));
    if (move) {
      return { rule: 'find-pairs', row: move.index, col, value: move.value };
    }
  }

  for (let row = 0; row < size; row += 1) {
    const move = findAvoidTrioMoveInLine(board[row]);
    if (move) {
      return { rule: 'avoid-trios', row, col: move.index, value: move.value };
    }
  }

  for (let col = 0; col < size; col += 1) {
    const move = findAvoidTrioMoveInLine(getColumn(board, col));
    if (move) {
      return { rule: 'avoid-trios', row: move.index, col, value: move.value };
    }
  }

  for (let row = 0; row < size; row += 1) {
    const line = board[row];
    const zeroes = countValue(line, 0);
    const ones = countValue(line, 1);
    if (zeroes === half || ones === half) {
      const col = line.findIndex((cell) => cell === null);
      if (col !== -1) {
        return { rule: 'complete-lines', row, col, value: zeroes === half ? 1 : 0 };
      }
    }
  }

  for (let col = 0; col < size; col += 1) {
    const line = getColumn(board, col);
    const zeroes = countValue(line, 0);
    const ones = countValue(line, 1);
    if (zeroes === half || ones === half) {
      const row = line.findIndex((cell) => cell === null);
      if (row !== -1) {
        return { rule: 'complete-lines', row, col, value: zeroes === half ? 1 : 0 };
      }
    }
  }

  const eliminateFilledLinesRowMove = findEliminateFilledLinesRow(board);
  if (eliminateFilledLinesRowMove) {
    return eliminateFilledLinesRowMove;
  }

  const eliminateFilledLinesColumnMove = findEliminateFilledLinesColumn(board);
  if (eliminateFilledLinesColumnMove) {
    return eliminateFilledLinesColumnMove;
  }

  return null;
}

function applyProofStep(board: Grid, step: HumanProofStep): void {
  board[step.row][step.col] = step.value;
}

export function runHumanBranchProof(board: Grid): HumanBranchProof {
  const working = board.map((row) => [...row]) as Grid;
  const steps: HumanProofStep[] = [];

  while (true) {
    const contradiction =
      detectTriple(working) ??
      detectBalanceOverflow(working) ??
      detectDuplicateCompletedLine(working);
    if (contradiction) {
      return { kind: 'contradiction', steps, contradiction };
    }

    const step = findHumanProofStep(working);
    if (!step) {
      return { kind: 'stalled', steps };
    }

    applyProofStep(working, step);
    steps.push(step);
  }
}
