import { getWordSearchStrings as resolveWordSearchStrings, type WordSearchStrings } from './i18n';

export type { WordSearchStrings };

export function getWordSearchStrings(): WordSearchStrings {
  return resolveWordSearchStrings();
}
