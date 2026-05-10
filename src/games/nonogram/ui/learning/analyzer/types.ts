import type {
  PuzzleAnalysisPayload,
  PuzzleAnalysisStep,
  PuzzleAnalysisSource,
} from '../../../../../app/analysis/types';
import type { NonogramCellState, NonogramPuzzle } from '../../../types';
import type { NonogramCellRef } from '../../../types';

export interface NonogramAnalysisSourcePayload {
  puzzle: NonogramPuzzle;
  cells: NonogramCellState[];
}

export interface NonogramAnalysisSource extends PuzzleAnalysisSource {
  puzzleTypeId: 'nonogram';
  payload: NonogramAnalysisSourcePayload;
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
