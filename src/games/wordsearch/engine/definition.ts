import path from 'path';
import type { EngineCatalogEntry, EngineGameDefinition } from '../../../engine/gameDefinition';
import type { WordSearchCatalogEntry } from '../platform/codecs/codec';
import { normalizeWordSearchCatalogEntry } from '../platform/codecs/codec';
import type { WordSearchLanguage } from '../types';
import { WORD_SEARCH_ALLOWED_SIZES } from './constraints';
import { generateWordSearchPuzzle } from './generator';

const WORD_SEARCH_DIFFICULTIES = ['easy', 'medium', 'hard', 'expert'] as const;

export interface WordSearchEngineEntry extends EngineCatalogEntry, WordSearchCatalogEntry {}
const WORD_SEARCH_LANGUAGES: readonly WordSearchLanguage[] = ['en', 'nl', 'de', 'fr', 'es'];

function toPreferredLanguages(value: readonly string[] | undefined): WordSearchLanguage[] | undefined {
  if (!value || value.length === 0) {
    return undefined;
  }
  const normalized = value.filter((language): language is WordSearchLanguage => (
    WORD_SEARCH_LANGUAGES.includes(language as WordSearchLanguage)
  ));
  return normalized.length > 0 ? normalized : undefined;
}

function formatWordSearchEntry(entry: WordSearchEngineEntry): string {
  return `  { id: '${entry.id}', schemaVersion: 1, difficulty: '${entry.difficulty}', rows: ${entry.rows}, cols: ${entry.cols}, language: '${entry.language}', themeId: '${entry.themeId}', words: ${JSON.stringify(entry.words)}, hiddenWord: ${JSON.stringify(entry.hiddenWord)}, diversitySignature: ${JSON.stringify(entry.diversitySignature)}, quality: ${JSON.stringify(entry.quality)} },`;
}

export const wordSearchEngineDefinition: EngineGameDefinition<WordSearchEngineEntry> = {
  id: 'wordsearch',
  title: 'Word Search',
  catalogPath: path.resolve(__dirname, '../puzzles/all.ts'),
  entryIdPrefix: 'w',
  catalog: {
    importTypePath: '../platform/codecs/codec',
    entryTypeName: 'WordSearchCatalogEntry',
    formatEntry: formatWordSearchEntry,
    normalizeParsedEntry: (entry) => normalizeWordSearchCatalogEntry(entry as WordSearchCatalogEntry) as WordSearchEngineEntry,
  },
  listAllowedSizes: () => WORD_SEARCH_ALLOWED_SIZES,
  listAllowedDifficulties: () => WORD_SEARCH_DIFFICULTIES,
  pickTargetDifficulty: () => WORD_SEARCH_DIFFICULTIES[Math.floor(Math.random() * WORD_SEARCH_DIFFICULTIES.length)],
  describeSizeOptions: (size) => [`${size}x${size}`],
  generateOne: (size, targetDifficulty, context) => generateWordSearchPuzzle(size, targetDifficulty, {
    preferredLanguages: toPreferredLanguages(context?.preferredLanguages),
    preferredThemeIds: context?.preferredThemeIds,
  }),
  getEntryDedupeKey: (entry) => `${entry.language}:${entry.themeId}:${entry.difficulty}:${entry.rows}:${entry.diversitySignature}`,
  reclassifyEntries: (entries) => entries.map((entry) => normalizeWordSearchCatalogEntry(entry)),
};
