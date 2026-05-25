import type { NonogramCatalogEntry } from '../platform/codecs/codec';

export type NonogramSolutionGrid = boolean[][];

export interface NonogramClueSet {
  rowClues: number[][];
  colClues: number[][];
}

export function cloneSolutionGrid(solution: NonogramSolutionGrid): NonogramSolutionGrid {
  return solution.map((row) => [...row]);
}

export function transposeSolutionGrid(solution: NonogramSolutionGrid): NonogramSolutionGrid {
  const rows = solution.length;
  const cols = solution[0]?.length ?? 0;
  return Array.from({ length: cols }, (_, colIndex) => (
    Array.from({ length: rows }, (_, rowIndex) => solution[rowIndex][colIndex])
  ));
}

export function mirrorSolutionHorizontally(solution: NonogramSolutionGrid): NonogramSolutionGrid {
  return solution.map((row) => [...row].reverse());
}

export function mirrorSolutionVertically(solution: NonogramSolutionGrid): NonogramSolutionGrid {
  return [...solution].reverse().map((row) => [...row]);
}

export function rotateSolution180(solution: NonogramSolutionGrid): NonogramSolutionGrid {
  return mirrorSolutionVertically(mirrorSolutionHorizontally(solution));
}

function createSeededRng(seed: number): () => number {
  let state = (seed ^ 0x9e3779b9) >>> 0;
  return () => {
    state = (state * 1664525 + 1013904223) >>> 0;
    return state / 0x100000000;
  };
}

function shuffleIndexes(length: number, seed: number): number[] {
  const values = Array.from({ length }, (_, index) => index);
  const random = createSeededRng(seed);

  for (let index = values.length - 1; index > 0; index -= 1) {
    const swapIndex = Math.floor(random() * (index + 1));
    [values[index], values[swapIndex]] = [values[swapIndex], values[index]];
  }

  return values;
}

export function buildNonogramClues(line: readonly boolean[]): number[] {
  const clues: number[] = [];
  let run = 0;

  line.forEach((cell) => {
    if (cell) {
      run += 1;
      return;
    }

    if (run > 0) {
      clues.push(run);
      run = 0;
    }
  });

  if (run > 0) {
    clues.push(run);
  }

  return clues.length > 0 ? clues : [0];
}

export function buildNonogramClueSet(solution: NonogramSolutionGrid): NonogramClueSet {
  const rowClues = solution.map((row) => buildNonogramClues(row));
  const colClues = Array.from({ length: solution[0]?.length ?? 0 }, (_, colIndex) => (
    buildNonogramClues(solution.map((row) => row[colIndex]))
  ));

  return { rowClues, colClues };
}

export function buildNonogramCatalogEntry(
  id: string,
  difficulty: NonogramCatalogEntry['difficulty'],
  solution: NonogramSolutionGrid,
): NonogramCatalogEntry {
  return {
    id,
    difficulty,
    rows: solution.length,
    cols: solution[0]?.length ?? 0,
    solution: cloneSolutionGrid(solution),
  };
}

export function solutionToKey(solution: NonogramSolutionGrid): string {
  return JSON.stringify(solution);
}

export function applySymmetryVariant(
  solution: NonogramSolutionGrid,
  variantIndex: number,
): NonogramSolutionGrid {
  switch (variantIndex % 4) {
    case 0:
      return cloneSolutionGrid(solution);
    case 1:
      return mirrorSolutionHorizontally(solution);
    case 2:
      return mirrorSolutionVertically(solution);
    default:
      return rotateSolution180(solution);
  }
}

export function applySeededPermutation(
  solution: NonogramSolutionGrid,
  seed: number,
  allowTranspose = false,
): NonogramSolutionGrid {
  const source = allowTranspose && seed % 2 !== 0
    ? transposeSolutionGrid(solution)
    : cloneSolutionGrid(solution);
  const rowOrder = shuffleIndexes(source.length, seed * 31 + 7);
  const colOrder = shuffleIndexes(source[0]?.length ?? 0, seed * 17 + 13);

  return rowOrder.map((rowIndex) => colOrder.map((colIndex) => source[rowIndex][colIndex]));
}
