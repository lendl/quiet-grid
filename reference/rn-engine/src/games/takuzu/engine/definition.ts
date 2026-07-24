import path from 'path';
import {
  analyzeDifficulty,
  DifficultyAnalysisStalledError,
} from '../../../engine/difficultyAnalyzer';
import {
  classifyPuzzleDifficulty,
  computeDifficultyScore,
} from '../../../engine/difficultyScore';
import {
  SUPPORTED_BUCKETS,
  SUPPORTED_PUZZLE_SIZES,
  type DifficultyLabel,
  type SupportedPuzzleSize,
} from '../../../engine/difficultyConfig';
import { gridToHex, maskToHex } from '../../../engine/encoding';
import { generateGrid } from '../../../engine/generator';
import type {
  EngineCatalogEntry,
  EngineGameDefinition,
  EngineGenerateResult,
} from '../../../engine/gameDefinition';
import { generateMask } from '../../../engine/mask';
import type { Puzzle } from '../types';

interface TakuzuCatalogEntry extends Puzzle, EngineCatalogEntry {}

function formatPuzzleEntry(puzzle: TakuzuCatalogEntry): string {
  return `  { id: '${puzzle.id}', size: ${puzzle.size}, rows: ${puzzle.rows}, cols: ${puzzle.cols}, difficulty: '${puzzle.difficulty}', solution: '${puzzle.solution}', mask: '${puzzle.mask}' },`;
}

function normalizePuzzleEntry(puzzle: TakuzuCatalogEntry): TakuzuCatalogEntry {
  return {
    ...puzzle,
    rows: puzzle.rows ?? puzzle.size,
    cols: puzzle.cols ?? puzzle.size,
  };
}

function toSupportedPuzzleSize(size: number): SupportedPuzzleSize | null {
  return size === 6 || size === 8 || size === 10 ? size : null;
}

function getSupportedDifficultiesForSize(size: SupportedPuzzleSize): DifficultyLabel[] {
  return [...new Set(
    SUPPORTED_BUCKETS
      .filter((bucket) => bucket.size === size)
      .map((bucket) => bucket.difficulty),
  )];
}

function generateTakuzuPuzzleWithDifficulty(
  size: SupportedPuzzleSize,
  targetDifficulty: DifficultyLabel,
): EngineGenerateResult<TakuzuCatalogEntry> | null {
  const grid = generateGrid(size);
  if (!grid) {
    return null;
  }

  const solutionHex = gridToHex(grid);
  const maskGrid = generateMask(grid, targetDifficulty);
  if (!maskGrid) {
    return null;
  }

  const maskHex = maskToHex(maskGrid);
  const metrics = analyzeDifficulty(solutionHex, maskHex, size);
  const score = computeDifficultyScore(size, metrics);
  const difficulty = classifyPuzzleDifficulty(size, metrics, score);
  if (difficulty !== targetDifficulty) {
    return null;
  }

  return {
    dedupeKey: solutionHex,
    entry: {
      size,
      rows: size,
      cols: size,
      difficulty,
      solution: solutionHex,
      mask: maskHex,
    },
    label: `${size}x${size} ${difficulty}`,
    score,
  };
}

export const takuzuEngineDefinition: EngineGameDefinition<TakuzuCatalogEntry> = {
  id: 'takuzu',
  title: 'Takuzu',
  catalogPath: path.resolve(__dirname, '../puzzles/all.ts'),
  entryIdPrefix: 'p',
  catalog: {
    importTypePath: '../types',
    entryTypeName: 'Puzzle',
    formatEntry: formatPuzzleEntry,
    normalizeParsedEntry: normalizePuzzleEntry,
  },
  listAllowedSizes: () => SUPPORTED_PUZZLE_SIZES,
  listAllowedDifficulties: (size) => {
    const supportedSize = toSupportedPuzzleSize(size);
    if (!supportedSize) {
      throw new Error(`Takuzu engine does not support size ${size}.`);
    }

    return getSupportedDifficultiesForSize(supportedSize);
  },
  pickTargetDifficulty: (size) => {
    const supportedSize = toSupportedPuzzleSize(size);
    if (!supportedSize) {
      throw new Error(`Takuzu engine does not support size ${size}.`);
    }

    const difficulties = getSupportedDifficultiesForSize(supportedSize);
    return difficulties[Math.floor(Math.random() * difficulties.length)];
  },
  describeSizeOptions: (size) => [`${size}x${size}`],
  generateOne: (size, targetDifficulty) => {
    const supportedSize = toSupportedPuzzleSize(size);
    if (!supportedSize) {
      throw new Error(`Takuzu engine does not support size ${size}.`);
    }

    return generateTakuzuPuzzleWithDifficulty(supportedSize, targetDifficulty as DifficultyLabel);
  },
  getEntryDedupeKey: (entry) => entry.solution,
  reclassifyEntries: (entries) => {
    let stalledCount = 0;

    const rewrittenEntries = entries.flatMap((entry) => {
      let metrics: ReturnType<typeof analyzeDifficulty>;
      try {
        metrics = analyzeDifficulty(entry.solution, entry.mask, entry.size);
      } catch (error) {
        if (error instanceof DifficultyAnalysisStalledError) {
          stalledCount += 1;
          return [];
        }

        throw error;
      }

      const difficultyScore = computeDifficultyScore(entry.size, metrics);
      const difficulty = classifyPuzzleDifficulty(entry.size, metrics, difficultyScore);
      if (!difficulty) {
        return [];
      }

      return [{
        ...entry,
        difficulty,
      }];
    });

    if (stalledCount > 0) {
      console.log(
        `Dropped ${stalledCount} Takuzu puzzle(s) that no longer solve with the current human-only tip set.`,
      );
    }

    return rewrittenEntries;
  },
};
