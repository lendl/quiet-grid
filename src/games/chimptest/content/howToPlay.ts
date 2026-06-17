import type { HowToPlayContent } from '../../../app/shell/games/howToPlayContent';
import { getChimpTestHowToPlay as resolveChimpTestHowToPlay } from './i18n';

export function getChimpTestHowToPlay(): HowToPlayContent {
  return resolveChimpTestHowToPlay();
}
