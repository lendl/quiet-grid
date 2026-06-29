import type { PuzzleDifficulty } from '../../shared/types';
import type { WordSearchCatalogEntry } from '../platform/codecs/codec';
import type { WordSearchDirection, WordSearchLanguage } from '../types';
import {
  WORD_SEARCH_DIFFICULTY_CONFIG,
  WORD_SEARCH_QUALITY_THRESHOLDS,
} from './constraints';
import { directionToDelta, toGridKey } from './gridUtils';
import { wordSearchSeedCorpus } from './seedCorpus';

interface Placement {
  id: string;
  word: string;
  start: { row: number; col: number };
  direction: WordSearchDirection;
  bendAt?: number;
  direction2?: WordSearchDirection;
  positions: Array<{ row: number; col: number }>;
}

interface GridBuildResult {
  grid: string[][];
  words: Placement[];
}

interface WordSearchQualityMetrics {
  overlapRatio: number;
  directionEntropy: number;
  spreadRatio: number;
  deadZoneRatio: number;
  score: number;
}

function randomFrom<T>(items: readonly T[]): T {
  return items[Math.floor(Math.random() * items.length)] as T;
}

function clamp01(value: number): number {
  if (value < 0) {
    return 0;
  }
  if (value > 1) {
    return 1;
  }
  return value;
}

function toUpperWord(word: string): string {
  return word
    .normalize('NFKD')
    .replace(/[^A-Za-z]/g, '')
    .toUpperCase();
}

function createEmptyGrid(rows: number, cols: number): string[][] {
  return Array.from({ length: rows }, () => Array.from({ length: cols }, () => ''));
}


function computePlacementScore(
  grid: string[][],
  positions: Array<{ row: number; col: number }>,
  word: string,
  overlapFrequency: number,
  clustering: number,
): number {
  let overlapCount = 0;
  let centerBias = 0;
  const centerRow = (grid.length - 1) / 2;
  const centerCol = ((grid[0]?.length ?? grid.length) - 1) / 2;

  positions.forEach((cell, index) => {
    const existing = grid[cell.row][cell.col];
    if (existing === word[index]) {
      overlapCount += 1;
    }
    centerBias += Math.abs(cell.row - centerRow) + Math.abs(cell.col - centerCol);
  });

  const overlapScore = overlapCount * overlapFrequency * 10;
  const clusterScore = -centerBias * clustering * 0.1;
  return overlapScore + clusterScore + Math.random();
}

function canPlace(grid: string[][], positions: Array<{ row: number; col: number }>, word: string): boolean {
  return positions.every((cell, index) => {
    const existing = grid[cell.row][cell.col];
    return existing === '' || existing === word[index];
  });
}

function placeWord(grid: string[][], positions: Array<{ row: number; col: number }>, word: string): void {
  positions.forEach((cell, index) => {
    grid[cell.row][cell.col] = word[index] ?? '';
  });
}

function normalizeWordToken(value: string): string {
  return value.trim().toUpperCase().replace(/[^A-Z]/g, '');
}


const ORTHOGONAL_DIRECTIONS = ['right', 'left', 'up', 'down'] as const;
type OrthogonalDirection = (typeof ORTHOGONAL_DIRECTIONS)[number];

function perpendicularDirs(dir: OrthogonalDirection): OrthogonalDirection[] {
  return dir === 'right' || dir === 'left' ? ['up', 'down'] : ['right', 'left'];
}

function buildBentPositions(
  rows: number,
  cols: number,
  start: { row: number; col: number },
  direction1: OrthogonalDirection,
  direction2: OrthogonalDirection,
  wordLen: number,
  bendAt: number,
): Array<{ row: number; col: number }> | null {
  const d1 = directionToDelta[direction1];
  const d2 = directionToDelta[direction2];
  const positions: Array<{ row: number; col: number }> = [];

  for (let i = 0; i <= bendAt; i++) {
    const r = start.row + d1.row * i;
    const c = start.col + d1.col * i;
    if (r < 0 || r >= rows || c < 0 || c >= cols) return null;
    positions.push({ row: r, col: c });
  }
  const corner = positions[bendAt]!;
  for (let j = 1; j <= wordLen - 1 - bendAt; j++) {
    const r = corner.row + d2.row * j;
    const c = corner.col + d2.col * j;
    if (r < 0 || r >= rows || c < 0 || c >= cols) return null;
    positions.push({ row: r, col: c });
  }
  return positions.length === wordLen ? positions : null;
}


