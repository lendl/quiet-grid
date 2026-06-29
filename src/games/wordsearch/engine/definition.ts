import path from 'path';
import type { EngineCatalogEntry, EngineGameDefinition } from '../../../engine/gameDefinition';
import type { WordSearchCatalogEntry } from '../platform/codecs/codec';
import { normalizeWordSearchCatalogEntry } from '../platform/codecs/codec';
import type { WordSearchLanguage } from '../types';
import { WORD_SEARCH_DIFFICULTY_CONFIG } from './constraints';
import { generateWordSearchPuzzle } from './generator';

const WORD_SEARCH_DIFFICULTIES = ['easy', 'medium', 'hard', 'expert'] as const;

export interface WordSearchEngineEntry extends EngineCatalogEntry, WordSearchCatalogEntry {}
const WORD_SEARCH_LANGUAGES: readonly WordSearchLanguage[] = ['en', 'nl', 'de', 'fr', 'es'];

// Encode {rows, cols} as a single number for the EngineGameDefinition size interface.
// Convention: rows * 100 + cols (valid for grid dimensions up to 99).
function encodeSize(rows: number, cols: number): number {
  return rows * 100 + cols;
}

function decodeSize(encoded: number): { rows: number; cols: number } {
  return { rows: Math.floor(encoded / 100), cols: encoded % 100 };
}

function listAllowedSizes(): readonly number[] {
  const sizes = new Set<number>();
  for (const difficulty of WORD_SEARCH_DIFFICULTIES) {
    for (const { rows, cols } of WORD_SEARCH_DIFFICULTY_CONFIG[difficulty].sizeOptions) {
      sizes.add(encodeSize(rows, cols));
    }
  }
  return [...sizes].sort((a, b) => a - b);
}

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
  return `  { id: '${entry.id}', schemaVersion: 1, difficulty: '${entry.difficulty}', rows: ${entry.rows}, cols: ${entry.cols}, language: '${entry.language}', themeId: '${entry.themeId}', words: ${JSON.stringify(entry.words)}, hiddenWord: ${JSON.stringify(entry.hiddenWord)}, noiseFill: ${JSON.stringify(entry.noiseFill)}, diversitySignature: ${JSON.stringify(entry.diversitySignature)}, quality: ${JSON.stringify(entry.quality)} },`;
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
  listAllowedSizes,
  listAllowedDifficulties: (encodedSize) => {
    const { rows, cols } = decodeSize(encodedSize);
    return WORD_SEARCH_DIFFICULTIES.filter((d) =>
      WORD_SEARCH_DIFFICULTY_CONFIG[d].sizeOptions.some((s) => s.rows === rows && s.cols === cols),
    );
  },
  pickTargetDifficulty: (encodedSize) => {
    const { rows, cols } = decodeSize(encodedSize);
    const allowed = WORD_SEARCH_DIFFICULTIES.filter((d) =>
      WORD_SEARCH_DIFFICULTY_CONFIG[d].sizeOptions.some((s) => s.rows === rows && s.cols === cols),
    );
    const pool = allowed.length > 0 ? allowed : WORD_SEARCH_DIFFICULTIES;
    return pool[Math.floor(Math.random() * pool.length)]!;
  },
  describeSizeOptions: (encodedSize) => {
    const { rows, cols } = decodeSize(encodedSize);
    return [`${rows}x${cols}`];
  },
  generateOne: (encodedSize, targetDifficulty, context) => {
    const { rows, cols } = decodeSize(encodedSize);
    return generateWordSearchPuzzle(rows, cols, targetDifficulty, {
      preferredLanguages: toPreferredLanguages(context?.preferredLanguages),
      preferredThemeIds: context?.preferredThemeIds,
    });
  },
  getEntryDedupeKey: (entry) => `${entry.language}:${entry.themeId}:${entry.difficulty}:${entry.rows}x${entry.cols}:${entry.diversitySignature}`,
  reclassifyEntries: (entries) => entries.map((entry) => normalizeWordSearchCatalogEntry(entry)),
};
