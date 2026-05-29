export * from '../gameplay/analysis';

import path from 'path';
import type { EngineCatalogEntry, EngineGameDefinition } from '../../../engine/gameDefinition';
import { SUDOKU_ENGINE_DIFFICULTIES } from '../gameplay/analysis';
import {
  classifySudokuEntry,
  generateSudokuPuzzle,
  getSudokuEntryDedupeKey,
} from './generator';
import type { SudokuCatalogEntry } from '../platform/codecs/codec';
import { normalizeSudokuCatalogEntry } from '../platform/codecs/codec';

export interface SudokuEngineEntry extends EngineCatalogEntry, SudokuCatalogEntry {}

function formatSudokuEntry(entry: SudokuEngineEntry): string {
  return `  { id: '${entry.id}', difficulty: '${entry.difficulty}', rows: ${entry.rows}, cols: ${entry.cols}, givens: ${JSON.stringify(entry.givens)}, solution: ${JSON.stringify(entry.solution)} },`;
}

export const sudokuEngineDefinition: EngineGameDefinition<SudokuEngineEntry> = {
  id: 'sudoku',
  title: 'Sudoku',
  catalogPath: path.resolve(__dirname, '../puzzles/all.ts'),
  entryIdPrefix: 's',
  catalog: {
    importTypePath: '../platform/codecs/codec',
    entryTypeName: 'SudokuCatalogEntry',
    formatEntry: formatSudokuEntry,
    normalizeParsedEntry: normalizeSudokuCatalogEntry,
  },
  listAllowedSizes: () => [9],
  listAllowedDifficulties: () => SUDOKU_ENGINE_DIFFICULTIES,
  pickTargetDifficulty: () => SUDOKU_ENGINE_DIFFICULTIES[Math.floor(Math.random() * SUDOKU_ENGINE_DIFFICULTIES.length)],
  describeSizeOptions: (size) => [`${size}x${size}`],
  generateOne: (size, targetDifficulty) => {
    if (size !== 9) {
      throw new Error(`Sudoku engine only supports size 9. Received ${size}.`);
    }

    return generateSudokuPuzzle(targetDifficulty);
  },
  getEntryDedupeKey: (entry) => getSudokuEntryDedupeKey(entry),
  reclassifyEntries: (entries) => entries.flatMap((entry) => {
    const normalized = normalizeSudokuCatalogEntry(entry);
    const classification = classifySudokuEntry(normalized);
    if (!classification) {
      return [];
    }

    return [{
      ...normalized,
      difficulty: classification.difficulty,
    }];
  }),
};
