import type { PuzzleDifficulty } from '../../shared/types';
import type { TakuzuSolutionGrid, CellValue, Grid, Puzzle, PuzzleSize } from './types';
import allPuzzles from '../puzzles/all';

type PuzzleMap = Record<PuzzleDifficulty, Puzzle[]>;

function buildPuzzleMap(puzzles: Puzzle[]): PuzzleMap {
  return {
    easy: puzzles.filter(p => p.difficulty === 'easy'),
    medium: puzzles.filter(p => p.difficulty === 'medium'),
    hard: puzzles.filter(p => p.difficulty === 'hard'),
    expert: puzzles.filter(p => p.difficulty === 'expert'),
  };
}

export const PUZZLES: PuzzleMap = buildPuzzleMap(allPuzzles);

/** Returns a random puzzle for the given difficulty, or null if none available. */
export function getRandomPuzzle(difficulty: PuzzleDifficulty): Puzzle | null {
  const pool = PUZZLES[difficulty] ?? [];
  if (pool.length === 0) return null;
  const puzzle = pool[Math.floor(Math.random() * pool.length)];
  return {
    ...puzzle,
    rows: puzzle.rows ?? puzzle.size,
    cols: puzzle.cols ?? puzzle.size,
  };
}

export function getAvailableSizesForDifficulty(difficulty: PuzzleDifficulty): PuzzleSize[] {
  const pool = PUZZLES[difficulty] ?? [];
  return [...new Set(pool.map(puzzle => puzzle.size))].sort((a, b) => a - b);
}

/** Deep-copies a grid so mutations don't affect the source. */
export function cloneGrid(grid: Grid): Grid {
  return grid.map(row => [...row] as CellValue[]);
}

/** Converts a hex-encoded bitstring to an array of bits (0 or 1), trimmed to size*size. */
function hexToBits(hex: string, total: number): number[] {
  const bits: number[] = [];
  for (const ch of hex) {
    const val = parseInt(ch, 16);
    bits.push((val >> 3) & 1, (val >> 2) & 1, (val >> 1) & 1, val & 1);
  }
  return bits.slice(0, total);
}

/** Decodes the solution hex string into a full Grid of 0s and 1s. */
export function decodeSolution(solution: string, size: number): TakuzuSolutionGrid {
  const bits = hexToBits(solution, size * size);
  return Array.from({ length: size }, (_, r) =>
    Array.from({ length: size }, (_, c) => bits[r * size + c] as 0 | 1),
  );
}

/** Decodes the mask hex string into a boolean grid (true = cell is given). */
export function decodeMask(mask: string, size: number): boolean[][] {
  const bits = hexToBits(mask, size * size);
  return Array.from({ length: size }, (_, r) =>
    Array.from({ length: size }, (_, c) => bits[r * size + c] === 1),
  );
}

/** Reconstructs the starting puzzle grid (given cells filled, rest null). */
export function decodePuzzle(solution: string, mask: string, size: number): Grid {
  const solBits  = hexToBits(solution, size * size);
  const maskBits = hexToBits(mask, size * size);
  return Array.from({ length: size }, (_, r) =>
    Array.from({ length: size }, (_, c) => {
      const i = r * size + c;
      return maskBits[i] === 1 ? (solBits[i] as CellValue) : null;
    }),
  );
}


