import type { WordSearchLanguage } from '../types';
import enThemes from '../puzzles/seeds/en';
import nlThemes from '../puzzles/seeds/nl';
import deThemes from '../puzzles/seeds/de';
import frThemes from '../puzzles/seeds/fr';
import esThemes from '../puzzles/seeds/es';

export interface WordSearchThemeSeed {
  themeId: string;
  words: string[];
}

export type WordSearchSeedCorpus = Record<WordSearchLanguage, WordSearchThemeSeed[]>;

export const wordSearchSeedCorpus: WordSearchSeedCorpus = {
  en: enThemes,
  nl: nlThemes,
  de: deThemes,
  fr: frThemes,
  es: esThemes,
};

export function listWordSearchLanguages(): WordSearchLanguage[] {
  return Object.keys(wordSearchSeedCorpus) as WordSearchLanguage[];
}
