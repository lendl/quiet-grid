import type { PuzzleDefinition } from '../../app/shell/games/gameDefinition';
import { getNonogramHowToPlay } from './content/howToPlay';
import { getNonogramLossContent } from './content/loss';
import { getNonogramStrings } from './content/strings';
import { nonogramPlayAdapter } from './ui/play/adapter';
import TutorialScreen from './ui/tutorial/screen';

export const nonogramDefinition: PuzzleDefinition = {
  id: 'nonogram',
  get title() {
    return getNonogramStrings().title;
  },
  get shortTitle() {
    return getNonogramStrings().shortTitle;
  },
  emoji: '🧩',
  get tagline() {
    return getNonogramStrings().tagline;
  },
  supports: {
    tutorial: true,
    learning: true,
    scoring: true,
  },
  difficulties: ['easy', 'medium'],
  createOutcome: () => {
    throw new Error('Nonogram outcome adapter not wired yet.');
  },
  playAdapter: nonogramPlayAdapter,
  content: {
    get howToPlay() {
      return getNonogramHowToPlay();
    },
    get loss() {
      return getNonogramLossContent();
    },
    get difficultyLabels() {
      return getNonogramStrings().difficultyLabels;
    },
    get difficultyDescriptions() {
      return getNonogramStrings().difficultyDescriptions;
    },
  },
  screens: {
    tutorial: TutorialScreen,
  },
};
