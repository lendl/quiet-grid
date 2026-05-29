import type { LossContent } from '../../../app/shell/games/lossContent';
import { getSudokuLossContent as resolveSudokuLossContent } from './i18n';

export function getSudokuLossContent(): LossContent {
  return resolveSudokuLossContent();
}
