import type { PuzzleDifficulty } from '../../shared/types';
import type { WordSearchCatalogEntry } from '../platform/codecs/codec';
import type { WordSearchDirection, WordSearchLanguage } from '../types';
import {
  WORD_SEARCH_DIFFICULTY_CONFIG,
  WORD_SEARCH_QUALITY_THRESHOLDS,
} from './constraints';
import { collectEmptyCells, directionToDelta, toGridKey } from './gridUtils';
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

function pathSpellsWord(
  grid: string[][],
  positions: readonly { row: number; col: number }[],
  word: string,
): boolean {
  if (positions.length !== word.length) return false;
  return positions.every((p, i) => grid[p.row]?.[p.col] === word[i]);
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

function hasGhostOccurrence(
  grid: string[][],
  wordText: string,
  intendedPositions: readonly { row: number; col: number }[],
  maxBends: number,
): boolean {
  const rows = grid.length;
  const cols = grid[0]?.length ?? 0;
  const rev = wordText.split('').reverse().join('');

  function checkPath(positions: { row: number; col: number }[]): boolean {
    if (positionsMatch(positions, intendedPositions)) return false;
    return pathSpellsWord(grid, positions, wordText)
      || pathSpellsWord(grid, positions, rev);
  }

  // Straight-line scan in all 8 directions
  const allDirs = Object.keys(directionToDelta) as WordSearchDirection[];
  for (const dir of allDirs) {
    const d = directionToDelta[dir];
    for (let row = 0; row < rows; row += 1) {
      for (let col = 0; col < cols; col += 1) {
        const positions: { row: number; col: number }[] = [];
        let valid = true;
        for (let i = 0; i < wordText.length; i += 1) {
          const r = row + d.row * i;
          const c = col + d.col * i;
          if (r < 0 || r >= rows || c < 0 || c >= cols) { valid = false; break; }
          positions.push({ row: r, col: c });
        }
        if (valid && checkPath(positions)) return true;
      }
    }
  }

  // Single-bend scan (orthogonal L-shapes)
  if (maxBends >= 1 && wordText.length >= 3) {
    for (const dir1 of ORTHOGONAL_DIRECTIONS) {
      const d1 = directionToDelta[dir1];
      for (const dir2 of perpendicularDirs(dir1)) {
        const d2 = directionToDelta[dir2];
        for (let bendAt = 1; bendAt <= wordText.length - 2; bendAt += 1) {
          for (let row = 0; row < rows; row += 1) {
            for (let col = 0; col < cols; col += 1) {
              const positions: { row: number; col: number }[] = [];
              let valid = true;
              for (let i = 0; i <= bendAt; i += 1) {
                const r = row + d1.row * i;
                const c = col + d1.col * i;
                if (r < 0 || r >= rows || c < 0 || c >= cols) { valid = false; break; }
                positions.push({ row: r, col: c });
              }
              if (!valid) continue;
              const corner = positions[bendAt]!;
              for (let j = 1; j <= wordText.length - 1 - bendAt; j += 1) {
                const r = corner.row + d2.row * j;
                const c = corner.col + d2.col * j;
                if (r < 0 || r >= rows || c < 0 || c >= cols) { valid = false; break; }
                positions.push({ row: r, col: c });
              }
              if (valid && checkPath(positions)) return true;
            }
          }
        }
      }
    }
  }

  return false;
}

function buildGrid(
  words: string[],
  rows: number,
  cols: number,
  difficulty: PuzzleDifficulty,
): GridBuildResult {
  const config = WORD_SEARCH_DIFFICULTY_CONFIG[difficulty];
  const grid = createEmptyGrid(rows, cols);
  const placements: Placement[] = [];

  for (let wordIndex = 0; wordIndex < words.length; wordIndex += 1) {
    const word = words[wordIndex]!;
    let bestPlacement: Placement | null = null;
    let bestScore = Number.NEGATIVE_INFINITY;

    for (const direction of config.allowedDirections) {
      const delta = directionToDelta[direction];
      const wordLen = word.length;

      // Pre-compute the valid start-cell range for this direction+length.
      // This eliminates all positions where the word would exit the grid,
      // so no bounds check is needed when building positions below.
      const minRow = delta.row < 0 ? wordLen - 1 : 0;
      const maxRow = delta.row > 0 ? rows - wordLen : rows - 1;
      const minCol = delta.col < 0 ? wordLen - 1 : 0;
      const maxCol = delta.col > 0 ? cols - wordLen : cols - 1;

      if (minRow > maxRow || minCol > maxCol) {
        continue;
      }

      for (let row = minRow; row <= maxRow; row += 1) {
        for (let col = minCol; col <= maxCol; col += 1) {
          const positions = Array.from({ length: wordLen }, (_, i) => ({
            row: row + delta.row * i,
            col: col + delta.col * i,
          }));

          if (!canPlace(grid, positions, word)) {
            continue;
          }

          const score = computePlacementScore(
            grid,
            positions,
            word,
            config.overlapFrequency,
            config.clustering,
          );

          if (score > bestScore) {
            bestScore = score;
            bestPlacement = {
              id: `${wordIndex + 1}`,
              word,
              start: { row, col },
              direction,
              positions,
            };
          }
        }
      }
    }

    // Bent placements (hard/expert, max 1 orthogonal bend)
    if (config.maxBends >= 1 && word.length >= 3) {
      for (const direction1 of ORTHOGONAL_DIRECTIONS) {
        for (const direction2 of perpendicularDirs(direction1)) {
          for (let bendAt = 1; bendAt <= word.length - 2; bendAt += 1) {
            for (let row = 0; row < rows; row += 1) {
              for (let col = 0; col < cols; col += 1) {
                const positions = buildBentPositions(rows, cols, { row, col }, direction1, direction2, word.length, bendAt);
                if (!positions || !canPlace(grid, positions, word)) {
                  continue;
                }
                const score = computePlacementScore(grid, positions, word, config.overlapFrequency, config.clustering);
                if (score > bestScore) {
                  bestScore = score;
                  bestPlacement = {
                    id: `${wordIndex + 1}`,
                    word,
                    start: { row, col },
                    direction: direction1,
                    bendAt,
                    direction2,
                    positions,
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
  }

  return {
    grid,
    words: placements,
  };
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
  const maxPlacementWords = Math.min(
    allWords.length,
    difficultyConfig.wordCount.max,
    Math.ceil(totalCells / Math.max(1, config.min)),
  );

  // Pre-build a length → candidates map so each attempt pays O(1) for the hidden-word check
  // instead of scanning all theme words every time.
  const hiddenWordByLength = new Map<number, string[]>();
  for (const word of theme.words) {
    const normalized = normalizeWordToken(word);
    if (normalized.length >= 3) {
      const bucket = hiddenWordByLength.get(normalized.length);
      if (bucket) {
        bucket.push(normalized);
      } else {
        hiddenWordByLength.set(normalized.length, [normalized]);
      }
    }
  }

  const availableHiddenLengths = [...hiddenWordByLength.keys()];
  if (availableHiddenLengths.length === 0) {
    return null;
  }

  // Estimate average word length to guide word-count selection toward a target fill.
  const avgWordLength = (config.min + config.max) / 2;

  let selectedPlacements: Placement[] | null = null;
  let selectedHiddenWord: string | null = null;
  let selectedHiddenPositions: Array<{ row: number; col: number }> | null = null;
  let selectedQuality: WordSearchQualityMetrics | null = null;
  let selectedSignature: string | null = null;

  for (let attempt = 0; attempt < 300; attempt += 1) {
    // Pick a random target hidden-word length and derive how many regular words
    // to place so the grid is filled to leave roughly that many empty cells.
    // Using 0.85 as an empirical fill-efficiency factor (accounts for ~15% overlap).
    const targetHiddenLength = randomFrom(availableHiddenLengths);
    const targetFill = totalCells - targetHiddenLength;
    const estimatedWordCount = Math.round(targetFill / (avgWordLength * 0.85));
    const targetWordCount = Math.max(
      difficultyConfig.wordCount.min,
      Math.min(maxPlacementWords, estimatedWordCount),
    );

    const shuffled = [...allWords].sort(() => Math.random() - 0.5);
    // Sort longest-first so large words are placed when the grid is most open.
    const candidateWords = shuffled.slice(0, targetWordCount).sort((a, b) => b.length - a.length);

    const { grid, words: placements } = buildGrid(candidateWords, rows, cols, difficulty);
    if (placements.length === 0) {
      continue;
    }

    // Reject if any word's cells are fully covered by another word's cells.
    if (hasCoverageViolation(placements)) {
      continue;
    }

    // Check hidden-word match first — it's O(1) and cheaper than the quality BFS.
    const emptyCells = collectEmptyCells(grid);
    const hiddenCandidates = hiddenWordByLength.get(emptyCells.length);
    if (!hiddenCandidates) {
      continue;
    }

    const quality = buildQualityMetrics(rows, cols, placements);
    if (!passesQualityThreshold(difficulty, quality)) {
      continue;
    }

    // Reject if any word accidentally spells itself (forward or backward) at another
    // location in the grid — either as a straight line or single-bend path.
    const ghostDetected = placements.some(
      (p) => hasGhostOccurrence(grid, p.word, p.positions, difficultyConfig.maxBends),
    );
    if (ghostDetected) {
      continue;
    }

    selectedPlacements = placements;
    selectedHiddenWord = randomFrom(hiddenCandidates);
    selectedHiddenPositions = emptyCells;
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
