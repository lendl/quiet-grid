import type { LossContent } from '../../../app/shell/games/lossContent';
import { getTakuzuLossContent as resolveTakuzuLossContent } from '../i18n';

export function getTakuzuLossContent(): LossContent {
  return resolveTakuzuLossContent();
}
