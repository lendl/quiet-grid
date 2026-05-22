import type { CellValue, Grid } from './types';
import {
  runHumanBranchProof,
  type HumanProofContradictionKind,
  type HumanProofRule,
} from './humanProof';

export function otherValue(value: 0 | 1): 0 | 1 {
  return value === 0 ? 1 : 0;
}

export function countValue(line: readonly CellValue[], value: 0 | 1): number {
  return line.filter((cell) => cell === value).length;
}

export function countEmpties(line: readonly CellValue[]): number {
  return line.filter((cell) => cell === null).length;
}

export function getColumn(board: Grid, colIndex: number): CellValue[] {
  return board.map((row) => row[colIndex]);
}

export function findEmptyIndexes(line: readonly CellValue[]): number[] {
  return line.flatMap((value, index) => (value === null ? [index] : []));
}

export function noThreeConsec(line: readonly CellValue[]): boolean {
  for (let index = 0; index <= line.length - 3; index += 1) {
    const first = line[index];
    const second = line[index + 1];
    const third = line[index + 2];

    if (first !== null && first === second && second === third) {
      return false;
    }
  }

  return true;
}

export function hasTriple(line: readonly CellValue[]): boolean {
  return !noThreeConsec(line);
}

export function equalHalvesFeasible(line: readonly CellValue[], size: number): boolean {
  const half = size / 2;
  return countValue(line, 0) <= half && countValue(line, 1) <= half;
}

export function isLegalCell(grid: Grid, row: number, col: number, size: number): boolean {
  const currentRow = grid[row];
  const currentCol = getColumn(grid, col);

  return (
    noThreeConsec(currentRow) &&
    noThreeConsec(currentCol) &&
    equalHalvesFeasible(currentRow, size) &&
    equalHalvesFeasible(currentCol, size)
  );
}

export function hasUniqueLines(grid: (0 | 1)[][]): boolean {
  const size = grid.length;
  const rowStrings = new Set(grid.map((row) => row.join('')));
  if (rowStrings.size !== size) {
    return false;
  }

  const columnStrings = new Set(
    Array.from({ length: size }, (_, colIndex) => grid.map((row) => row[colIndex]).join('')),
  );

  return columnStrings.size === size;
}

export function countSolutions(puzzle: Grid, maxCount = 2): number {
  const size = puzzle.length;
  const grid = puzzle.map((row) => [...row]) as Grid;
  let count = 0;

  function solve(pos: number): void {
    if (count >= maxCount) {
      return;
    }

    if (pos === size * size) {
      if (hasUniqueLines(grid as (0 | 1)[][])) {
        count += 1;
      }
      return;
    }

    const row = Math.floor(pos / size);
    const col = pos % size;

    if (grid[row][col] !== null) {
      solve(pos + 1);
      return;
    }

    for (const value of [0, 1] as const) {
      grid[row][col] = value;
      if (isLegalCell(grid, row, col, size)) {
        solve(pos + 1);
      }
      grid[row][col] = null;
      if (count >= maxCount) {
        return;
      }
    }
  }

  solve(0);
  return count;
}

export function lineWouldDuplicateCompleteLine(
  line: readonly CellValue[],
  completeLines: readonly (readonly CellValue[])[],
): boolean {
  return completeLines.some((otherLine) => otherLine.every((cell, index) => cell === line[index]));
}

export function isCandidateLegal(board: Grid, row: number, col: number, value: 0 | 1): boolean {
  const size = board.length;
  board[row][col] = value;

  if (!isLegalCell(board, row, col, size)) {
    board[row][col] = null;
    return false;
  }

  const rowValues = board[row];
  if (rowValues.every((cell) => cell !== null)) {
    const otherRows = board.filter((line, index) => index !== row && line.every((cell) => cell !== null));
    if (lineWouldDuplicateCompleteLine(rowValues, otherRows)) {
      board[row][col] = null;
      return false;
    }
  }

  const colValues = getColumn(board, col);
  if (colValues.every((cell) => cell !== null)) {
    const otherColumns = Array.from({ length: size }, (_, index) => getColumn(board, index))
      .filter((line, index) => index !== col && line.every((cell) => cell !== null));
    if (lineWouldDuplicateCompleteLine(colValues, otherColumns)) {
      board[row][col] = null;
      return false;
    }
  }

  board[row][col] = null;
  return true;
}

