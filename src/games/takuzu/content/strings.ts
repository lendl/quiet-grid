import type { Difficulty } from '../../../app/types';
import { getTakuzuStrings as resolveTakuzuStrings } from '../i18n';

export interface TakuzuStrings {
  title: string;
  shortTitle: string;
  tagline: string;
  difficultyLabels: Record<Difficulty, string>;
  difficultyDescriptions: Record<Difficulty, string>;
  play: {
    metadataLabels: {
      size: string;
      difficulty: string;
    };
    helperToggle: {
      show: string;
      hide: string;
    };
    noPuzzlesDialog: {
      title: string;
      message(difficultyLabel: string): string;
    };
    tutorial: {
      progressLabel(step: number): string;
      exitLabel: {
        end: string;
        skip: string;
      };
      status: {
        finishing: string;
        nextLesson: string;
        nextStep: string;
      };
      selectAnswerLabel(value: 0 | 1): string;
    };
  };
}

export function getTakuzuStrings(): TakuzuStrings {
  return resolveTakuzuStrings();
}
