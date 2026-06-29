import type { GameDefinition } from '../../app/shell/games/gameDefinition';
import { getWordSearchHowToPlay } from './content/howToPlay';
import { getWordSearchLossContent } from './content/loss';
import { getWordSearchStrings } from './content/strings';
import { buildWordSearchResult, type WordSearchPlaySession } from './gameplay/playContract';
import { wordSearchPlayAdapter } from './ui/play/adapter';
export const wordSearchDefinition: GameDefinition<WordSearchPlaySession> = {
  id: 'wordsearch',
  get title() {
    return getWordSearchStrings().title;
  },
  get shortTitle() {
    return getWordSearchStrings().shortTitle;
  },
  emoji: '🔤',
  get tagline() {
    return getWordSearchStrings().tagline;
  },
  gestureProfile: { mode: 'zoom', viewport: 'optional' },
  supports: {
    learning: true,
    scoring: true,
  },
  difficulties: ['easy', 'medium', 'hard', 'expert'],
  createResult: (session) => buildWordSearchResult(session),
  playAdapter: wordSearchPlayAdapter,
  content: {
    get howToPlay() {
      return getWordSearchHowToPlay();
    },
    get loss() {
      return getWordSearchLossContent();
    },
    get difficultyLabels() {
      return getWordSearchStrings().difficultyLabels;
    },
    get difficultyDescriptions() {
      return getWordSearchStrings().difficultyDescriptions;
    },
  },
};
