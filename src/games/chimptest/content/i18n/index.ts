import { getCurrentLanguage } from '../../../../app/i18n';
import { resolveGameContent, type LocalizedGameContent } from '../../../../app/i18n/gameContent';
import type { HowToPlayContent } from '../../../../app/shell/games/howToPlayContent';
import type { LossContent } from '../../../../app/shell/games/lossContent';
import type { PuzzleDifficulty } from '../../../shared/types';
import en from './en';
import nl from './nl';
import de from './de';
import fr from './fr';
import es from './es';

export interface ChimpTestStrings {
  title: string;
  shortTitle: string;
  tagline: string;
  difficultyLabels: Record<PuzzleDifficulty, string>;
  difficultyDescriptions: Record<PuzzleDifficulty, string>;
  play: {
    metadataLabels: {
      round: string;
      difficulty: string;
    };
  };
}

export interface ChimpTestI18n {
  strings: ChimpTestStrings;
  howToPlayGoal: string;
  howToPlayControls: string;
  howToPlayWrongMove: string;
  howToPlayRules: HowToPlayContent['rules'];
  howToPlayTechniques: HowToPlayContent['techniques'];
  howToPlayTips: HowToPlayContent['tips'];
  howToPlayScoring: string;
  loss: LossContent;
}

const CONTENT: LocalizedGameContent<ChimpTestI18n> = {
  en,
  nl,
  de,
  fr,
  es,
};

export function getChimpTestI18n(): ChimpTestI18n {
  return resolveGameContent(getCurrentLanguage(), CONTENT);
}

export function getChimpTestStrings(): ChimpTestStrings {
  return getChimpTestI18n().strings;
}

export function getChimpTestHowToPlay(): HowToPlayContent {
  const i18n = getChimpTestI18n();
  return {
    goal: i18n.howToPlayGoal,
    controls: i18n.howToPlayControls,
    wrongMove: i18n.howToPlayWrongMove,
    rules: i18n.howToPlayRules,
    techniques: i18n.howToPlayTechniques,
    scoring: i18n.howToPlayScoring,
    tips: [...i18n.howToPlayTips],
  };
}

export function getChimpTestLossContent(): LossContent {
  return getChimpTestI18n().loss;
}
