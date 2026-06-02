import type { GameId } from '../games/shared/types';
import type { PuzzleAnalysisAdapter } from './analysis/types';
import { minesweeperAnalysisAdapter } from '../games/minesweeper/learningCenter/analyzer';
import { nonogramAnalysisAdapter } from '../games/nonogram/learningCenter/analyzer';
import { takuzuAnalysisAdapter } from '../games/takuzu/learningCenter/analyzer';
import { sudokuAnalysisAdapter } from '../games/sudoku/ui/learning/analyzer';
import { wordSearchAnalysisAdapter } from '../games/wordsearch/learningCenter/analyzer';

export function getGameAnalysisAdapter(gameId: GameId): PuzzleAnalysisAdapter | null {
  switch (gameId) {
    case 'minesweeper':
      return minesweeperAnalysisAdapter;
    case 'nonogram':
      return nonogramAnalysisAdapter;
    case 'takuzu':
      return takuzuAnalysisAdapter;
    case 'sudoku':
      return sudokuAnalysisAdapter;
    case 'wordsearch':
      return wordSearchAnalysisAdapter;
    default:
      return null;
  }
}

