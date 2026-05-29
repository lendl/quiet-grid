import type { SudokuDigit } from '../../types';
import {
  boxCellIndexes,
  cellBoxIndexes,
  cellColIndexes,
  cellPeers,
  cellRowIndexes,
  columnCellIndexes,
  getCellCol,
  getCellIndex,
  getCellRow,
  hasCandidateAtIndex,
  iterateMaskDigits,
  rowCellIndexes,
  type SudokuBitmaskState,
} from './bitmask';
import type { SudokuCanonicalMove, SudokuCellRef, SudokuHouseRef } from './moves';
import type { SudokuTechnique } from './techniques';

export interface SudokuHouseCellsRef {
  house: SudokuHouseRef;
  cells: number[];
}

export const allHouseRefs: readonly SudokuHouseCellsRef[] = [
  ...rowCellIndexes.map((cells, index) => ({ house: { kind: 'row' as const, index }, cells })),
  ...columnCellIndexes.map((cells, index) => ({ house: { kind: 'column' as const, index }, cells })),
  ...boxCellIndexes.map((cells, index) => ({ house: { kind: 'box' as const, index }, cells })),
];

function buildCellKey(cell: SudokuCellRef & { digit?: SudokuDigit }): string {
  return `${cell.row}:${cell.col}:${cell.digit ?? 0}`;
}

function compareCellRefs(left: SudokuCellRef, right: SudokuCellRef): number {
  if (left.row !== right.row) {
    return left.row - right.row;
  }
  return left.col - right.col;
}

export function buildPlacementHouseRefs(row: number, col: number): SudokuHouseRef[] {
  return [
    { kind: 'row', index: row },
    { kind: 'column', index: col },
    { kind: 'box', index: cellBoxIndexes[getCellIndex(row, col)] },
  ];
}

export function collectHousesFromIndexes(indexes: readonly number[]): SudokuHouseRef[] {
  const seen = new Map<string, SudokuHouseRef>();
  indexes.forEach((index) => {
    const row = cellRowIndexes[index];
    const col = cellColIndexes[index];
    const box = cellBoxIndexes[index];
    seen.set(`r:${row}`, { kind: 'row', index: row });
    seen.set(`c:${col}`, { kind: 'column', index: col });
    seen.set(`b:${box}`, { kind: 'box', index: box });
  });
  return Array.from(seen.values()).sort((left, right) => {
    if (left.kind !== right.kind) {
      return left.kind.localeCompare(right.kind);
    }
    return left.index - right.index;
  });
}

export function cellRefFromIndex(index: number): SudokuCellRef {
  return {
    row: getCellRow(index),
    col: getCellCol(index),
  };
}

export function getCellCandidates(state: SudokuBitmaskState, row: number, col: number): SudokuDigit[] {
  return iterateMaskDigits(state.candidateMask[getCellIndex(row, col)] ?? 0);
}

export function getCellCandidatesByIndex(state: SudokuBitmaskState, index: number): SudokuDigit[] {
  return iterateMaskDigits(state.candidateMask[index] ?? 0);
}

export function isSameCell(left: { row: number; col: number }, right: { row: number; col: number }): boolean {
  return left.row === right.row && left.col === right.col;
}

export function arePeerCells(left: { row: number; col: number }, right: { row: number; col: number }): boolean {
  return cellPeers[getCellIndex(left.row, left.col)].includes(getCellIndex(right.row, right.col));
}

export function arePeerIndexes(left: number, right: number): boolean {
  return cellPeers[left].includes(right);
}

export function buildPlacementMove(input: {
  technique: SudokuTechnique;
  row: number;
  col: number;
  digit: SudokuDigit;
  evidenceCells: number[];
  houses?: SudokuHouseRef[];
}): SudokuCanonicalMove {
  return {
    kind: 'placement',
    technique: input.technique,
    target: {
      row: input.row,
      col: input.col,
      digit: input.digit,
    },
    evidenceCells: input.evidenceCells.map(cellRefFromIndex).sort(compareCellRefs),
    houses: input.houses ?? buildPlacementHouseRefs(input.row, input.col),
  };
}

export function buildCandidateEliminationMove(input: {
  technique: SudokuTechnique;
  eliminations: Array<{ index: number; digit: SudokuDigit }>;
  evidenceCells: number[];
  houses?: SudokuHouseRef[];
}): SudokuCanonicalMove | null {
  const uniqueEliminations = Array.from(new Map(
    input.eliminations.map((elimination) => {
      const cell = cellRefFromIndex(elimination.index);
      return [
        buildCellKey({ ...cell, digit: elimination.digit }),
        {
          ...cell,
          digit: elimination.digit,
        },
      ] as const;
    }),
  ).values()).sort((left, right) => {
    if (left.row !== right.row) {
      return left.row - right.row;
    }
    if (left.col !== right.col) {
      return left.col - right.col;
    }
    return left.digit - right.digit;
  });

  if (uniqueEliminations.length === 0) {
    return null;
  }

  return {
    kind: 'candidate-elimination',
    technique: input.technique,
    eliminations: uniqueEliminations,
    evidenceCells: Array.from(new Map(
      input.evidenceCells.map((index) => {
        const cell = cellRefFromIndex(index);
        return [buildCellKey(cell), cell] as const;
      }),
    ).values()).sort(compareCellRefs),
    houses: input.houses ?? collectHousesFromIndexes(input.evidenceCells),
  };
}

export function peerIntersectionIndexes(indexes: readonly number[]): number[] {
  if (indexes.length === 0) {
    return [];
  }

  const [first, ...rest] = indexes;
  return cellPeers[first].filter((candidate) => rest.every((index) => arePeerIndexes(candidate, index)));
}

export function getHouseDigitMatches(
  state: SudokuBitmaskState,
  cells: readonly number[],
  digit: SudokuDigit,
): number[] {
  return cells.filter((index) => hasCandidateAtIndex(state, index, digit));
}
