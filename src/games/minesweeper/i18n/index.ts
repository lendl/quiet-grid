import { getCurrentLanguage } from '../../../app/i18n';
import { resolveGameContent, type LocalizedGameContent } from '../../../app/i18n/gameContent';
import type { LossContent } from '../../../app/shell/games/lossContent';
import type { HowToPlayContent } from '../../../app/shell/games/howToPlayContent';
import type { MinesweeperStrings } from '../content/strings';
import en from './en';
import nl from './nl';

type TutorialTextKey = 'forced-flag' | 'safe-reveal' | 'diagonals-count' | 'compare-clues' | 'guess-moments';

type ActionLessonText = {
  title: string;
  body: string;
  prompt: string;
  retry: string;
  success: string;
};

type InfoLessonText = {
  title: string;
  body: string;
  prompt: string;
  summary: string;
  continueLabel: string;
};

export interface MinesweeperI18n {
  strings: MinesweeperStrings;
  howToPlay: HowToPlayContent;
  loss: LossContent;
  tutorialText: Record<TutorialTextKey, ActionLessonText | InfoLessonText>;
  learningCenter: {
    safeReveal(clueLabel: string, tileLabel: string, mineCount: number, mineLabel: string): {
      title: string;
      body: string;
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

export function getMinesweeperTutorialText(): MinesweeperI18n['tutorialText'] {
  return getMinesweeperI18n().tutorialText;
}

export function getMinesweeperLearningCenterContent(): MinesweeperI18n['learningCenter'] {
  return getMinesweeperI18n().learningCenter;
}
