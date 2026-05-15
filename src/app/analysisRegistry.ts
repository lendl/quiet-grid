import type { PuzzleTypeId } from './shell/types';
import type { PuzzleAnalysisAdapter } from './analysis/types';
import { minesweeperAnalysisAdapter } from '../games/minesweeper/learningCenter/analyzer';
import { takuzuAnalysisAdapter } from '../games/takuzu/learningCenter/analyzer';

export function getPuzzleAnalysisAdapter(puzzleTypeId: PuzzleTypeId): PuzzleAnalysisAdapter | null {
  switch (puzzleTypeId) {
    case 'minesweeper':
      return minesweeperAnalysisAdapter;
    case 'takuzu':
      return takuzuAnalysisAdapter;
    default:
      return null;
  }
}
