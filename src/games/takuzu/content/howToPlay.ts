import type { HowToPlayContent } from '../../../app/shell/games/howToPlayContent';
import { getTakuzuHowToPlay as resolveTakuzuHowToPlay } from './i18n';

export function getTakuzuHowToPlay(): HowToPlayContent {
  return resolveTakuzuHowToPlay();
}
