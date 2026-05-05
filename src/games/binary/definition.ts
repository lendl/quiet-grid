import TutorialScreen from './screens/TutorialScreen';
import type { PuzzleDefinition } from '../../app/shell/games/gameDefinition';
import { getBinaryHowToPlay } from './content/howToPlay';
import { getBinaryLossContent } from './content/loss';
import { getBinaryStrings } from './content/strings';
import { binaryPlayAdapter } from './playAdapter';

export const binaryDefinition: PuzzleDefinition = {
  id: 'binary',
  get title() {
    return getBinaryStrings().title;
  },
  get shortTitle() {
    return getBinaryStrings().shortTitle;
  },
  emoji: '⬛',
  get tagline() {
    return getBinaryStrings().tagline;
  },
  supports: {
    tutorial: true,
    learning: true,
    scoring: true,
  },
  difficulties: ['easy', 'medium', 'hard', 'expert'],
  createOutcome: () => {
    throw new Error('Binary outcome adapter not wired yet.');
  },
  playAdapter: binaryPlayAdapter,
  content: {
    get howToPlay() {
      return getBinaryHowToPlay();
    },
    get loss() {
      return getBinaryLossContent();
    },
    get difficultyLabels() {
      return getBinaryStrings().difficultyLabels;
    },
    get difficultyDescriptions() {
      return getBinaryStrings().difficultyDescriptions;
    },
  },
  screens: {
    tutorial: TutorialScreen,
  },
};
