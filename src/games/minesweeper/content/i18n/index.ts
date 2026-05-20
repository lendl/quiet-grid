import { getCurrentLanguage } from '../../../../app/i18n';
import { resolveGameContent, type LocalizedGameContent } from '../../../../app/i18n/gameContent';
import type { LossContent } from '../../../../app/shell/games/lossContent';
import type { HowToPlayContent } from '../../../../app/shell/games/howToPlayContent';
import type { MinesweeperStrings } from '../strings';
import en from './en';
import nl from './nl';
import de from './de';
import fr from './fr';
import es from './es';

export type TutorialTextKey =
  | 'goal-and-stakes'
  | 'core-actions'
  | 'reading-clues'
  | 'forced-flag'
  | 'safe-reveal'
  | 'compare-clues'
  | 'advanced-patterns'
  | 'guess-and-help';

export type ActionLessonText = {
  title: string;
  body: string;
  prompt: string;
  retry: string;
  success: string;
};

export type InfoLessonText = {
  title: string;
  body: string;
  prompt: string;
  summary: string;
  continueLabel: string;
};

export type MinesweeperLearningCenterPatternKey =
  | 'single-mine-logic'
  | 'all-mines-accounted-for'
  | 'only-one-possible-mine'
  | 'guaranteed-safe-tile'
  | 'full-clue-resolution';

export type MinesweeperMineFlagReason =
  | 'direct-local'
  | 'subset-difference';

export type LearningCenterPatternParams = {
  patternKey: MinesweeperLearningCenterPatternKey;
  clueLabel?: string;
  secondaryClueLabel?: string;
  tileLabel: string;
  mineLabel: string;
  mineCount: number;
};

export type LearningCenterMineFlagParams = {
  reason: MinesweeperMineFlagReason;
  clueLabel?: string;
  secondaryClueLabel?: string;
  tileLabel: string;
  mineLabel: string;
  mineCount: number;
};

type LearningCenterTeachingText = {
  patternTitle: string;
  patternLabel: string;
  explanationTitle: string;
  explanation: string;
};

export interface MinesweeperI18n {
  strings: MinesweeperStrings;
  howToPlay: HowToPlayContent;
  loss: LossContent;
  analysis: {
    lossSummary(params: { safeCount: number; mineCount: number }): {
      title: string;
      body: string;
    };
    groupedFlagStep(params: { mineCount: number }): {
      title: string;
      body: string;
    };
    legendEvidence: string;
    legendSafe: string;
    legendMine: string;
  };
  tutorialText: Record<TutorialTextKey, ActionLessonText | InfoLessonText>;
  tutorialUi: {
    progressLabel: (step: number) => string;
    exitLabel: {
      end: string;
      skip: string;
    };
    status: {
      finishing: string;
      nextLesson: string;
    };
    highlightedTile: string;
  };
  learningCenter: {
    formatCellLabel(cell: { row: number; col: number }): string;
    tileLabel(count: number): string;
    mineLabel(count: number): string;
    nextMovePattern(params: LearningCenterPatternParams): {
      title: string;
      body: string;
      teaching: LearningCenterTeachingText;
    };
    flagMovePattern(params: LearningCenterMineFlagParams): {
      title: string;
      body: string;
      teaching: LearningCenterTeachingText;
    };
    guess: {
      title: string;
      body: string;
    };
  };
}

const CONTENT: LocalizedGameContent<MinesweeperI18n> = {
  en,
  nl,
  de,
  fr,
  es,
};

export function getMinesweeperI18n(): MinesweeperI18n {
  return resolveGameContent(getCurrentLanguage(), CONTENT);
}

export function getMinesweeperStrings(): MinesweeperStrings {
  return getMinesweeperI18n().strings;
}

export function getMinesweeperHowToPlay(): HowToPlayContent {
  return getMinesweeperI18n().howToPlay;
}

export function getMinesweeperLossContent(): LossContent {
  return getMinesweeperI18n().loss;
}

export function getMinesweeperAnalysisContent(): MinesweeperI18n['analysis'] {
  return getMinesweeperI18n().analysis;
}

export function getMinesweeperTutorialUi(): MinesweeperI18n['tutorialUi'] {
  return getMinesweeperI18n().tutorialUi;
}

export function getMinesweeperTutorialText(): MinesweeperI18n['tutorialText'] {
  return getMinesweeperI18n().tutorialText;
}

export function getMinesweeperLearningCenterContent(): MinesweeperI18n['learningCenter'] {
  return getMinesweeperI18n().learningCenter;
}
