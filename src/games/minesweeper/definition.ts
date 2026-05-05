import type { PuzzleDefinition } from '../../app/shell/games/gameDefinition';
import { getMinesweeperHowToPlay } from './content/howToPlay';
import { getMinesweeperLossContent } from './content/loss';
import { getMinesweeperStrings } from './content/strings';
import { minesweeperPlayAdapter } from './playAdapter';

export const minesweeperDefinition: PuzzleDefinition = {
  id: 'minesweeper',
  get title() {
    return getMinesweeperStrings().title;
  },
  get shortTitle() {
    return getMinesweeperStrings().shortTitle;
  },
  emoji: '💣',
  get tagline() {
    return getMinesweeperStrings().tagline;
  },
  supports: {
    tutorial: false,
    learning: true,
    scoring: true,
  },
  difficulties: ['easy', 'medium', 'hard', 'expert'],
  createOutcome: () => {
    throw new Error('Minesweeper outcome adapter not wired yet.');
  },
  playAdapter: minesweeperPlayAdapter,
  content: {
    get howToPlay() {
      return getMinesweeperHowToPlay();
    },
    get loss() {
      return getMinesweeperLossContent();
    },
    get difficultyLabels() {
      return getMinesweeperStrings().difficultyLabels;
    },
    get difficultyDescriptions() {
      return getMinesweeperStrings().difficultyDescriptions;
    },
  },
  screens: {
  },
};
