import type { HowToPlayContent } from '../../../app/shell/games/howToPlayContent';
import { getSudokuHowToPlay as resolveSudokuHowToPlay } from './i18n';

export function getSudokuHowToPlay(): HowToPlayContent {
  return resolveSudokuHowToPlay();
}