function hasCoverageViolation(placements: readonly Placement[]): boolean {
  const sets = placements.map((p) => new Set(p.positions.map((c) => toGridKey(c))));
  const unionWithout = (excludeIndex: number): Set<number> => {
    const union = new Set<number>();
    sets.forEach((s, i) => { if (i !== excludeIndex) s.forEach((k) => union.add(k)); });
    return union;
  };
  return sets.some((s, i) => [...s].every((k) => unionWithout(i).has(k)));
}

function positionsMatch(
  a: readonly { row: number; col: number }[],
  b: readonly { row: number; col: number }[],
): boolean {
  if (a.length !== b.length) return false;
  const fwd = a.every((p, i) => p.row === b[i]!.row && p.col === b[i]!.col);
  if (fwd) return true;
  const n = b.length;
  return a.every((p, i) => p.row === b[n - 1 - i]!.row && p.col === b[n - 1 - i]!.col);
}

// Scans the grid once across all directions and checks every placed word in a
// single pass instead of performing a full grid scan per word. Maps each word
// text (and its reverse) to its intended positions so any occurrence at a
// different position is detected immediately.
function hasAnyGhostOccurrence(
  grid: string[][],
  placements: readonly Placement[],
  maxBends: number,
): boolean {
  const rows = grid.length;
  const cols = grid[0]?.length ?? 0;
  const allDirs = Object.keys(directionToDelta) as WordSearchDirection[];

  // Build lookup: word text and its reverse → list of intended position arrays.
  // A list is needed because two words can be reverses of each other.
  const intendedByText = new Map<string, Array<readonly { row: number; col: number }[]>>();
  const addIntended = (text: string, positions: readonly { row: number; col: number }[]) => {
    if (!intendedByText.has(text)) intendedByText.set(text, []);
    intendedByText.get(text)!.push(positions);
  };
  for (const p of placements) {
    addIntended(p.word, p.positions);
    addIntended(p.word.split('').reverse().join(''), p.positions);
  }

  const wordLengths = new Set(placements.map((p) => p.word.length));

  // Single straight-line pass in all 8 directions.
  for (const dir of allDirs) {
    const d = directionToDelta[dir];
    for (let row = 0; row < rows; row += 1) {
      for (let col = 0; col < cols; col += 1) {
        const positions: Array<{ row: number; col: number }> = [];
        let seq = '';
        let r = row;
        let c = col;
        while (r >= 0 && r < rows && c >= 0 && c < cols) {
          const cell = grid[r][c];
          if (cell === '') break; // empty cells are never part of a word
          positions.push({ row: r, col: c });
          seq += cell;
          if (wordLengths.has(seq.length)) {
            const intendedList = intendedByText.get(seq);
            if (intendedList !== undefined) {
              const isIntended = intendedList.some((intended) => positionsMatch(positions, intended));
              if (!isIntended) return true;
            }
          }
          r += d.row;
          c += d.col;
        }
      }
    }
  }

  // Single bent-path pass (hard/expert only).
  if (maxBends >= 1) {
    for (const dir1 of ORTHOGONAL_DIRECTIONS) {
      const d1 = directionToDelta[dir1];
      for (const dir2 of perpendicularDirs(dir1)) {
        const d2 = directionToDelta[dir2];
        for (let row = 0; row < rows; row += 1) {
          for (let col = 0; col < cols; col += 1) {
            let seg1 = '';
            let r1 = row;
            let c1 = col;
            while (r1 >= 0 && r1 < rows && c1 >= 0 && c1 < cols) {
              const cell1 = grid[r1][c1];
              if (cell1 === '') break; // empty cell — no bent path continues through here
              seg1 += cell1;
              if (seg1.length >= 2) {
                let seg2 = '';
                let r2 = r1 + d2.row;
                let c2 = c1 + d2.col;
                while (r2 >= 0 && r2 < rows && c2 >= 0 && c2 < cols) {
                  const cell2 = grid[r2][c2];
                  if (cell2 === '') break; // empty cell — bent path ends here
                  seg2 += cell2;
                  const fullLen = seg1.length + seg2.length;
                  if (wordLengths.has(fullLen)) {
                    const seq = seg1 + seg2;
                    const intendedList = intendedByText.get(seq);
                    if (intendedList !== undefined) {
                      // Build positions only on a potential match.
                      const bentPositions: Array<{ row: number; col: number }> = [];
                      let pr = row;
                      let pc = col;
                      for (let i = 0; i < seg1.length; i += 1) {
                        bentPositions.push({ row: pr, col: pc });
                        pr += d1.row;
                        pc += d1.col;
                      }
                      let qr = r1 + d2.row;
                      let qc = c1 + d2.col;
                      for (let j = 0; j < seg2.length; j += 1) {
                        bentPositions.push({ row: qr, col: qc });
                        qr += d2.row;
                        qc += d2.col;
                      }
                      const isIntended = intendedList.some((intended) => positionsMatch(bentPositions, intended));
                      if (!isIntended) return true;
                    }
                  }
                  r2 += d2.row;
                  c2 += d2.col;
                }
              }
              r1 += d1.row;
              c1 += d1.col;
            }
          }
        }
      }
    }
  }

  return false;
}

