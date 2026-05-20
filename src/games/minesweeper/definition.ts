import TutorialScreen from './screens/TutorialScreen';
import type { GameDefinition } from '../../app/shell/games/gameDefinition';
import { getMinesweeperHowToPlay } from './content/howToPlay';
import { getMinesweeperLossContent } from './content/loss';
import { getMinesweeperStrings } from './content/strings';
import { minesweeperPlayAdapter } from './playAdapter';

export const minesweeperDefinition: GameDefinition = {
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
    tutorial: true,
    learning: true,
    scoring: true,
  },
  difficulties: ['easy', 'medium', 'hard', 'expert'],
  createResult: () => {
    throw new Error('Minesweeper result adapter not wired yet.');
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
    tutorial: TutorialScreen,
  },
};
