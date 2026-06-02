import type { HowToPlayContent } from '../../../app/shell/games/howToPlayContent';
import { getWordSearchHowToPlay as resolveWordSearchHowToPlay } from './i18n';

export function getWordSearchHowToPlay(): HowToPlayContent {
  return resolveWordSearchHowToPlay();
}