// Score a candidate placement, prioritising positions that cover currently-empty
// (uncovered) cells. This steers words toward sparse areas of the grid so the
// final empty-cell count more reliably matches the committed hidden word length.
// Covering currently-uncovered cells is the primary objective; overlap and
// clustering serve as tiebreakers between equally-covering placements.
function scoreBackwardsPlacement(
  grid: string[][],
  positions: Array<{ row: number; col: number }>,
  word: string,
  overlapFrequency: number,
  clustering: number,
  uncovered: ReadonlySet<number>,
): number {
  let overlapCount = 0;
  let uncoveredCoverage = 0;
  let centerBias = 0;
  const centerRow = (grid.length - 1) / 2;
  const centerCol = ((grid[0]?.length ?? grid.length) - 1) / 2;

  positions.forEach((cell, index) => {
    const key = toGridKey(cell);
    if (uncovered.has(key)) {
      uncoveredCoverage += 1;
    }
    const existing = grid[cell.row][cell.col];
    if (existing === word[index]) {
      overlapCount += 1;
    }
    centerBias += Math.abs(cell.row - centerRow) + Math.abs(cell.col - centerCol);
  });

  return uncoveredCoverage * 200
    + overlapCount * overlapFrequency * 10
    - centerBias * clustering * 0.1
    + Math.random();
}

// Place a fixed set of words, scoring each candidate position by how many
// currently-uncovered cells it fills. This steers words toward empty areas
// of the grid, improving fill accuracy so the final empty-cell count matches
// the committed hidden word length more reliably.

