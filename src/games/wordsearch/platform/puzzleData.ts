import { getCurrentLanguage } from '../../../app/i18n';
import type { PuzzleDifficulty } from '../../shared/types';
import { pickRandomPuzzleForDifficulty } from '../../shared/randomPuzzleSelection';
import allEntries from '../puzzles/all';
import {
  cloneWordSearchPuzzle,
  type WordSearchLanguage,
  type WordSearchSession,
} from '../types';
import {
  materializeWordSearchCatalogEntry,
  type WordSearchCatalogEntry,
  type WordSearchResolvedCatalogEntry,
} from './codecs/codec';

function getEntriesForLanguage(language: WordSearchLanguage): WordSearchCatalogEntry[] {
  return allEntries.filter((entry) => entry.language === language);
}

function toWordSearchLanguage(language: ReturnType<typeof getCurrentLanguage>): WordSearchLanguage {
  if (language === 'nl' || language === 'de' || language === 'fr' || language === 'es') {
    return language;
  }

  return 'en';
}

export function getWordSearchEntriesForLanguage(language: WordSearchLanguage): WordSearchResolvedCatalogEntry[] {
  return getEntriesForLanguage(language).map((entry) => materializeWordSearchCatalogEntry(entry));
}

export function getRandomWordSearchEntry(
  difficulty: PuzzleDifficulty,
  language = toWordSearchLanguage(getCurrentLanguage()),
): WordSearchResolvedCatalogEntry | null {
  const localizedEntries = getWordSearchEntriesForLanguage(language);
  const localizedPick = pickRandomPuzzleForDifficulty(`wordsearch:${language}`, localizedEntries, difficulty);
  if (localizedPick) {
    return localizedPick;
  }

  const englishEntries = getWordSearchEntriesForLanguage('en');
  return pickRandomPuzzleForDifficulty('wordsearch:en', englishEntries, difficulty);
}

export function createWordSearchSession(entry: WordSearchResolvedCatalogEntry): WordSearchSession {
  return {
    puzzle: cloneWordSearchPuzzle(entry),
    foundWordIds: [],
    tempSelection: null,
    hiddenWordMode: false,
    hiddenWordProgress: [],
    hiddenWordSolved: false,
  };
}

export function createRandomWordSearchSession(difficulty: PuzzleDifficulty): WordSearchSession | null {
  const entry = getRandomWordSearchEntry(difficulty);
  return entry ? createWordSearchSession(entry) : null;
}
