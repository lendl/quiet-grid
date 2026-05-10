import type { NonogramPuzzle } from '../../types';

export function normalizeClues(clues: readonly number[]): number[] {
  return clues.length === 0 ? [0] : [...clues];
}

export function buildLineClues(line: readonly boolean[]): number[] {
  const runs: number[] = [];
  let current = 0;
  line.forEach((cell) => {
    if (cell) {
      current += 1;
      return;
    }

    if (current > 0) {
      runs.push(current);
      current = 0;
    }
  });

  if (current > 0) {
    runs.push(current);
  }

  return normalizeClues(runs);
}

export function buildRowClues(grid: readonly (readonly boolean[])[]): number[][] {
  return grid.map((row) => buildLineClues(row));
}

export function buildColClues(grid: readonly (readonly boolean[])[]): number[][] {
  if (grid.length === 0) {
    return [];
  }

  return Array.from({ length: grid[0]?.length ?? 0 }, (_, colIndex) => (
    buildLineClues(grid.map((row) => row[colIndex] === true))
  ));
}

export function puzzleMatchesClues(
  filledGrid: readonly (readonly boolean[])[],
  puzzle: Pick<NonogramPuzzle, 'rowClues' | 'colClues'>,
): boolean {
  const rowClues = buildRowClues(filledGrid);
  const colClues = buildColClues(filledGrid);
  return JSON.stringify(rowClues) === JSON.stringify(puzzle.rowClues)
    && JSON.stringify(colClues) === JSON.stringify(puzzle.colClues);
}
