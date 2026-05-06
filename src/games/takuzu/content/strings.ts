import type { Difficulty } from '../../../app/types';
import { getTakuzuStrings as resolveTakuzuStrings } from '../i18n';

export interface TakuzuStrings {
  title: string;
  shortTitle: string;
  tagline: string;
  difficultyLabels: Record<Difficulty, string>;
  difficultyDescriptions: Record<Difficulty, string>;
}

export function getTakuzuStrings(): TakuzuStrings {
  return resolveTakuzuStrings();
}