export function countSolutionsForCandidate(board: Grid, row: number, col: number, value: 0 | 1): number {
  board[row][col] = value;
  const snapshot = board.map((line) => [...line]) as Grid;
  board[row][col] = null;
  return countSolutions(snapshot);
}

export function isValidCompletedLine(line: readonly (0 | 1)[]): boolean {
  const half = line.length / 2;
  if (countValue(line, 0) !== half || countValue(line, 1) !== half) {
    return false;
  }

  return noThreeConsec(line);
}

const validLinesBySize = new Map<number, (0 | 1)[][]>();

function getValidCompletedLines(size: number): (0 | 1)[][] {
  const cached = validLinesBySize.get(size);
  if (cached) {
    return cached;
  }

  const lines: (0 | 1)[][] = [];
  const totalMasks = 1 << size;

  for (let mask = 0; mask < totalMasks; mask += 1) {
    const line = Array.from(
      { length: size },
      (_, index) => ((mask >> (size - index - 1)) & 1) as 0 | 1,
    );

    if (isValidCompletedLine(line)) {
      lines.push(line);
    }
  }

  validLinesBySize.set(size, lines);
  return lines;
}

export function countValidLineCompletions(line: readonly CellValue[]): number {
  return getValidCompletedLines(line.length).filter((candidate) =>
    line.every((cell, index) => cell === null || candidate[index] === cell),
  ).length;
}

export interface ImpossibleCombinationInsight {
  row: number;
  col: number;
  forcedValue: 0 | 1;
  blockedValue: 0 | 1;
  lineKind: 'row' | 'column';
  lineIndex: number;
  validCompletionCount: number;
  contradictionKind: HumanProofContradictionKind;
  contradictionLineKind: 'row' | 'column';
  contradictionLineIndex: number;
  proofStepCount: number;
  proofUsesRule: HumanProofRule | null;
}

export function getImpossibleCombinationInsight(
  board: Grid,
  row: number,
  col: number,
): ImpossibleCombinationInsight | null {
  if (board[row]?.[col] !== null) {
    return null;
  }

  // Count completions before placing forcedValue so difficulty reflects ambiguity
  // at deduction time. On ties, prefer row to keep hint output deterministic.
  const rowCompletionCount = countValidLineCompletions(board[row]);
  const columnCompletionCount = countValidLineCompletions(getColumn(board, col));
  const lineKind = rowCompletionCount <= columnCompletionCount ? 'row' : 'column';
  const validCompletionCount = lineKind === 'row' ? rowCompletionCount : columnCompletionCount;

  const zeroBoard = board.map((line) => [...line]) as Grid;
  zeroBoard[row][col] = 0;
  const zeroProof = runHumanBranchProof(zeroBoard);

  const oneBoard = board.map((line) => [...line]) as Grid;
  oneBoard[row][col] = 1;
  const oneProof = runHumanBranchProof(oneBoard);

  const zeroDead = zeroProof.kind === 'contradiction';
  const oneDead = oneProof.kind === 'contradiction';

  if (zeroDead === oneDead) {
    return null;
  }

  const forcedValue: 0 | 1 = zeroDead ? 1 : 0;
  const blockedValue = otherValue(forcedValue);
  const blockedProof = zeroDead ? zeroProof : oneProof;
  const contradiction = blockedProof.contradiction;
  if (!contradiction || blockedProof.steps.length === 0) {
    return null;
  }

  return {
    row,
    col,
    forcedValue,
    blockedValue,
    lineKind,
    lineIndex: lineKind === 'row' ? row : col,
    validCompletionCount,
    contradictionKind: contradiction.kind,
    contradictionLineKind: contradiction.lineKind,
    contradictionLineIndex: contradiction.lineIndex,
    proofStepCount: blockedProof.steps.length,
    proofUsesRule: blockedProof.steps[0]?.rule ?? null,
  };
}

export function findPairMoveInLine(line: readonly CellValue[]): { index: number; value: 0 | 1 } | null {
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

export function findAvoidTrioMoveInLine(line: readonly CellValue[]): { index: number; value: 0 | 1 } | null {
  for (let index = 0; index <= line.length - 3; index += 1) {
    const [first, second, third] = [line[index], line[index + 1], line[index + 2]];
    if (first !== null && first === third && second === null) {
      return { index: index + 1, value: otherValue(first) };
    }
  }

  return null;
}
