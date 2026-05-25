import type { LossContent } from '../../../app/shell/games/lossContent';
import { getNonogramLossContent as resolveNonogramLossContent } from './i18n';

export function getNonogramLossContent(): LossContent {
  return resolveNonogramLossContent();
}
