import { Dimensions } from 'react-native';
import catalogEntries from '../puzzles/all';
import type { NonogramCatalogEntry } from './codecs/codec';
import {
  buildNonogramCatalogEntry,
  buildNonogramClueSet,
  cloneSolutionGrid,
  applySeededPermutation,
  solutionToKey,
} from '../engine/solver';
import type { NonogramPuzzle, NonogramSession } from '../types';
import { createEmptyNonogramBoard } from '../types';
import { pickRandomPuzzleForDifficulty } from '../../shared/randomPuzzleSelection';
import { createNonogramBoardLayout } from '../../../app/shell/boardLayout';

const MIN_SWIPE_CELL_SIZE = 30;
const CONTAINER_PADDING = 32;

function fitsOnScreen(rows: number, cols: number): boolean {
  const { width } = Dimensions.get('window');
  const containerWidth = Math.max(200, width - CONTAINER_PADDING);
  const worstRowClues = Array.from(
    { length: rows },
    () => Array.from({ length: Math.ceil(cols / 2) }, () => 1),
  );
  const worstColClues = Array.from(
    { length: cols },
    () => Array.from({ length: Math.ceil(rows / 2) }, () => 1),
  );
  const { cellSize } = createNonogramBoardLayout({
    containerWidth,
    rows,
    cols,
    rowClues: worstRowClues,
    colClues: worstColClues,
    interactive: true,
  });
  return cellSize >= MIN_SWIPE_CELL_SIZE;
}

const BUILT_IN_BASE_SPECS = [
  { rows: 5, cols: 5, difficulties: ['easy', 'medium', 'hard', 'expert'] },
  { rows: 10, cols: 5, difficulties: ['easy', 'medium', 'hard', 'expert'] },
  { rows: 10, cols: 10, difficulties: ['medium', 'hard', 'expert'] },
] as const;

const VARIANTS_PER_SPEC = 16;

function createTemplateSolution(
  rows: number,
  cols: number,
  difficulty: NonogramCatalogEntry['difficulty'],
): boolean[][] {
  const rowMid = Math.floor(rows / 2);
  const colMid = Math.floor(cols / 2);
  const rowBand = Math.max(1, Math.floor(rows / 4));
  const colBand = Math.max(1, Math.floor(cols / 4));
  const denseRadius = Math.max(1, Math.floor(Math.min(rows, cols) / 3));

  return Array.from({ length: rows }, (_, row) => Array.from({ length: cols }, (_, col) => {
    switch (difficulty) {
      case 'easy':
        return (
          (Math.abs(row - rowMid) <= rowBand && Math.abs(col - colMid) <= colBand)
          || row === rowMid
          || col === colMid
        );
      case 'medium':
        return (
          Math.abs(row - rowMid) <= 1
          || Math.abs(col - colMid) <= 1
          || Math.abs(row - Math.floor((rows - 1) * (col / Math.max(1, cols - 1)))) <= 1
        );
      case 'hard':
        return row % 3 === 0
          || col % 4 === 0
          || Math.abs((row + col) - Math.floor((rows + cols - 2) / 2)) <= 1;
      case 'expert':
        return ((row * 2 + col * 3 + rows + cols) % 5) < 2
          || ((row + col) % 4 === 0 && ((row * cols + col) % 3 === 0));
      default:
        return Math.abs(row - rowMid) + Math.abs(col - colMid) <= denseRadius;
    }
  }));
}

const BUILT_IN_ENTRIES: NonogramCatalogEntry[] = BUILT_IN_BASE_SPECS.flatMap((spec, index) => (
  spec.difficulties.flatMap((difficulty, difficultyIndex) => {
    const base = createTemplateSolution(spec.rows, spec.cols, difficulty);
    return Array.from({ length: VARIANTS_PER_SPEC }, (_, variant) => buildNonogramCatalogEntry(
      `builtin-${spec.rows}x${spec.cols}-${difficulty}-${index + 1}-${difficultyIndex + 1}-${variant + 1}`,
      difficulty,
      applySeededPermutation(base, index * 100 + difficultyIndex * VARIANTS_PER_SPEC + variant),
    ));
  })
));

function dedupeEntries(entries: NonogramCatalogEntry[]): NonogramCatalogEntry[] {
  const seen = new Set<string>();
  return entries.filter((entry) => {
    const key = solutionToKey(entry.solution);
    if (seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
}

function normalizeCatalogEntry(entry: NonogramCatalogEntry): NonogramCatalogEntry {
  return {
    ...entry,
    rows: entry.solution.length,
    cols: entry.solution[0]?.length ?? 0,
    solution: cloneSolutionGrid(entry.solution),
  };
}

export function getBuiltInNonogramEntries(): NonogramCatalogEntry[] {
  return dedupeEntries(BUILT_IN_ENTRIES.map(normalizeCatalogEntry));
}

export function getAllNonogramEntries(): NonogramCatalogEntry[] {
  return dedupeEntries([
    ...catalogEntries.map(normalizeCatalogEntry),
    ...getBuiltInNonogramEntries(),
  ]);
}

export function getRandomNonogramEntry(difficulty: NonogramCatalogEntry['difficulty']): NonogramCatalogEntry | null {
  return pickRandomPuzzleForDifficulty('nonogram', getAllNonogramEntries(), difficulty);
}

export function buildNonogramPuzzle(entry: NonogramCatalogEntry): NonogramPuzzle {
  const { rowClues, colClues } = buildNonogramClueSet(entry.solution);
  return {
    id: entry.id,
    difficulty: entry.difficulty,
    rows: entry.rows,
    cols: entry.cols,
    rowClues,
    colClues,
  };
}

export function createNonogramSession(entry: NonogramCatalogEntry): NonogramSession {
  return {
    puzzle: buildNonogramPuzzle(entry),
    board: createEmptyNonogramBoard(entry.rows, entry.cols),
    solution: cloneSolutionGrid(entry.solution),
  };
}

export function createRandomNonogramSession(
  difficulty: NonogramCatalogEntry['difficulty'],
): NonogramSession | null {
  const fittingEntries = getAllNonogramEntries().filter(
    (entry) => fitsOnScreen(entry.rows, entry.cols),
  );
  const entry = pickRandomPuzzleForDifficulty('nonogram', fittingEntries, difficulty);
  return entry ? createNonogramSession(entry) : null;
}
