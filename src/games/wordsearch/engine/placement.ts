import type { WordSearchDirection } from '../types';
import { directionToDelta, toGridKey } from './gridUtils';
import type { WordSearchDifficultyConfig } from './constraints';

export interface WordPlacement {
  id: string;
  word: string;
  start: { row: number; col: number };
  direction: WordSearchDirection;
  positions: Array<{ row: number; col: number }>;
}

export interface PlacementResult {
  grid: string[][];
  placements: WordPlacement[];
}

interface CandidatePlacement {
  start: { row: number; col: number };
  direction: WordSearchDirection;
  positions: Array<{ row: number; col: number }>;
}

function createGrid(rows: number, cols: number, reserved: ReadonlySet<number>): string[][] {
  const grid: string[][] = Array.from({ length: rows }, () => Array.from({ length: cols }, () => ''));
  reserved.forEach((key) => {
    const row = Math.floor(key / 1000);
    const col = key % 1000;
    grid[row][col] = '#';
  });
  return grid;
}

function buildStraightPositions(
  startRow: number,
  startCol: number,
  dRow: number,
  dCol: number,
  wordLen: number,
): Array<{ row: number; col: number }> {
  return Array.from({ length: wordLen }, (_, i) => ({ row: startRow + dRow * i, col: startCol + dCol * i }));
}

function canPlace(grid: string[][], positions: readonly { row: number; col: number }[], word: string): boolean {
  return positions.every((cell, index) => {
    const existing = grid[cell.row][cell.col];
    return existing === '' || existing === word[index];
  });
}

function placeWord(grid: string[][], positions: readonly { row: number; col: number }[], word: string): void {
  positions.forEach((cell, index) => { grid[cell.row][cell.col] = word[index]!; });
}

function scorePlacement(
  grid: string[][],
  positions: readonly { row: number; col: number }[],
  word: string,
  uncovered: ReadonlySet<number>,
  overlapFrequency: number,
): number {
  let overlapCount = 0;
  let uncoveredCoverage = 0;
  positions.forEach((cell, index) => {
    const key = toGridKey(cell);
    if (uncovered.has(key)) uncoveredCoverage += 1;
    if (grid[cell.row][cell.col] === word[index]) overlapCount += 1;
  });
  return uncoveredCoverage * 100 + overlapCount * overlapFrequency * 10 + Math.random();
}

// Finds the best-scoring valid straight-line placement for `word` anywhere in
// the grid, across `directions`. Returns null if the word cannot be placed.
function findBestPlacement(
  grid: string[][],
  rows: number,
  cols: number,
  word: string,
  directions: readonly WordSearchDirection[],
  uncovered: ReadonlySet<number>,
  overlapFrequency: number,
): CandidatePlacement | null {
  const wordLen = word.length;
  let best: CandidatePlacement | null = null;
  let bestScore = Number.NEGATIVE_INFINITY;

  for (const direction of directions) {
    const delta = directionToDelta[direction];
    const minRow = delta.row < 0 ? wordLen - 1 : 0;
    const maxRow = delta.row > 0 ? rows - wordLen : rows - 1;
    const minCol = delta.col < 0 ? wordLen - 1 : 0;
    const maxCol = delta.col > 0 ? cols - wordLen : cols - 1;
    if (minRow > maxRow || minCol > maxCol) continue;

    for (let row = minRow; row <= maxRow; row += 1) {
      for (let col = minCol; col <= maxCol; col += 1) {
        const positions = buildStraightPositions(row, col, delta.row, delta.col, wordLen);
        if (!canPlace(grid, positions, word)) continue;
        const score = scorePlacement(grid, positions, word, uncovered, overlapFrequency);
        if (score > bestScore) {
          bestScore = score;
          best = { start: { row, col }, direction, positions };
        }
      }
    }
  }
  return best;
}