function buildGrid(
  words: string[],
  rows: number,
  cols: number,
  difficulty: PuzzleDifficulty,
  seedGrid?: string[][],
): GridBuildResult {
  const config = WORD_SEARCH_DIFFICULTY_CONFIG[difficulty];
  const grid = seedGrid ?? createEmptyGrid(rows, cols);
  const placements: Placement[] = [];

  // Track which cells are still empty so placement scoring can prioritise them.
  // Pre-seeded cells (word letters or '#' sentinels for reserved positions) are
  // excluded — they are not available and must not attract the uncovered-coverage bonus.
  const uncovered = new Set<number>();
  for (let r = 0; r < rows; r += 1) {
    for (let c = 0; c < cols; c += 1) {
      if (grid[r][c] === '') {
        uncovered.add(r * 1000 + c);
      }
    }
  }

  // Reusable position buffer — pre-allocated once per buildGrid call so the hot
  // candidate loop never allocates. Max word length is bounded by max(rows, cols).
  const posBuffer: Array<{ row: number; col: number }> = Array.from(
    { length: Math.max(rows, cols) + 1 },
    () => ({ row: 0, col: 0 }),
  );

  // Fills posBuffer[0..wordLen-1] for a straight path and returns true.
  // Start bounds are pre-validated by the caller so no bounds check is needed.
  function fillStraight(startRow: number, startCol: number, dRow: number, dCol: number, wordLen: number): void {
    for (let i = 0; i < wordLen; i += 1) {
      posBuffer[i].row = startRow + dRow * i;
      posBuffer[i].col = startCol + dCol * i;
    }
  }

  // Fills posBuffer[0..wordLen-1] for a bent path. Returns false if any cell is out of bounds.
  function fillBent(startRow: number, startCol: number, d1Row: number, d1Col: number, d2Row: number, d2Col: number, wordLen: number, bendAt: number): boolean {
    for (let i = 0; i <= bendAt; i += 1) {
      posBuffer[i].row = startRow + d1Row * i;
      posBuffer[i].col = startCol + d1Col * i;
      if (posBuffer[i].row < 0 || posBuffer[i].row >= rows || posBuffer[i].col < 0 || posBuffer[i].col >= cols) return false;
    }
    const cornerRow = posBuffer[bendAt].row;
    const cornerCol = posBuffer[bendAt].col;
    for (let j = 1; j <= wordLen - 1 - bendAt; j += 1) {
      posBuffer[bendAt + j].row = cornerRow + d2Row * j;
      posBuffer[bendAt + j].col = cornerCol + d2Col * j;
      if (posBuffer[bendAt + j].row < 0 || posBuffer[bendAt + j].row >= rows || posBuffer[bendAt + j].col < 0 || posBuffer[bendAt + j].col >= cols) return false;
    }
    return true;
  }

  // Checks posBuffer[0..wordLen-1] for conflicts with the current grid.
  function canPlaceBuffer(wordLen: number, word: string): boolean {
    for (let i = 0; i < wordLen; i += 1) {
      const existing = grid[posBuffer[i].row][posBuffer[i].col];
      if (existing !== '' && existing !== word[i]) return false;
    }
    return true;
  }

  // Scores posBuffer[0..wordLen-1] for placement quality.
  function scoreBuffer(wordLen: number, word: string): number {
    let overlapCount = 0;
    let uncoveredCoverage = 0;
    let centerBias = 0;
    const centerRow = (rows - 1) / 2;
    const centerCol = (cols - 1) / 2;
    for (let i = 0; i < wordLen; i += 1) {
      const cell = posBuffer[i];
      if (uncovered.has(cell.row * 1000 + cell.col)) uncoveredCoverage += 1;
      if (grid[cell.row][cell.col] === word[i]) overlapCount += 1;
      centerBias += Math.abs(cell.row - centerRow) + Math.abs(cell.col - centerCol);
    }
    return uncoveredCoverage * 200
      + overlapCount * config.overlapFrequency * 10
      - centerBias * config.clustering * 0.1
      + Math.random();
  }

  // Snapshots posBuffer[0..wordLen-1] into a new array — called only when a new
  // best placement is found, so allocations happen rarely rather than per-check.
  function snapshotBuffer(wordLen: number): Array<{ row: number; col: number }> {
    return Array.from({ length: wordLen }, (_, i) => ({ row: posBuffer[i].row, col: posBuffer[i].col }));
  }

  for (let wordIndex = 0; wordIndex < words.length; wordIndex += 1) {
    const word = words[wordIndex]!;
    const wordLen = word.length;
    let bestPlacement: Placement | null = null;
    let bestScore = Number.NEGATIVE_INFINITY;

    for (const direction of config.allowedDirections) {
      const delta = directionToDelta[direction];
      const minRow = delta.row < 0 ? wordLen - 1 : 0;
      const maxRow = delta.row > 0 ? rows - wordLen : rows - 1;
      const minCol = delta.col < 0 ? wordLen - 1 : 0;
      const maxCol = delta.col > 0 ? cols - wordLen : cols - 1;

      if (minRow > maxRow || minCol > maxCol) {
        continue;
      }

      for (let row = minRow; row <= maxRow; row += 1) {
        for (let col = minCol; col <= maxCol; col += 1) {
          fillStraight(row, col, delta.row, delta.col, wordLen);
          if (!canPlaceBuffer(wordLen, word)) continue;
          const score = scoreBuffer(wordLen, word);
          if (score > bestScore) {
            bestScore = score;
            bestPlacement = {
              id: `${wordIndex + 1}`,
              word,
              start: { row, col },
              direction,
              positions: snapshotBuffer(wordLen),
            };
          }
        }
      }
    }

    // Bent placements (hard/expert, max 1 orthogonal bend).
    if (config.maxBends >= 1 && wordLen >= 3) {
      for (const direction1 of ORTHOGONAL_DIRECTIONS) {
        const d1 = directionToDelta[direction1];
        for (const direction2 of perpendicularDirs(direction1)) {
          const d2 = directionToDelta[direction2];
          for (let bendAt = 1; bendAt <= wordLen - 2; bendAt += 1) {
            for (let row = 0; row < rows; row += 1) {
              for (let col = 0; col < cols; col += 1) {
                if (!fillBent(row, col, d1.row, d1.col, d2.row, d2.col, wordLen, bendAt)) continue;
                if (!canPlaceBuffer(wordLen, word)) continue;
                const score = scoreBuffer(wordLen, word);
                if (score > bestScore) {
                  bestScore = score;
                  bestPlacement = {
                    id: `${wordIndex + 1}`,
                    word,
                    start: { row, col },
                    direction: direction1,
                    bendAt,
                    direction2,
                    positions: snapshotBuffer(wordLen),
                  };
                }
              }
            }
          }
        }
      }
    }

    if (!bestPlacement) {
      continue;
    }

    placeWord(grid, bestPlacement.positions, bestPlacement.word);
    placements.push(bestPlacement);

    for (const pos of bestPlacement.positions) {
      uncovered.delete(toGridKey(pos));
    }
  }

  return { grid, words: placements };
}

