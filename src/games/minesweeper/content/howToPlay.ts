import type { HowToPlayContent } from '../../../app/shell/games/howToPlayContent';
import { getMinesweeperHowToPlay as resolveMinesweeperHowToPlay } from '../i18n';

export function getMinesweeperHowToPlay(): HowToPlayContent {
  return resolveMinesweeperHowToPlay();
}
