import type { HowToPlayContent } from '../../../app/shell/games/howToPlayContent';
import { getNonogramHowToPlay as resolveNonogramHowToPlay } from './i18n';

export function getNonogramHowToPlay(): HowToPlayContent {
  return resolveNonogramHowToPlay();
}
