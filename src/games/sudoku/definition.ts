import type { GameDefinition } from '../../app/shell/games/gameDefinition';
import { getSudokuHowToPlay } from './content/howToPlay';
import { getSudokuLossContent } from './content/loss';
import { getSudokuStrings } from './content/strings';
import { buildSudokuResult, type SudokuPlaySession } from './gameplay/playContract';
import { sudokuPlayAdapter } from './ui/play/adapter';
export const sudokuDefinition: GameDefinition<SudokuPlaySession> = {
  id: 'sudoku',
  get title() {
    return getSudokuStrings().title;
  },
  get shortTitle() {
    return getSudokuStrings().shortTitle;
  },
  emoji: '🔢',
  features: {
    explainTechnique: false,
  },
  get tagline() {
    return getSudokuStrings().tagline;
  },
  supports: {
    learning: true,
    scoring: true,
  },
  difficulties: ['easy', 'medium', 'hard', 'expert'],
  createResult: (session) => buildSudokuResult(session),
  playAdapter: sudokuPlayAdapter,
  content: {
    get howToPlay() {
      return getSudokuHowToPlay();
    },
    get loss() {
      return getSudokuLossContent();
    },
    get difficultyLabels() {
      return getSudokuStrings().difficultyLabels;
    },
    get difficultyDescriptions() {
      return getSudokuStrings().difficultyDescriptions;
    },
  },
};