function pickTheme(language: WordSearchLanguage, preferredThemeIds?: readonly string[]) {
  const themes = wordSearchSeedCorpus[language] ?? wordSearchSeedCorpus.en;
  if (preferredThemeIds && preferredThemeIds.length > 0) {
    const preferredThemes = themes.filter((theme) => preferredThemeIds.includes(theme.themeId));
    if (preferredThemes.length > 0) {
      return randomFrom(preferredThemes);
    }
  }
  return randomFrom(themes);
}

function pickLanguage(preferredLanguages?: readonly WordSearchLanguage[]): WordSearchLanguage {
  if (preferredLanguages && preferredLanguages.length > 0) {
    const supported = preferredLanguages.filter((language) => language in wordSearchSeedCorpus);
    if (supported.length > 0) {
      return randomFrom(supported);
    }
  }
  const languages = Object.keys(wordSearchSeedCorpus) as WordSearchLanguage[];
  return randomFrom(languages);
}

function pickWords(
  themeWords: string[],
  difficulty: PuzzleDifficulty,
  rows: number,
  cols: number,
): string[] {
  const config = WORD_SEARCH_DIFFICULTY_CONFIG[difficulty].wordLengthProfile;
  const maxFit = Math.max(rows, cols);
  const candidates = themeWords
    .map(toUpperWord)
    .filter((word) => word.length >= config.min && word.length <= Math.min(config.max, maxFit));

  return [...new Set(candidates)];
}


function calculateDirectionEntropy(placements: readonly Placement[]): number {
  if (placements.length === 0) {
    return 0;
  }
  const counts = new Map<WordSearchDirection, number>();
  placements.forEach((placement) => {
    counts.set(placement.direction, (counts.get(placement.direction) ?? 0) + 1);
  });
  const total = placements.length;
  let entropy = 0;
  counts.forEach((count) => {
    const probability = count / total;
    entropy += -(probability * Math.log2(probability));
  });
  const maxEntropy = Math.log2(Math.max(2, counts.size));
  return maxEntropy <= 0 ? 0 : clamp01(entropy / maxEntropy);
}

function calculateLargestEmptyClusterRatio(rows: number, cols: number, placements: readonly Placement[]): number {
  const occupancy = Array.from({ length: rows }, () => Array.from({ length: cols }, () => false));
  placements.forEach((placement) => {
    placement.positions.forEach((cell) => {
      occupancy[cell.row][cell.col] = true;
    });
  });
  const visited = Array.from({ length: rows }, () => Array.from({ length: cols }, () => false));
  const directions = [
    { row: 1, col: 0 },
    { row: -1, col: 0 },
    { row: 0, col: 1 },
    { row: 0, col: -1 },
  ];
  let maxCluster = 0;

  for (let row = 0; row < rows; row += 1) {
    for (let col = 0; col < cols; col += 1) {
      if (visited[row][col] || occupancy[row][col]) {
        continue;
      }
      const queue = [{ row, col }];
      let head = 0;
      visited[row][col] = true;
      let clusterSize = 0;
      while (head < queue.length) {
        const current = queue[head];
        head += 1;
        clusterSize += 1;
        directions.forEach((direction) => {
          const nextRow = current.row + direction.row;
          const nextCol = current.col + direction.col;
          if (
            nextRow < 0
            || nextCol < 0
            || nextRow >= rows
            || nextCol >= cols
            || visited[nextRow][nextCol]
            || occupancy[nextRow][nextCol]
          ) {
            return;
          }
          visited[nextRow][nextCol] = true;
          queue.push({ row: nextRow, col: nextCol });
        });
      }
      maxCluster = Math.max(maxCluster, clusterSize);
    }
  }

  return clamp01(maxCluster / (rows * cols));
}

function calculateSpreadRatio(rows: number, cols: number, placements: readonly Placement[]): number {
  if (placements.length === 0) {
    return 0;
  }
  const usedRows = new Set<number>();
  const usedCols = new Set<number>();
  placements.forEach((placement) => {
    placement.positions.forEach((cell) => {
      usedRows.add(cell.row);
      usedCols.add(cell.col);
    });
  });
  return clamp01(((usedRows.size / rows) + (usedCols.size / cols)) / 2);
}

