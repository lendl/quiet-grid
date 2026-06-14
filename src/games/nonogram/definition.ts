import type { GameDefinition } from '../../app/shell/games/gameDefinition';
import { getNonogramHowToPlay } from './content/howToPlay';
import { getNonogramLossContent } from './content/loss';
import { getNonogramStrings } from './content/strings';
import { nonogramPlayAdapter } from './ui/play/adapter';
import { buildNonogramResult, type NonogramPlaySession } from './gameplay/playContract';
export { nonogramGameSemantics } from './gameplay/semantics';

export const nonogramDefinition: GameDefinition<NonogramPlaySession> = {
  id: 'nonogram',
  get title() {
    return getNonogramStrings().title;
  },
  get shortTitle() {
    return getNonogramStrings().shortTitle;
  },
  emoji: '🧩',
  beta: true,
  get tagline() {
    return getNonogramStrings().tagline;
  },
  supports: {
    learning: true,
    scoring: true,
  },
  difficulties: ['easy', 'medium', 'hard', 'expert'],
  createResult: (session) => buildNonogramResult(session),
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
};
