import TutorialScreen from './screens/TutorialScreen';
import type { GameDefinition } from '../../app/shell/games/gameDefinition';
import { getTakuzuHowToPlay } from './content/howToPlay';
import { getTakuzuLossContent } from './content/loss';
import { getTakuzuStrings } from './content/strings';
import { takuzuPlayAdapter } from './playAdapter';
import { buildTakuzuResult, type TakuzuPlaySession } from './playContract';

export { takuzuGameSemantics } from './gameplay/semantics';

export const takuzuDefinition: GameDefinition<TakuzuPlaySession> = {
  id: 'takuzu',
  get title() {
    return getTakuzuStrings().title;
  },
  get shortTitle() {
    return getTakuzuStrings().shortTitle;
  },
  emoji: '⬛',
  get tagline() {
    return getTakuzuStrings().tagline;
  },
  supports: {
    tutorial: true,
    learning: true,
    scoring: true,
  },
  difficulties: ['easy', 'medium', 'hard', 'expert'],
  createResult: (session) => buildTakuzuResult(session),
  playAdapter: takuzuPlayAdapter,
  content: {
    get howToPlay() {
      return getTakuzuHowToPlay();
    },
    get loss() {
      return getTakuzuLossContent();
    },
    get difficultyLabels() {
      return getTakuzuStrings().difficultyLabels;
    },
    get difficultyDescriptions() {
      return getTakuzuStrings().difficultyDescriptions;
    },
  },
  screens: {
    tutorial: TutorialScreen,
  },
};
