import type { GameId } from '../games/shared/types';
import type { PuzzleAnalysisAdapter } from './analysis/types';
import { minesweeperAnalysisAdapter } from '../games/minesweeper/learningCenter/analyzer';
import { takuzuAnalysisAdapter } from '../games/takuzu/learningCenter/analyzer';

export function getGameAnalysisAdapter(gameId: GameId): PuzzleAnalysisAdapter | null {
  switch (gameId) {
    case 'minesweeper':
      return minesweeperAnalysisAdapter;
    case 'takuzu':
      return takuzuAnalysisAdapter;
    default:
      return null;
  }
}