function buildQualityMetrics(
  rows: number,
  cols: number,
  placements: readonly Placement[],
): WordSearchQualityMetrics {
  const totalWordLetters = placements.reduce((sum, placement) => sum + placement.word.length, 0);
  const occupied = new Set<number>();
  placements.forEach((placement) => {
    placement.positions.forEach((cell) => {
      occupied.add(toGridKey(cell));
    });
  });
  const occupiedCount = occupied.size;
  const overlapRatio = totalWordLetters === 0
    ? 0
    : clamp01((totalWordLetters - occupiedCount) / totalWordLetters);
  const directionEntropy = calculateDirectionEntropy(placements);
  const spreadRatio = calculateSpreadRatio(rows, cols, placements);
  const deadZoneRatio = calculateLargestEmptyClusterRatio(rows, cols, placements);
  const coverageRatio = clamp01(occupiedCount / (rows * cols));
  const score = clamp01(
    (overlapRatio * 0.25)
    + (directionEntropy * 0.2)
    + (spreadRatio * 0.25)
    + (coverageRatio * 0.2)
    + ((1 - deadZoneRatio) * 0.1),
  );

  return {
    overlapRatio,
    directionEntropy,
    spreadRatio,
    deadZoneRatio,
    score,
  };
}

function passesQualityThreshold(
  difficulty: PuzzleDifficulty,
  quality: WordSearchQualityMetrics,
): boolean {
  const threshold = WORD_SEARCH_QUALITY_THRESHOLDS[difficulty];
  return quality.score >= threshold.minScore
    && quality.overlapRatio >= threshold.minOverlapRatio
    && quality.directionEntropy >= threshold.minDirectionEntropy
    && quality.deadZoneRatio <= threshold.maxDeadZoneRatio;
}

function buildDiversitySignature(rows: number, cols: number, placements: readonly Placement[]): string {
  const words = [...placements].map((placement) => placement.word).sort().join(',');
  const directionCounts = new Map<WordSearchDirection, number>();
  const overlapCounts = new Map<number, number>();
  const occupancy = new Map<number, number>();
  placements.forEach((placement) => {
    directionCounts.set(placement.direction, (directionCounts.get(placement.direction) ?? 0) + 1);
    placement.positions.forEach((cell) => {
      const key = toGridKey(cell);
      occupancy.set(key, (occupancy.get(key) ?? 0) + 1);
    });
  });
  occupancy.forEach((count) => {
    overlapCounts.set(count, (overlapCounts.get(count) ?? 0) + 1);
  });

  const directionMix = [...directionCounts.entries()]
    .sort((left, right) => left[0].localeCompare(right[0]))
    .map(([direction, count]) => `${direction}:${count}`)
    .join('|');
  const overlapHistogram = [...overlapCounts.entries()]
    .sort((left, right) => left[0] - right[0])
    .map(([depth, count]) => `${depth}:${count}`)
    .join('|');
  const anchors = [...placements]
    .map((placement) => {
      const base = `${placement.start.row},${placement.start.col},${placement.direction},${placement.word.length}`;
      return placement.bendAt !== undefined
        ? `${base},b${placement.bendAt},${placement.direction2}`
        : base;
    })
    .sort()
    .join('|');

  return `${rows}x${cols}:${words}:${directionMix}:${overlapHistogram}:${anchors}`;
}

function buildDifficultyRatedScore(
  difficulty: PuzzleDifficulty,
  qualityScore: number,
): number {
  const threshold = WORD_SEARCH_QUALITY_THRESHOLDS[difficulty];
  const normalizedQuality = clamp01(
    (qualityScore - threshold.minScore) / Math.max(0.001, 1 - threshold.minScore),
  );
  const tierBase: Record<PuzzleDifficulty, number> = {
    easy: 0,
    medium: 25,
    hard: 50,
    expert: 75,
  };
  return Number((tierBase[difficulty] + (normalizedQuality * 24.9)).toFixed(1));
}

function buildLetterPool(grid: string[][]): string[] {
  const letters: string[] = [];
  for (const row of grid) {
    for (const cell of row) {
      if (cell !== '' && cell !== '#') letters.push(cell);
    }
  }
  return letters;
}

