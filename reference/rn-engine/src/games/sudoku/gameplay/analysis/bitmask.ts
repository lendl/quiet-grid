import type { SudokuBoard, SudokuDigit, SudokuSession } from '../../types';
import { sudokuDigits, SUDOKU_BOX_SIZE, SUDOKU_SIZE } from '../../types';

export const FULL_MASK = 0b111111111;

export const digitToBit = Array.from({ length: 10 }, (_, digit) => (
  digit >= 1 && digit <= 9 ? (1 << (digit - 1)) : 0
));

export const popcount = Array.from({ length: 512 }, (_, mask) => {
  let remaining = mask;
  let count = 0;
  while (remaining > 0) {
    remaining &= remaining - 1;
    count += 1;
  }
  return count;
});

export const bitToDigit = Array.from({ length: 512 }, (_, mask) => (
  popcount[mask] === 1 ? (Math.log2(mask) + 1) : 0
));

export function getCellIndex(row: number, col: number): number {
  return (row * SUDOKU_SIZE) + col;
}

export function getCellRow(index: number): number {
  return Math.floor(index / SUDOKU_SIZE);
}

export function getCellCol(index: number): number {
  return index % SUDOKU_SIZE;
}

export function getCellBox(row: number, col: number): number {
  return (Math.floor(row / SUDOKU_BOX_SIZE) * SUDOKU_BOX_SIZE) + Math.floor(col / SUDOKU_BOX_SIZE);
}

export const rowCellIndexes = Array.from({ length: SUDOKU_SIZE }, (_, row) => (
  Array.from({ length: SUDOKU_SIZE }, (_, col) => getCellIndex(row, col))
));

export const columnCellIndexes = Array.from({ length: SUDOKU_SIZE }, (_, col) => (
  Array.from({ length: SUDOKU_SIZE }, (_, row) => getCellIndex(row, col))
));

export const boxCellIndexes = Array.from({ length: SUDOKU_SIZE }, (_, box) => {
  const rowStart = Math.floor(box / SUDOKU_BOX_SIZE) * SUDOKU_BOX_SIZE;
  const colStart = (box % SUDOKU_BOX_SIZE) * SUDOKU_BOX_SIZE;
  return Array.from({ length: SUDOKU_SIZE }, (_, offset) => (
    getCellIndex(
      rowStart + Math.floor(offset / SUDOKU_BOX_SIZE),
      colStart + (offset % SUDOKU_BOX_SIZE),
    )
  ));
});

export const cellRowIndexes = Array.from({ length: SUDOKU_SIZE * SUDOKU_SIZE }, (_, index) => getCellRow(index));
export const cellColIndexes = Array.from({ length: SUDOKU_SIZE * SUDOKU_SIZE }, (_, index) => getCellCol(index));
export const cellBoxIndexes = Array.from({ length: SUDOKU_SIZE * SUDOKU_SIZE }, (_, index) => (
  getCellBox(cellRowIndexes[index], cellColIndexes[index])
));

export const cellPeers = Array.from({ length: SUDOKU_SIZE * SUDOKU_SIZE }, (_, index) => {
  const peers = new Set<number>();
  rowCellIndexes[cellRowIndexes[index]].forEach((peerIndex) => peers.add(peerIndex));
  columnCellIndexes[cellColIndexes[index]].forEach((peerIndex) => peers.add(peerIndex));
  boxCellIndexes[cellBoxIndexes[index]].forEach((peerIndex) => peers.add(peerIndex));
  peers.delete(index);
  return Array.from(peers).sort((left, right) => left - right);
});

export interface SudokuBitmaskState {
  board: number[];
  candidateMask: number[];
  rowMask: number[];
  colMask: number[];
  boxMask: number[];
  unresolvedCount: number;
}

export function flattenSudokuBoard(board: SudokuBoard): number[] {
  return board.flatMap((row) => row.map((value) => value ?? 0));
}

export function inflateSudokuBoard(flatBoard: readonly number[]): SudokuBoard {
  return Array.from({ length: SUDOKU_SIZE }, (_, row) => (
    Array.from({ length: SUDOKU_SIZE }, (_, col) => {
      const value = flatBoard[getCellIndex(row, col)] ?? 0;
      return value === 0 ? null : value as SudokuDigit;
    })
  ));
}

export function iterateMaskDigits(mask: number): SudokuDigit[] {
  return sudokuDigits.filter((digit) => (mask & digitToBit[digit]) !== 0);
}