// Finds the best-scoring valid placement of `word` that passes through
// (mustCoverRow, mustCoverCol), in any of `directions`. Used by the closure
// phase to guarantee a specific stuck cell gets covered.
function findBestPlacementThroughCell(
  grid: string[][],
  rows: number,
  cols: number,
  word: string,
  directions: readonly WordSearchDirection[],
  mustCoverRow: number,
  mustCoverCol: number,
  uncovered: ReadonlySet<number>,
  overlapFrequency: number,
): CandidatePlacement | null {
  const wordLen = word.length;
  let best: CandidatePlacement | null = null;
  let bestScore = Number.NEGATIVE_INFINITY;

  for (const direction of directions) {
    const delta = directionToDelta[direction];
    for (let i = 0; i < wordLen; i += 1) {
      const startRow = mustCoverRow - delta.row * i;
      const startCol = mustCoverCol - delta.col * i;
      const endRow = startRow + delta.row * (wordLen - 1);
      const endCol = startCol + delta.col * (wordLen - 1);
      if (startRow < 0 || startRow >= rows || startCol < 0 || startCol >= cols) continue;
      if (endRow < 0 || endRow >= rows || endCol < 0 || endCol >= cols) continue;

      const positions = buildStraightPositions(startRow, startCol, delta.row, delta.col, wordLen);
      if (!canPlace(grid, positions, word)) continue;
      const score = scorePlacement(grid, positions, word, uncovered, overlapFrequency);
      if (score > bestScore) {
        bestScore = score;
        best = { start: { row: startRow, col: startCol }, direction, positions };
      }
    }
  }
  return best;
}

export function buildFullCoverageGrid(
  rows: number,
  cols: number,
  wordPool: readonly string[],
  reservedCells: ReadonlySet<number>,
  config: Pick<WordSearchDifficultyConfig, 'allowedDirections' | 'overlapFrequency'>,
): PlacementResult | null {
  const grid = createGrid(rows, cols, reservedCells);
  const placements: WordPlacement[] = [];

  const uncovered = new Set<number>();
  for (let row = 0; row < rows; row += 1) {
    for (let col = 0; col < cols; col += 1) {
      const key = row * 1000 + col;
      if (!reservedCells.has(key)) uncovered.add(key);
    }
  }

  // Spread phase: place words longest-first, biased toward covering
  // currently-uncovered cells. Longest-first is the standard greedy
  // heuristic for word search placement — big words are hardest to fit, so
  // they get the emptiest possible grid.
  const spreadOrder = [...new Set(wordPool)].sort((a, b) => b.length - a.length);
  let nextId = 1;
  for (const word of spreadOrder) {
    if (uncovered.size === 0) break;
    const placement = findBestPlacement(grid, rows, cols, word, config.allowedDirections, uncovered, config.overlapFrequency);
    if (!placement) continue;
    placeWord(grid, placement.positions, word);
    placements.push({ id: `${nextId}`, word, start: placement.start, direction: placement.direction, positions: placement.positions });
    nextId += 1;
    placement.positions.forEach((cell) => uncovered.delete(toGridKey(cell)));
  }

  // Closure phase: guarantee 100% coverage. For every cell still uncovered,
  // find any unused pool word that can be placed through it; fail the whole
  // attempt if none exists — the caller retries with fresh randomization.
  const usedWords = new Set(placements.map((p) => p.word));
  const remainingUncovered = [...uncovered].sort((a, b) => a - b);
  for (const key of remainingUncovered) {
    if (!uncovered.has(key)) continue;
    const row = Math.floor(key / 1000);
    const col = key % 1000;

    let bestClosure: (CandidatePlacement & { word: string }) | null = null;
    let bestClosureScore = Number.NEGATIVE_INFINITY;
    for (const word of wordPool) {
      if (usedWords.has(word)) continue;
      const candidate = findBestPlacementThroughCell(grid, rows, cols, word, config.allowedDirections, row, col, uncovered, config.overlapFrequency);
      if (!candidate) continue;
      const score = scorePlacement(grid, candidate.positions, word, uncovered, config.overlapFrequency);
      if (score > bestClosureScore) {
        bestClosureScore = score;
        bestClosure = { word, ...candidate };
      }
    }

    if (!bestClosure) {
      return null;
    }

    placeWord(grid, bestClosure.positions, bestClosure.word);
    placements.push({ id: `${nextId}`, word: bestClosure.word, start: bestClosure.start, direction: bestClosure.direction, positions: bestClosure.positions });
    nextId += 1;
    usedWords.add(bestClosure.word);
    bestClosure.positions.forEach((cell) => uncovered.delete(toGridKey(cell)));
  }

  return { grid, placements };
}
