import type { LossContent } from '../../../app/shell/games/lossContent';
import { getWordSearchLossContent as resolveWordSearchLossContent } from './i18n';

export function getWordSearchLossContent(): LossContent {
  return resolveWordSearchLossContent();
}
