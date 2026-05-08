import type { PuzzleTypeId } from './shell/types';
import type { PuzzleAnalysisAdapter } from './analysis/types';
import { takuzuAnalysisAdapter } from '../games/takuzu/learningCenter/analyzer';

export function getPuzzleAnalysisAdapter(puzzleTypeId: PuzzleTypeId): PuzzleAnalysisAdapter | null {
  switch (puzzleTypeId) {
    case 'takuzu':
      return takuzuAnalysisAdapter;
    default:
      return null;
  }
}
