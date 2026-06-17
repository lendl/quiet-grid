import type { LossContent } from '../../../app/shell/games/lossContent';
import { getChimpTestLossContent as resolveChimpTestLossContent } from './i18n';

export function getChimpTestLossContent(): LossContent {
  return resolveChimpTestLossContent();
}
