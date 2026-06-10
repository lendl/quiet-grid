import type { SudokuDigit } from '../../types';
import type { SudokuTechnique } from './techniques';

export interface SudokuCellRef {
  row: number;
  col: number;
}

export interface SudokuHouseRef {
  kind: 'row' | 'column' | 'box';
  index: number;
}

export interface SudokuPlacementMove {
  kind: 'placement';
  technique: SudokuTechnique;
  complexity: number;
  target: SudokuCellRef & { digit: SudokuDigit };
  evidenceCells: SudokuCellRef[];
  houses: SudokuHouseRef[];
}

export interface SudokuCandidateEliminationMove {
  kind: 'candidate-elimination';
  technique: SudokuTechnique;
  complexity: number;
  eliminations: Array<SudokuCellRef & { digit: SudokuDigit }>;
  evidenceCells: SudokuCellRef[];
  houses: SudokuHouseRef[];
}

export type SudokuCanonicalMove = SudokuPlacementMove | SudokuCandidateEliminationMove;

export function countSudokuMoveTargets(move: SudokuCanonicalMove): number {
  return move.kind === 'placement' ? 1 : move.eliminations.length;
}
