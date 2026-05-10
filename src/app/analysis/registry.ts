import type { PuzzleTypeId } from '../shell/types';
import type { PuzzleAnalysisAdapter } from './types';
import { nonogramAnalysisAdapter } from '../../games/nonogram/ui/learning/analyzer';
import { takuzuAnalysisAdapter } from '../../games/takuzu/learningCenter/analyzer';

export function getPuzzleAnalysisAdapter(puzzleTypeId: PuzzleTypeId): PuzzleAnalysisAdapter | null {
  switch (puzzleTypeId) {
    case 'nonogram':
      return nonogramAnalysisAdapter;
    case 'takuzu':
      return takuzuAnalysisAdapter;
    default:
      return null;
  }
}
