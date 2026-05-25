import type { PuzzleAnalysisPayload } from '../../../../../app/analysis/types';
import type { NonogramPuzzle } from '../../../types';

export interface NonogramAnalysisPayload extends PuzzleAnalysisPayload {
  payload: {
    puzzle: NonogramPuzzle;
  };
}
