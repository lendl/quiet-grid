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

interface RepairCandidate extends CandidatePlacement {
  word: string;
  score: number;
}

const MAX_REPAIR_STEPS = 20000;
const TABU_TENURE = 15;

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
// Used only by the fast fill pass (Phase 1) — cheap because it doesn't need
// to know how many OTHER words could also work, just the best spot for this
// one word.
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
// (mustCoverRow, mustCoverCol), in any of `directions`. Used by the repair
// search (Phase 2) to guarantee a specific stuck cell gets covered.
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

function isOrthogonallyAdjacent(a: { row: number; col: number }, b: { row: number; col: number }): boolean {
  return Math.abs(a.row - b.row) + Math.abs(a.col - b.col) === 1;
}

function incrementCover(coverCounts: Map<number, number>, key: number): void {
  coverCounts.set(key, (coverCounts.get(key) ?? 0) + 1);
}

// Decrements the cover count for `key` and returns the new count. A cell is
// only safe to clear/reopen once its count reaches 0 — until then, some
// other still-standing placement still needs that letter there.
function decrementCover(coverCounts: Map<number, number>, key: number): number {
  const next = (coverCounts.get(key) ?? 1) - 1;
  if (next <= 0) {
    coverCounts.delete(key);
  } else {
    coverCounts.set(key, next);
  }
  return next;
}

