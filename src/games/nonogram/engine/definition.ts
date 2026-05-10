import path from 'path';
import type {
  EngineCatalogEntry,
  EngineGameDefinition,
} from '../../../engine/gameDefinition';
import { createEmptyCells } from '../gameplay/rules/board';
import { solveNonogramFromState } from '../gameplay/rules/solver';
import type { NonogramDifficulty, NonogramPuzzle, NonogramSize } from '../types';
import { classifyNonogramDifficulty } from './difficulty';
import { generateNonogramPuzzle } from './generator';

type NonogramCatalogEntry = NonogramPuzzle & EngineCatalogEntry;

function formatPuzzleEntry(puzzle: NonogramCatalogEntry): string {
  return `  { id: '${puzzle.id}', size: ${puzzle.size}, rows: ${puzzle.rows}, cols: ${puzzle.cols}, difficulty: '${puzzle.difficulty}', solution: '${puzzle.solution}', rowClues: ${JSON.stringify(puzzle.rowClues)}, colClues: ${JSON.stringify(puzzle.colClues)} },`;
}

function normalizePuzzleEntry(puzzle: NonogramCatalogEntry): NonogramCatalogEntry {
  return {
    ...puzzle,
    rows: puzzle.rows ?? puzzle.size,
    cols: puzzle.cols ?? puzzle.size,
  };
}

function toSupportedSize(size: number): NonogramSize | null {
  return size === 5 || size === 10 ? size : null;
}

function listSupportedDifficulties(): readonly NonogramDifficulty[] {
  return ['easy', 'medium'];
}

function reclassifyEntry(entry: NonogramCatalogEntry): NonogramCatalogEntry | null {
  const solveResult = solveNonogramFromState(entry, createEmptyCells(entry.rows, entry.cols));
  const difficulty = classifyNonogramDifficulty(entry, solveResult);
  if (!difficulty) {
    return null;
  }

  return {
    ...entry,
    difficulty,
  };
}

export const nonogramEngineDefinition: EngineGameDefinition<NonogramCatalogEntry> = {
  id: 'nonogram',
  title: 'Nonogram',
  catalogPath: path.resolve(__dirname, '../puzzles/all.ts'),
  entryIdPrefix: 'n',
  catalog: {
    importTypePath: '../types',
    entryTypeName: 'NonogramPuzzle',
    formatEntry: formatPuzzleEntry,
    normalizeParsedEntry: normalizePuzzleEntry,
  },
  listAllowedSizes: () => [5, 10],
  listAllowedDifficulties: () => listSupportedDifficulties(),
  pickTargetDifficulty: (size) => {
    const supported = toSupportedSize(size);
    if (!supported) {
      throw new Error(`Nonogram engine does not support size ${size}.`);
    }

    const difficulties = listSupportedDifficulties();
    return difficulties[Math.floor(Math.random() * difficulties.length)] ?? 'easy';
  },
  generateOne: (size, targetDifficulty) => {
    const supported = toSupportedSize(size);
    if (!supported) {
      throw new Error(`Nonogram engine does not support size ${size}.`);
    }
    if (targetDifficulty !== 'easy' && targetDifficulty !== 'medium') {
      throw new Error(`Nonogram engine does not support difficulty ${targetDifficulty}.`);
    }

    return generateNonogramPuzzle(supported, targetDifficulty);
  },
  getEntryDedupeKey: (entry) => entry.solution,
  reclassifyEntries: (entries) => entries.flatMap((entry) => {
    const normalized = reclassifyEntry(entry);
    return normalized ? [normalized] : [];
  }),
};
