import type { GameDefinition } from '../../app/shell/games/gameDefinition';
import { getChimpTestHowToPlay } from './content/howToPlay';
import { getChimpTestLossContent } from './content/loss';
import { getChimpTestStrings } from './content/i18n';
import { chimpTestPlayAdapter } from './ui/play/adapter';
import { buildChimpTestResult, type ChimpTestPlaySession } from './gameplay/playContract';

export const chimpTestDefinition: GameDefinition<ChimpTestPlaySession> = {
  id: 'chimptest',
  get title() {
    return getChimpTestStrings().title;
  },
  get shortTitle() {
    return getChimpTestStrings().shortTitle;
  },
  emoji: '🐒',
  get tagline() {
    return getChimpTestStrings().tagline;
  },
  supports: {
    learning: true,
    scoring: true,
  },
  difficulties: ['easy', 'medium', 'hard', 'expert'],
  createResult: (session) => buildChimpTestResult(session),
  playAdapter: chimpTestPlayAdapter,
  content: {
    get howToPlay() {
      return getChimpTestHowToPlay();
    },
    get loss() {
      return getChimpTestLossContent();
    },
    get difficultyLabels() {
      return getChimpTestStrings().difficultyLabels;
    },
    get difficultyDescriptions() {
      return getChimpTestStrings().difficultyDescriptions;
    },
  },
};
