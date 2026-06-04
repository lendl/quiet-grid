import TutorialScreen from './ui/tutorial/screen';
import type { GameDefinition } from '../../app/shell/games/gameDefinition';
import { getMinesweeperHowToPlay } from './content/howToPlay';
import { getMinesweeperLossContent } from './content/loss';
import { getMinesweeperStrings } from './content/strings';
import { buildMinesweeperResult, type MinesweeperPlaySession } from './gameplay/playContract';
import { minesweeperPlayAdapter } from './ui/play/adapter';

export const minesweeperDefinition: GameDefinition<MinesweeperPlaySession> = {
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
  createResult: (session) => buildMinesweeperResult(session),
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
