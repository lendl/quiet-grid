import { getNonogramStrings as resolveNonogramStrings } from './i18n';
import type { NonogramStrings } from './i18n';

export function getNonogramStrings(): NonogramStrings {
  return resolveNonogramStrings();
}