export function createSudokuBitmaskStateFromFlatBoard(flatBoard: readonly number[]): SudokuBitmaskState {
  const board = [...flatBoard];
  const candidateMask = Array.from({ length: SUDOKU_SIZE * SUDOKU_SIZE }, () => 0);
  const rowMask = Array.from({ length: SUDOKU_SIZE }, () => 0);
  const colMask = Array.from({ length: SUDOKU_SIZE }, () => 0);
  const boxMask = Array.from({ length: SUDOKU_SIZE }, () => 0);

  for (let index = 0; index < board.length; index += 1) {
    const value = board[index] ?? 0;
    if (value === 0) {
      continue;
    }

    const bit = digitToBit[value];
    const row = cellRowIndexes[index];
    const col = cellColIndexes[index];
    const box = cellBoxIndexes[index];
    if ((rowMask[row] & bit) !== 0 || (colMask[col] & bit) !== 0 || (boxMask[box] & bit) !== 0) {
      throw new Error(`Invalid Sudoku board: duplicate digit ${value} at row ${row + 1}, col ${col + 1}.`);
    }
    rowMask[row] |= bit;
    colMask[col] |= bit;
    boxMask[box] |= bit;
  }

  let unresolvedCount = 0;
  for (let index = 0; index < board.length; index += 1) {
    if (board[index] !== 0) {
      continue;
    }
    const row = cellRowIndexes[index];
    const col = cellColIndexes[index];
    const box = cellBoxIndexes[index];
    candidateMask[index] = FULL_MASK & ~(rowMask[row] | colMask[col] | boxMask[box]);
    unresolvedCount += 1;
  }

  return {
    board,
    candidateMask,
    rowMask,
    colMask,
    boxMask,
    unresolvedCount,
  };
}

export function createSudokuBitmaskStateFromBoard(board: SudokuBoard): SudokuBitmaskState {
  return createSudokuBitmaskStateFromFlatBoard(flattenSudokuBoard(board));
}

export function createSudokuBitmaskStateFromSession(session: SudokuSession): SudokuBitmaskState {
  return createSudokuBitmaskStateFromBoard(session.board);
}

export function cloneSudokuBitmaskState(state: SudokuBitmaskState): SudokuBitmaskState {
  return {
    board: [...state.board],
    candidateMask: [...state.candidateMask],
    rowMask: [...state.rowMask],
    colMask: [...state.colMask],
    boxMask: [...state.boxMask],
    unresolvedCount: state.unresolvedCount,
  };
}

export function encodeSudokuBitmaskState(state: SudokuBitmaskState): string {
  return `${state.board.join('')}:${state.candidateMask.join(',')}`;
}

export function hasCandidateAtIndex(state: SudokuBitmaskState, index: number, digit: SudokuDigit): boolean {
  return state.board[index] === 0 && (state.candidateMask[index] & digitToBit[digit]) !== 0;
}

export function getCandidateMaskAtCell(state: SudokuBitmaskState, row: number, col: number): number {
  return state.candidateMask[getCellIndex(row, col)] ?? 0;
}

export function placeSudokuDigit(state: SudokuBitmaskState, index: number, digit: SudokuDigit): void {
  const current = state.board[index];
  if (current === digit) {
    return;
  }
  if (current !== 0) {
    throw new Error(`Cannot place digit ${digit} into filled Sudoku cell ${index}.`);
  }

  const bit = digitToBit[digit];
  const row = cellRowIndexes[index];
  const col = cellColIndexes[index];
  const box = cellBoxIndexes[index];
  if ((state.rowMask[row] & bit) !== 0 || (state.colMask[col] & bit) !== 0 || (state.boxMask[box] & bit) !== 0) {
    throw new Error(`Cannot place conflicting digit ${digit} at row ${row + 1}, col ${col + 1}.`);
  }

  state.board[index] = digit;
  state.candidateMask[index] = 0;
  state.rowMask[row] |= bit;
  state.colMask[col] |= bit;
  state.boxMask[box] |= bit;
  state.unresolvedCount -= 1;

  cellPeers[index].forEach((peerIndex) => {
    if (state.board[peerIndex] !== 0) {
      return;
    }
    state.candidateMask[peerIndex] &= ~bit;
  });
}

export function eliminateSudokuCandidate(state: SudokuBitmaskState, index: number, digit: SudokuDigit): void {
  if (state.board[index] !== 0) {
    return;
  }
  state.candidateMask[index] &= ~digitToBit[digit];
}

export function isSudokuBitmaskStateSolved(state: SudokuBitmaskState): boolean {
  return state.unresolvedCount === 0;
}
