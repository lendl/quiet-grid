import type { LossContent } from '../../../app/shell/games/lossContent';
import { getMinesweeperLossContent as resolveMinesweeperLossContent } from './i18n';

export function getMinesweeperLossContent(): LossContent {
  return resolveMinesweeperLossContent();
}
