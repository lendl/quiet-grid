import type { HowToPlayContent } from '../../../../app/shell/games/howToPlayContent';
import type { LossContent } from '../../../../app/shell/games/lossContent';
import { getCurrentLanguage } from '../../../../app/i18n';
import { resolveGameContent, type LocalizedGameContent } from '../../../../app/i18n/gameContent';
import type {
  NonogramAxis,
  NonogramDifficulty,
  NonogramExplanationCopy,
} from '../../types';
import en from './en';
import nl from './nl';

export type NonogramTutorialLessonKey =
  | 'read-clues'
  | 'forced-fill'
  | 'forced-mark'
  | 'combine-lines'
  | 'tap-cycle';

export type NonogramTutorialLessonCopy = {
  title: string;
  body: string;
  prompt: string;
  retry: string;
  success: string;
};

export interface NonogramStrings {
  title: string;
  shortTitle: string;
  tagline: string;
  difficultyLabels: Record<'easy' | 'medium' | 'hard' | 'expert', string>;
  difficultyDescriptions: Record<'easy' | 'medium' | 'hard' | 'expert', string>;
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
      message: string;
    };
  };
  tutorial: {
    progressLabel(step: number, total: number): string;
    exitLabel: {
      end: string;
      skip: string;
    };
    status: {
      finishing: string;
      nextLesson: string;
    };
    actionLabels: Record<'filled' | 'marked', string>;
  };
  analyzer: {
    legendEvidence: string;
    legendTarget: string;
    noAnalysisTitle: string;
    noAnalysisBody: string;
  };
}

export interface NonogramAnalysisContent {
  legendEvidence: string;
  legendTarget: string;
  pausedNextMove: NonogramExplanationCopy;
  overlapFill(lineLabel: string, clueLabel: string, cellCount: number): NonogramExplanationCopy;
  forcedEmpty(lineLabel: string, clueLabel: string, cellCount: number): NonogramExplanationCopy;
  completeLine(lineLabel: string, clueLabel: string): NonogramExplanationCopy;
  groupedStep(kind: 'filled' | 'marked', lineLabel: string, clueLabel: string, cellCount: number): NonogramExplanationCopy;
  lineLabel(axis: NonogramAxis, index: number): string;
  clueLabel(clues: readonly number[]): string;
}

export interface NonogramI18n {
  strings: NonogramStrings;
  howToPlay: HowToPlayContent;
  loss: LossContent;
  tutorialLessons: Record<NonogramTutorialLessonKey, NonogramTutorialLessonCopy>;
  analysis: NonogramAnalysisContent;
}

const CONTENT: LocalizedGameContent<NonogramI18n> = {
  en,
  nl,
};

export function getNonogramI18n(): NonogramI18n {
  return resolveGameContent(getCurrentLanguage(), CONTENT);
}

export function getNonogramStrings(): NonogramStrings {
  return getNonogramI18n().strings;
}

export function getNonogramHowToPlayContent(): HowToPlayContent {
  return getNonogramI18n().howToPlay;
}

export function getNonogramLossContent(): LossContent {
  return getNonogramI18n().loss;
}

export function getNonogramTutorialLessonCopies(): Record<NonogramTutorialLessonKey, NonogramTutorialLessonCopy> {
  return getNonogramI18n().tutorialLessons;
}

export function getNonogramAnalysisContent(): NonogramAnalysisContent {
  return getNonogramI18n().analysis;
}

export function isSupportedNonogramDifficulty(value: string): value is NonogramDifficulty {
  return value === 'easy' || value === 'medium';
}