// Phase 2: bounded ITERATIVE local repair for whatever the fast fill
// (Phase 1) left uncovered. No recursion anywhere in this function — it's a
// plain while loop — so there is no stack-depth risk regardless of how many
// repair steps are needed. At each step, finds the most-constrained
// still-uncovered cell (fewest valid candidate words, among words not
// currently tabu) and either places the best candidate directly, or — if
// it has none — evicts an existing placement (from Phase 1 or an earlier
// repair step) that is orthogonally adjacent to the stuck cell, freeing its
// cells back up. `coverCounts` (a per-cell reference count) makes evicting
// ANY placement safe: a cell is only cleared and reopened once every
// placement touching it has been evicted, so removing one word never
// corrupts a letter another, still-standing word still needs there. An
// evicted word is placed on a short "tabu" list forbidding it from being
// re-placed for the next TABU_TENURE steps — without this, the search can
// immediately re-place the same word right back where it was (or somewhere
// that recreates the same conflict), oscillating forever instead of making
// progress; the tabu list forces it to try a genuinely different
// combination before that word becomes available again. This is a
// heuristic, not a complete search — it can fail to find a solution that
// exists — but the caller (generator.ts) retries the whole attempt with
// fresh randomization up to 200 times, which comfortably absorbs that.
// Bounded by MAX_REPAIR_STEPS so it always terminates.
function repairCoverage(
  grid: string[][],
  rows: number,
  cols: number,
  wordPool: readonly string[],
  uncovered: Set<number>,
  usedWords: Set<string>,
  placements: WordPlacement[],
  coverCounts: Map<number, number>,
  nextId: { value: number },
  config: Pick<WordSearchDifficultyConfig, 'allowedDirections' | 'overlapFrequency'>,
): boolean {
  const tabuUntilStep = new Map<string, number>();
  let step = 0;

  while (uncovered.size > 0) {
    step += 1;
    if (step > MAX_REPAIR_STEPS) {
      return false;
    }

    let targetKey: number | null = null;
    let targetCandidates: RepairCandidate[] | null = null;
    for (const key of uncovered) {
      const row = Math.floor(key / 1000);
      const col = key % 1000;
      const candidates: RepairCandidate[] = [];
      for (const word of wordPool) {
        if (usedWords.has(word)) continue;
        if ((tabuUntilStep.get(word) ?? 0) >= step) continue;
        const candidate = findBestPlacementThroughCell(grid, rows, cols, word, config.allowedDirections, row, col, uncovered, config.overlapFrequency);
        if (!candidate) continue;
        candidates.push({
          word,
          ...candidate,
          score: scorePlacement(grid, candidate.positions, word, uncovered, config.overlapFrequency),
        });
      }
      if (targetCandidates === null || candidates.length < targetCandidates.length) {
        targetKey = key;
        targetCandidates = candidates;
      }
      if (targetCandidates.length === 0) break;
    }

    if (targetKey === null || !targetCandidates) {
      return false;
    }

    if (targetCandidates.length > 0) {
      const best = [...targetCandidates].sort((a, b) => b.score - a.score)[0]!;
      const touchedKeys = best.positions.map((cell) => toGridKey(cell));
      placeWord(grid, best.positions, best.word);
      usedWords.add(best.word);
      touchedKeys.forEach((key) => {
        incrementCover(coverCounts, key);
        uncovered.delete(key);
      });
      placements.push({
        id: `${nextId.value}`,
        word: best.word,
        start: best.start,
        direction: best.direction,
        positions: best.positions,
      });
      nextId.value += 1;
      continue;
    }

    // No direct candidate for the most-constrained cell. Evict an existing
    // placement adjacent to it to free up new options — prefer the
    // SMALLEST nearby placement (fewest cells), not the most recent one.
    // Evicting a short word frees a small, easy-to-refill area; evicting a
    // long word can free a large cluster that's disproportionately hard to
    // fill back in, triggering a cascade of further evictions instead of
    // making net progress.
    const targetRow = Math.floor(targetKey / 1000);
    const targetCol = targetKey % 1000;
    const nearbyPlacements = placements.filter((placement) => (
      placement.positions.some((cell) => isOrthogonallyAdjacent(cell, { row: targetRow, col: targetCol }))
    ));

    if (nearbyPlacements.length === 0) {
      return false;
    }

    const evicted = [...nearbyPlacements].sort((a, b) => a.positions.length - b.positions.length)[0]!;
    const index = placements.indexOf(evicted);
    placements.splice(index, 1);
    usedWords.delete(evicted.word);
    evicted.positions.forEach((cell) => {
      const key = toGridKey(cell);
      if (decrementCover(coverCounts, key) === 0) {
        uncovered.add(key);
        grid[cell.row][cell.col] = '';
      }
    });
    tabuUntilStep.set(evicted.word, step + TABU_TENURE);
  }

  return true;
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
  const coverCounts = new Map<number, number>();

  const uncovered = new Set<number>();
  for (let row = 0; row < rows; row += 1) {
    for (let col = 0; col < cols; col += 1) {
      const key = row * 1000 + col;
      if (!reservedCells.has(key)) uncovered.add(key);
    }
  }

  const dedupedPool = [...new Set(wordPool)];

  // Phase 1: fast greedy fill. Cheap — no candidate-counting, just the best
  // spot for each word in turn, longest-first. Handles the easy majority of
  // the grid quickly. Every placement here is tracked in `coverCounts` just
  // like a Phase 2 placement, so Phase 2 can safely undo it later if needed.
  const spreadOrder = [...dedupedPool].sort((a, b) => b.length - a.length);
  let nextIdCounter = 1;
  for (const word of spreadOrder) {
    if (uncovered.size === 0) break;
    const placement = findBestPlacement(grid, rows, cols, word, config.allowedDirections, uncovered, config.overlapFrequency);
    if (!placement) continue;
    placeWord(grid, placement.positions, word);
    placement.positions.forEach((cell) => {
      const key = toGridKey(cell);
      incrementCover(coverCounts, key);
      uncovered.delete(key);
    });
    placements.push({ id: `${nextIdCounter}`, word, start: placement.start, direction: placement.direction, positions: placement.positions });
    nextIdCounter += 1;
  }

  if (uncovered.size === 0) {
    return { grid, placements };
  }

  // Phase 2: bounded iterative local repair for whatever Phase 1 left
  // uncovered (see repairCoverage above).
  const usedWords = new Set(placements.map((p) => p.word));
  const nextId = { value: nextIdCounter };
  const repaired = repairCoverage(grid, rows, cols, dedupedPool, uncovered, usedWords, placements, coverCounts, nextId, config);
  if (!repaired) {
    return null;
  }

  return { grid, placements };
}
