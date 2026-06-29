import { getCurrentLanguage } from '../../../../app/i18n';
import { resolveGameContent, type LocalizedGameContent } from '../../../../app/i18n/gameContent';
import type { HowToPlayContent } from '../../../../app/shell/games/howToPlayContent';
import type { LossContent } from '../../../../app/shell/games/lossContent';
import type { Difficulty } from '../../../../app/types';
import en from './en';
import nl from './nl';
import de from './de';
import fr from './fr';
import es from './es';

export interface WordSearchStrings {
  title: string;
  shortTitle: string;
  tagline: string;
  difficultyLabels: Record<Difficulty, string>;
  difficultyDescriptions: Record<Difficulty, string>;
  play: {
    metadataLabels: {
      size: string;
      difficulty: string;
      theme: string;
      found: string;
    };
    noPuzzlesDialog: {
      title: string;
      message(difficultyLabel: string): string;
    };
    helperToggle: {
      show: string;
      hide: string;
    };
    hiddenWord: {
      locked: string;
      revealed(clue: string, word: string): string;
      solvedTitle: string;
      enterMode: string;
      exitMode: string;
      instructions: string;
      resetOnMistake: string;
      nextLetterTitle(clue: string): string;
      nextLetterBody: string;
    };
    nextMove: {
      title(word: string): string;
      body: string;
    };
  };
}

export interface WordSearchI18n {
  strings: WordSearchStrings;
  howToPlay: HowToPlayContent;
  loss: LossContent;
}

const CONTENT: LocalizedGameContent<WordSearchI18n> = {
  en,
  nl,
  de,
  fr,
  es,
};

export function getWordSearchI18n(): WordSearchI18n {
  return resolveGameContent(getCurrentLanguage(), CONTENT);
}

export function getWordSearchStrings(): WordSearchStrings {
  return getWordSearchI18n().strings;
}

export function getWordSearchHowToPlay(): HowToPlayContent {
  return getWordSearchI18n().howToPlay;
}

export function getWordSearchLossContent(): LossContent {
  return getWordSearchI18n().loss;
}

