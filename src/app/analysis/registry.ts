import type { GameId } from '../../games/shared/types';
import type { PuzzleAnalysisAdapter } from './types';
import { takuzuAnalysisAdapter } from '../../games/takuzu/learningCenter/analyzer';

export function getGameAnalysisAdapter(gameId: GameId): PuzzleAnalysisAdapter | null {
  switch (gameId) {
    case 'takuzu':
      return takuzuAnalysisAdapter;
    default:
      return null;
  }
}
