import type { Cell} from './validator';
import { isLegalCell, hasUniqueLines } from './validator';
import { shuffle } from './encoding';

const MAX_BACKTRACKS = 10_000;

/**
 * Generates a random valid takuzu puzzle solution grid of the given size.
 * Uses constraint-propagation (legal-value filtering) with backtracking.
 * Returns null if generation fails after MAX_BACKTRACKS backtracks.
 */
export function generateGrid(size: number): (0 | 1)[][] | null {
  const grid: Cell[][] = Array.from({ length: size }, () =>
    Array<Cell>(size).fill(null),
  );
  let backtracks = 0;

  function solve(pos: number): boolean {
    if (pos === size * size) {
      return hasUniqueLines(grid as (0 | 1)[][]);
    }

    const r = Math.floor(pos / size);
    const c = pos % size;

    const candidates: (0 | 1)[] = [0, 1];
    shuffle(candidates);

    for (const v of candidates) {
      grid[r][c] = v;
      if (isLegalCell(grid, r, c, size)) {
        if (solve(pos + 1)) return true;
      }
      grid[r][c] = null;
      backtracks++;
      if (backtracks > MAX_BACKTRACKS) return false;
    }

    return false;
  }

  return solve(0) ? (grid as (0 | 1)[][]) : null;
}