// Pick `count` random grid positions and return them sorted into reading order
// (top-to-bottom, left-to-right) so the hidden word is read naturally.
function pickReservedPositions(
  rows: number,
  cols: number,
  count: number,
): Array<{ row: number; col: number }> {
  const positions: Array<{ row: number; col: number }> = [];
  for (let r = 0; r < rows; r += 1) {
    for (let c = 0; c < cols; c += 1) {
      positions.push({ row: r, col: c });
    }
  }
  for (let i = 0; i < count && i < positions.length; i += 1) {
    const j = i + Math.floor(Math.random() * (positions.length - i));
    const tmp = positions[i]!;
    positions[i] = positions[j]!;
    positions[j] = tmp;
  }
  return positions
    .slice(0, count)
    .sort((a, b) => (a.row !== b.row ? a.row - b.row : a.col - b.col));
}

// Greedily select `count` words from `allWords` by maximising letter
// intersection with the already-selected set. Words that share letters are
// more likely to overlap when placed, which raises the overlap ratio and
// reduces variance in the empty-cell count. A small random jitter keeps
// results varied across attempts while still preferring compatible words.
function pickByLetterAffinity(allWords: string[], count: number): string[] {
  if (allWords.length <= count) {
    return [...allWords].sort((a, b) => b.length - a.length);
  }

  const shuffled = [...allWords].sort(() => Math.random() - 0.5);

  const selected: string[] = [];
  const remaining = new Set(shuffled);
  const poolFreq = new Map<string, number>();

  // Seed with the longest word in the shuffled subset so large words always
  // get placed when the grid is most open.
  const seed = shuffled.reduce((best, w) => (w.length > best.length ? w : best), shuffled[0]!);
  selected.push(seed);
  remaining.delete(seed);
  for (const ch of seed) {
    poolFreq.set(ch, (poolFreq.get(ch) ?? 0) + 1);
  }

  while (selected.length < count && remaining.size > 0) {
    let bestWord = '';
    let bestScore = Number.NEGATIVE_INFINITY;
    for (const word of remaining) {
      let intersection = 0;
      const seen = new Map<string, number>();
      for (const ch of word) {
        const used = seen.get(ch) ?? 0;
        if (used < (poolFreq.get(ch) ?? 0)) intersection += 1;
        seen.set(ch, used + 1);
      }
      const score = intersection + Math.random() * 0.5;
      if (score > bestScore) {
        bestScore = score;
        bestWord = word;
      }
    }
    selected.push(bestWord);
    remaining.delete(bestWord);
    for (const ch of bestWord) {
      poolFreq.set(ch, (poolFreq.get(ch) ?? 0) + 1);
    }
  }

  return selected.sort((a, b) => b.length - a.length);
}

