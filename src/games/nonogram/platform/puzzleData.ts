import type { PuzzleDifficulty } from '../../shared/types';
import type {
  NonogramDifficulty,
  NonogramPuzzle,
} from '../types';
import allPuzzles from '../puzzles/all';

type PuzzleMap = Record<NonogramDifficulty, NonogramPuzzle[]>;

function normalizePuzzle(puzzle: NonogramPuzzle): NonogramPuzzle {
  return {
    ...puzzle,
    rows: puzzle.rows ?? puzzle.size,
    cols: puzzle.cols ?? puzzle.size,
  };
}

function buildPuzzleMap(puzzles: readonly NonogramPuzzle[]): PuzzleMap {
  return {
    easy: puzzles.filter((puzzle) => puzzle.difficulty === 'easy').map(normalizePuzzle),
    medium: puzzles.filter((puzzle) => puzzle.difficulty === 'medium').map(normalizePuzzle),
  };
}

export const PUZZLES: PuzzleMap = buildPuzzleMap(allPuzzles);

export function getRandomPuzzle(difficulty: PuzzleDifficulty): NonogramPuzzle | null {
  if (difficulty !== 'easy' && difficulty !== 'medium') {
    return null;
  }

  const pool = PUZZLES[difficulty];
  if (pool.length === 0) {
    return null;
  }

  return pool[Math.floor(Math.random() * pool.length)] ?? null;
}

function hexToBits(hex: string, total: number): number[] {
  const bits: number[] = [];
  for (const char of hex) {
    const value = parseInt(char, 16);
    bits.push((value >> 3) & 1, (value >> 2) & 1, (value >> 1) & 1, value & 1);
  }
  return bits.slice(0, total);
}

function bitsToHex(bits: readonly number[]): string {
  const padded = [...bits];
  while (padded.length % 4 !== 0) {
    padded.push(0);
  }

  let result = '';
  for (let index = 0; index < padded.length; index += 4) {
    const nibble = (padded[index] << 3)
      | (padded[index + 1] << 2)
      | (padded[index + 2] << 1)
      | padded[index + 3];
    result += nibble.toString(16);
  }
  return result;
}

export function decodeSolutionBits(solution: string, rows: number, cols: number): boolean[] {
  return hexToBits(solution, rows * cols).map((bit) => bit === 1);
}

export function decodeSolutionGrid(solution: string, rows: number, cols: number): boolean[][] {
  const bits = decodeSolutionBits(solution, rows, cols);
  return Array.from({ length: rows }, (_, rowIndex) => (
    Array.from({ length: cols }, (_, colIndex) => bits[rowIndex * cols + colIndex] === true)
  ));
}

export function encodeSolutionGrid(grid: readonly (readonly boolean[])[]): string {
  const bits = grid.flatMap((row) => row.map((cell) => cell ? 1 : 0));
  return bitsToHex(bits);
}
