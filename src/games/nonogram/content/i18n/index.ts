import { getCurrentLanguage } from '../../../../app/i18n';
import { resolveGameContent, type LocalizedGameContent } from '../../../../app/i18n/gameContent';
import type { HowToPlayContent } from '../../../../app/shell/games/howToPlayContent';
import type { LossContent } from '../../../../app/shell/games/lossContent';
import type { Difficulty } from '../../../../app/types';
import en from './en';

export type NonogramTutorialLessonKey =
  | 'tap-swipe'
  | 'overlap-fill'
  | 'forced-empty'
  | 'complete-line';

export type NonogramTutorialLessonCopy = {
  title: string;
  body: string;
  summary: string;
  continueLabel: string;
};

export interface NonogramStrings {
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
    cellLabel: string;
    tutorial: {
      progressLabel(step: number): string;
      introNote: string;
      exitLabel: {
        end: string;
        skip: string;
      };
      status: {
        finishing: string;
        nextLesson: string;
        nextStep: string;
      };
      answerPrompt: string;
      answerRetry: string;
      selectAnswerLabel(value: 0 | 1): string;
    };
    analysis: {
      invalidBoard: {
        title: string;
        body(lineLabel: string): string;
      };
      overlapFill: {
        title: string;
        body(lineLabel: string, targetCount: number): string;
      };
      forcedEmpty: {
        title: string;
        body(lineLabel: string, targetCount: number): string;
      };
      completeLine: {
        title: string;
        body(lineLabel: string, targetCount: number): string;
      };
    };
  };
}

export interface NonogramI18n {
  strings: NonogramStrings;
  howToPlayGoal: string;
  howToPlayControls: string;
  howToPlayWrongMove: string;
  howToPlayRules: HowToPlayContent['rules'];
  howToPlayTechniques: HowToPlayContent['techniques'];
  howToPlayTips: HowToPlayContent['tips'];
  loss: LossContent;
  tutorialLessons: Record<NonogramTutorialLessonKey, NonogramTutorialLessonCopy>;
}

const CONTENT: LocalizedGameContent<NonogramI18n> = {
  en,
};

export function getNonogramI18n(): NonogramI18n {
  return resolveGameContent(getCurrentLanguage(), CONTENT);
}

export function getNonogramStrings(): NonogramStrings {
  return getNonogramI18n().strings;
}

export function getNonogramTutorialLessonCopies(): Record<NonogramTutorialLessonKey, NonogramTutorialLessonCopy> {
  return getNonogramI18n().tutorialLessons;
}

export function getNonogramHowToPlay(): HowToPlayContent {
  const i18n = getNonogramI18n();
  return {
    goal: i18n.howToPlayGoal,
    controls: i18n.howToPlayControls,
    wrongMove: i18n.howToPlayWrongMove,
    rules: i18n.howToPlayRules,
    techniques: i18n.howToPlayTechniques,
    tips: [...i18n.howToPlayTips],
  };
}

export function getNonogramLossContent(): LossContent {
  return getNonogramI18n().loss;
}