export function generateWordSearchPuzzle(
  rows: number,
  cols: number,
  difficulty: PuzzleDifficulty,
  options?: { preferredLanguages?: readonly WordSearchLanguage[]; preferredThemeIds?: readonly string[] },
): {
  dedupeKey: string;
  entry: Omit<WordSearchCatalogEntry, 'id'>;
  label: string;
  score: number;
} | null {
  const language = pickLanguage(options?.preferredLanguages);
  const theme = pickTheme(language, options?.preferredThemeIds);
  const allWords = pickWords(theme.words, difficulty, rows, cols);
  if (allWords.length === 0) {
    return null;
  }

  const difficultyConfig = WORD_SEARCH_DIFFICULTY_CONFIG[difficulty];
  const config = difficultyConfig.wordLengthProfile;
  const totalCells = rows * cols;
  const maxHiddenWordLength = Math.max(rows, cols);
  const avgWordLength = (config.min + config.max) / 2;

  // Build a flat pool of hidden word candidates from all theme words in the
  // reachable length range. The hidden word is chosen before grid construction
  // so cells are reserved for it upfront — placement is built around the hidden
  // word rather than the hidden word being determined by leftover cells.
  const hiddenWordPool: string[] = [];
  for (const word of theme.words) {
    const normalized = normalizeWordToken(word);
    if (normalized.length >= 3 && normalized.length <= maxHiddenWordLength) {
      hiddenWordPool.push(normalized);
    }
  }
  if (hiddenWordPool.length === 0) {
    return null;
  }

  let selectedPlacements: Placement[] | null = null;
  let selectedHiddenWord: string | null = null;
  let selectedHiddenPositions: Array<{ row: number; col: number }> | null = null;
  let selectedNoiseFill: Array<{ row: number; col: number; letter: string }> = [];
  let selectedQuality: WordSearchQualityMetrics | null = null;
  let selectedSignature: string | null = null;

  for (let attempt = 0; attempt < 300; attempt += 1) {
    // Pick the hidden word first. Its positions are reserved in the grid before
    // any visible word is placed, so the placer is guaranteed to leave exactly
    // hiddenWord.length empty cells — no exact-match lookup or noise correction needed.
    const hiddenWord = randomFrom(hiddenWordPool);
    const hiddenLength = hiddenWord.length;
    const availableCells = totalCells - hiddenLength;

    // Reserve random positions for the hidden word, sorted into reading order.
    const reservedPositions = pickReservedPositions(rows, cols, hiddenLength);
    const reservedKeys = new Set(reservedPositions.map((p) => p.row * 1000 + p.col));

    // Pre-seed the grid with '#' sentinels. canPlaceBuffer rejects any word that
    // tries to occupy a '#' cell, so the placer avoids these positions naturally.
    const grid = createEmptyGrid(rows, cols);
    for (const pos of reservedPositions) {
      grid[pos.row][pos.col] = '#';
    }

    const targetWordCount = Math.max(
      difficultyConfig.wordCount.min,
      Math.min(
        Math.min(allWords.length, difficultyConfig.wordCount.max, Math.ceil(availableCells / Math.max(1, config.min))),
        Math.round(availableCells / (avgWordLength * difficultyConfig.fillEfficiency)),
      ),
    );
    const candidateWords = pickByLetterAffinity(allWords, targetWordCount);

    const { words: placements } = buildGrid(candidateWords, rows, cols, difficulty, grid);
    if (placements.length === 0) {
      continue;
    }

    if (hasCoverageViolation(placements)) {
      continue;
    }

    // Noise-fill any cells left empty by the placer (excluding reserved positions).
    // These are stored in the catalog entry so the grid can be reproduced exactly.
    const letterPool = buildLetterPool(grid);
    if (letterPool.length > 0) {
      for (let r = 0; r < rows; r += 1) {
        for (let c = 0; c < cols; c += 1) {
          if (grid[r][c] === '' && !reservedKeys.has(r * 1000 + c)) {
            grid[r][c] = randomFrom(letterPool);
          }
        }
      }
    }

    // Clear sentinels — reserved cells are now the hidden word positions.
    for (const pos of reservedPositions) {
      grid[pos.row][pos.col] = '';
    }

    const quality = buildQualityMetrics(rows, cols, placements);
    if (!passesQualityThreshold(difficulty, quality)) {
      continue;
    }

    if (hasAnyGhostOccurrence(grid, placements, difficultyConfig.maxBends)) {
      continue;
    }

    const wordCells = new Set<number>();
    for (const p of placements) {
      for (const pos of p.positions) {
        wordCells.add(pos.row * 1000 + pos.col);
      }
    }
    const noiseFill: Array<{ row: number; col: number; letter: string }> = [];
    for (let r = 0; r < rows; r += 1) {
      for (let c = 0; c < cols; c += 1) {
        const key = r * 1000 + c;
        const letter = grid[r][c];
        if (letter !== '' && !wordCells.has(key) && !reservedKeys.has(key)) {
          noiseFill.push({ row: r, col: c, letter });
        }
      }
    }

    selectedPlacements = placements;
    selectedHiddenWord = hiddenWord;
    selectedHiddenPositions = reservedPositions;
    selectedNoiseFill = noiseFill;
    selectedQuality = quality;
    selectedSignature = buildDiversitySignature(rows, cols, placements);
    break;
  }

  if (!selectedPlacements || !selectedHiddenWord || !selectedHiddenPositions || !selectedQuality || !selectedSignature) {
    return null;
  }

  const entry: Omit<WordSearchCatalogEntry, 'id'> = {
    schemaVersion: 1,
    difficulty,
    rows,
    cols,
    language,
    themeId: theme.themeId,
    words: selectedPlacements.map((placement) => ({
      id: placement.id,
      word: placement.word,
      start: { ...placement.start },
      direction: placement.direction,
      ...(placement.bendAt !== undefined && placement.direction2 !== undefined
        ? { bendAt: placement.bendAt, direction2: placement.direction2 }
        : {}),
    })),
    hiddenWord: {
      word: selectedHiddenWord,
      clue: theme.themeId,
      positions: selectedHiddenPositions,
    },
    noiseFill: selectedNoiseFill,
    diversitySignature: selectedSignature,
    quality: selectedQuality,
  };

  const dedupeKey = `${language}:${theme.themeId}:${difficulty}:${rows}x${cols}:${selectedSignature}`;

  return {
    dedupeKey,
    entry,
    label: `${rows}x${cols} ${difficulty} (${language})`,
    score: buildDifficultyRatedScore(difficulty, selectedQuality.score),
  };
}
