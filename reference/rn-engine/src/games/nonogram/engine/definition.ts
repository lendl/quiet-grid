import path from 'path';
import type { EngineCatalogEntry, EngineGameDefinition } from '../../../engine/gameDefinition';
import { classifyNonogramEntry } from '../gameplay/analysis/difficulty';
import { generateNonogramPuzzle } from './generator';
import type { NonogramCatalogEntry } from '../platform/codecs/codec';
import { normalizeNonogramCatalogEntry } from '../platform/codecs/codec';
import { solutionToKey } from './solver';

const NONOGRAM_DIFFICULTIES = ['easy', 'medium', 'hard', 'expert'] as const;
const NONOGRAM_SIZE_OPTIONS: Record<number, readonly string[]> = {
  5: ['5x5'],
  10: ['10x5', '10x10'],
  15: ['15x10', '15x15'],
};

export interface NonogramEngineEntry extends EngineCatalogEntry {
  difficulty: NonogramCatalogEntry['difficulty'];
  rows: number;
  cols: number;
  solution: boolean[][];
}

function normalizeParsedEntry(entry: NonogramEngineEntry): NonogramEngineEntry {
  return normalizeNonogramCatalogEntry(entry as unknown as NonogramCatalogEntry) as NonogramEngineEntry;
}

export const nonogramEngineDefinition: EngineGameDefinition<NonogramEngineEntry> = {
  id: 'nonogram',
  title: 'Nonogram',
  catalogPath: path.resolve(__dirname, '../puzzles/all.ts'),
  entryIdPrefix: 'n',
  catalog: {
    importTypePath: '../platform/codecs/codec',
    entryTypeName: 'NonogramCatalogEntry',
    formatEntry: (entry) => `  { id: '${entry.id}', difficulty: '${entry.difficulty}', rows: ${entry.rows}, cols: ${entry.cols}, solution: ${JSON.stringify(entry.solution)} },`,
    normalizeParsedEntry,
  },
  listAllowedSizes: () => [5, 10, 15],
  listAllowedDifficulties: (size) => {
    if (size >= 15) {
      return NONOGRAM_DIFFICULTIES.filter((difficulty) => difficulty !== 'easy');
    }

    return NONOGRAM_DIFFICULTIES;
  },
  pickTargetDifficulty: (size) => {
    const difficulties = size >= 15
      ? NONOGRAM_DIFFICULTIES.filter((difficulty) => difficulty !== 'easy')
      : NONOGRAM_DIFFICULTIES;
    return difficulties[Math.floor(Math.random() * difficulties.length)];
  },
  describeSizeOptions: (size) => NONOGRAM_SIZE_OPTIONS[size] ?? [`${size}x${size}`],
  generateOne: (size, targetDifficulty) => generateNonogramPuzzle(size, targetDifficulty),
  getEntryDedupeKey: (entry) => solutionToKey(entry.solution),
  reclassifyEntries: (entries) => entries.flatMap((entry) => {
    const classification = classifyNonogramEntry(entry as unknown as NonogramCatalogEntry);
    if (!classification) {
      return [entry];
    }

    return [{
      ...entry,
      difficulty: classification.difficulty,
    }];
  }),
};
