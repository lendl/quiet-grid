import { getCurrentLanguage } from '../../../../app/i18n';
import { resolveGameContent, type LocalizedGameContent } from '../../../../app/i18n/gameContent';
import type { LossContent } from '../../../../app/shell/games/lossContent';
import type { HowToPlayContent } from '../../../../app/shell/games/howToPlayContent';
import type { TakuzuStrings } from '../strings';
import type { HowToPlayTip } from '../howToPlayTips';
import en from './en';
import nl from './nl';
import de from './de';
import fr from './fr';
import es from './es';

export type TakuzuTutorialLessonKey =
  | 'find-pairs'
  | 'avoid-trios'
  | 'complete-lines'
  | 'eliminate-filled-lines'
  | 'eliminate-impossible-combinations';

export type TakuzuTutorialLessonCopy = {
  title: string;
  body: string;
  prompt: string;
  retry: string;
  success: string;
};

export interface TakuzuLearningCenterContent {
  pausedNextMove: {
    title: string;
    body: string;
  };
  findPairs(lineLabel: string, repeatedValue: 0 | 1, targetValue: 0 | 1, cellLabel: string): {
    title: string;
    body: string;
  };
  avoidTrios(lineLabel: string, repeatedValue: 0 | 1, targetValue: 0 | 1): {
    title: string;
    body: string;
  };
  completeLines(
    lineLabel: string,
    filledValue: 0 | 1,
    filledCount: number,
    targetValue: 0 | 1,
    cellLabel: string,
  ): { title: string; body: string };
  eliminateFilledLines(
    lineLabel: string,
    matchingLineLabel: string,
    targetValue: 0 | 1,
    cellLabel: string,
    lineKindLabel: string,
  ): { title: string; body: string };
  eliminateImpossible(
    lineLabel: string,
    validCompletionCount: number,
    blockedValue: 0 | 1,
    targetValue: 0 | 1,
    cellLabel: string,
    contradictionKind: 'triple' | 'balance' | 'duplicate-line',
    contradictionLineLabel: string,
    proofRuleLabel: string,
  ): { title: string; body: string };
  avoidTriosRepair(lineLabel: string, repeatedValue: 0 | 1): { title: string; body: string };
  completeLinesRepair(
    lineLabel: string,
    filledValue: 0 | 1,
    filledCount: number,
    limit: number,
  ): { title: string; body: string };
  eliminateFilledLinesRepair(
    firstLineLabel: string,
    secondLineLabel: string,
    lineLabel: string,
  ): { title: string; body: string };
}

export interface TakuzuI18n {
  strings: TakuzuStrings;
  howToPlayRules: HowToPlayContent['rules'];
  howToPlayTips: readonly HowToPlayTip[];
  loss: LossContent;
  tutorialLessons: Record<TakuzuTutorialLessonKey, TakuzuTutorialLessonCopy>;
  learningCenter: TakuzuLearningCenterContent;
}

const CONTENT: LocalizedGameContent<TakuzuI18n> = {
  en,
  nl,
  de,
  fr,
  es,
};

export function getTakuzuI18n(): TakuzuI18n {
  return resolveGameContent(getCurrentLanguage(), CONTENT);
}

export function getTakuzuStrings(): TakuzuStrings {
  return getTakuzuI18n().strings;
}

export function getTakuzuHowToPlayRules(): HowToPlayContent['rules'] {
  return getTakuzuI18n().howToPlayRules;
}

export function getTakuzuHowToPlayTips(): HowToPlayTip[] {
  return [...getTakuzuI18n().howToPlayTips];
}

export function getTakuzuLossContent(): LossContent {
  return getTakuzuI18n().loss;
}

export function getTakuzuTutorialLessonCopies(): Record<TakuzuTutorialLessonKey, TakuzuTutorialLessonCopy> {
  return getTakuzuI18n().tutorialLessons;
}

export function getTakuzuLearningCenterContent(): TakuzuLearningCenterContent {
  return getTakuzuI18n().learningCenter;
}
