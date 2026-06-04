import type { Difficulty } from '../../../app/types';
import { getMinesweeperStrings as resolveMinesweeperStrings } from './i18n';

export interface MinesweeperStrings {
  title: string;
  shortTitle: string;
  tagline: string;
  difficultyLabels: Record<Difficulty, string>;
  difficultyDescriptions: Record<Difficulty, string>;
  play: {
    metadataLabels: {
      size: string;
      difficulty: string;
      minesLeft: string;
    };
    helperToggle: {
      show: string;
      hide: string;
    };
  };
}

export function getMinesweeperStrings(): MinesweeperStrings {
  return resolveMinesweeperStrings();
}
