import type {
  PuzzleAnalysisPayload,
  PuzzleAnalysisStep,
  PuzzleLossAnalysisSource,
} from '../../../../../app/analysis/types';
import type { NonogramCellState, NonogramPuzzle } from '../../../types';
import type { NonogramCellRef } from '../../../types';

export interface NonogramLossAnalysisPayload {
  puzzle: NonogramPuzzle;
  cells: NonogramCellState[];
}

export interface NonogramLossAnalysisSource extends PuzzleLossAnalysisSource {
  puzzleTypeId: 'nonogram';
  payload: NonogramLossAnalysisPayload;
}

export interface NonogramAnalysisStep extends PuzzleAnalysisStep {
  beforeState: NonogramCellState[];
  afterState: NonogramCellState[];
  targetCells: NonogramCellRef[];
}

export interface NonogramAnalysisPayload extends PuzzleAnalysisPayload {
  puzzleTypeId: 'nonogram';
  steps: NonogramAnalysisStep[];
  payload: {
    puzzle: NonogramPuzzle;
    labels: {
      evidence: string;
      target: string;
    };
  };
}
