import type { PuzzleDifficulty } from '../../shared/types';
import type { WordSearchCatalogEntry } from '../platform/codecs/codec';
import type { WordSearchDirection, WordSearchLanguage } from '../types';
import {
  WORD_SEARCH_ALLOWED_SIZES,
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

function createEmptyGrid(size: number): string[][] {
  return Array.from({ length: size }, () => Array.from({ length: size }, () => ''));
}

function isInside(size: number, row: number, col: number): boolean {
  return row >= 0 && col >= 0 && row < size && col < size;
}

function buildPositions(
  size: number,
  row: number,
  col: number,
  direction: WordSearchDirection,
  length: number,
): Array<{ row: number; col: number }> | null {
  const delta = directionToDelta[direction];
  const positions = Array.from({ length }, (_, index) => ({
    row: row + (delta.row * index),
    col: col + (delta.col * index),
  }));

  if (!positions.every((cell) => isInside(size, cell.row, cell.col))) {
    return null;
  }

  return positions;
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
  const center = (grid.length - 1) / 2;

  positions.forEach((cell, index) => {
    const existing = grid[cell.row][cell.col];
    if (existing === word[index]) {
      overlapCount += 1;
    }
    centerBias += Math.abs(cell.row - center) + Math.abs(cell.col - center);
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


function buildGrid(
  words: string[],
  size: number,
  difficulty: PuzzleDifficulty,
): GridBuildResult {
  const config = WORD_SEARCH_DIFFICULTY_CONFIG[difficulty];
  const grid = createEmptyGrid(size);
  const placements: Placement[] = [];

  words.forEach((word, wordIndex) => {
    let bestPlacement: Placement | null = null;
    let bestScore = Number.NEGATIVE_INFINITY;

    for (let row = 0; row < size; row += 1) {
      for (let col = 0; col < size; col += 1) {
        for (const direction of config.allowedDirections) {
          const positions = buildPositions(size, row, col, direction, word.length);
          if (!positions || !canPlace(grid, positions, word)) {
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

    if (!bestPlacement) {
      return;
    }

    placeWord(grid, bestPlacement.positions, bestPlacement.word);
    placements.push(bestPlacement);
  });

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
  size: number,
): string[] {
  const config = WORD_SEARCH_DIFFICULTY_CONFIG[difficulty].wordLengthProfile;
  const candidates = themeWords
    .map(toUpperWord)
    .filter((word) => word.length >= config.min && word.length <= Math.min(config.max, size));

  return [...new Set(candidates)];
}

function findHiddenWordForLength(themeWords: string[], length: number): string | null {
  if (length < 3) {
    return null;
  }
  const candidates = [...new Set(
    themeWords
      .map(normalizeWordToken)
      .filter((w) => w.length === length),
  )];
  return candidates.length > 0 ? randomFrom(candidates) : null;
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

function calculateLargestEmptyClusterRatio(size: number, placements: readonly Placement[]): number {
  const occupancy = Array.from({ length: size }, () => Array.from({ length: size }, () => false));
  placements.forEach((placement) => {
    placement.positions.forEach((cell) => {
      occupancy[cell.row][cell.col] = true;
    });
  });
  const visited = Array.from({ length: size }, () => Array.from({ length: size }, () => false));
  const directions = [
    { row: 1, col: 0 },
    { row: -1, col: 0 },
    { row: 0, col: 1 },
    { row: 0, col: -1 },
  ];
  let maxCluster = 0;

  for (let row = 0; row < size; row += 1) {
    for (let col = 0; col < size; col += 1) {
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
            || nextRow >= size
            || nextCol >= size
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

  return clamp01(maxCluster / (size * size));
}

function calculateSpreadRatio(size: number, placements: readonly Placement[]): number {
  if (placements.length === 0) {
    return 0;
  }
  const rows = new Set<number>();
  const cols = new Set<number>();
  placements.forEach((placement) => {
    placement.positions.forEach((cell) => {
      rows.add(cell.row);
      cols.add(cell.col);
    });
  });
  return clamp01(((rows.size / size) + (cols.size / size)) / 2);
}

function buildQualityMetrics(
  size: number,
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
  const spreadRatio = calculateSpreadRatio(size, placements);
  const deadZoneRatio = calculateLargestEmptyClusterRatio(size, placements);
  const coverageRatio = clamp01(occupiedCount / (size * size));
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

function buildDiversitySignature(size: number, placements: readonly Placement[]): string {
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
    .map((placement) => `${placement.start.row},${placement.start.col},${placement.direction},${placement.word.length}`)
    .sort()
    .join('|');

  return `${size}:${words}:${directionMix}:${overlapHistogram}:${anchors}`;
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
  size: number,
  difficulty: PuzzleDifficulty,
  options?: { preferredLanguages?: readonly WordSearchLanguage[]; preferredThemeIds?: readonly string[] },
): {
  dedupeKey: string;
  entry: Omit<WordSearchCatalogEntry, 'id'>;
  label: string;
  score: number;
} | null {
  if (!WORD_SEARCH_ALLOWED_SIZES.includes(size as (typeof WORD_SEARCH_ALLOWED_SIZES)[number])) {
    throw new Error(`Unsupported Word Search size: ${size}`);
  }

  const language = pickLanguage(options?.preferredLanguages);
  const theme = pickTheme(language, options?.preferredThemeIds);
  const allWords = pickWords(theme.words, difficulty, size);
  if (allWords.length === 0) {
    return null;
  }

  const difficultyConfig = WORD_SEARCH_DIFFICULTY_CONFIG[difficulty];
  const config = difficultyConfig.wordLengthProfile;
  const preferredLength = Math.max(config.min, Math.min(config.preferred, config.max, size));
  const maxPlacementWords = Math.min(
    allWords.length,
    difficultyConfig.wordCount.max,
    Math.ceil((size * size) / Math.max(1, config.min)),
  );

  let selectedPlacements: Placement[] | null = null;
  let selectedHiddenWord: string | null = null;
  let selectedHiddenPositions: Array<{ row: number; col: number }> | null = null;
  let selectedQuality: WordSearchQualityMetrics | null = null;
  let selectedSignature: string | null = null;

  for (let attempt = 0; attempt < 300; attempt += 1) {
    const targetWordCount = Math.max(
      difficultyConfig.wordCount.min,
      Math.min(
        maxPlacementWords,
        difficultyConfig.wordCount.min + (attempt % (difficultyConfig.wordCount.max - difficultyConfig.wordCount.min + 1)),
      ),
    );
    const shuffled = [...allWords].sort(() => Math.random() - 0.5);
    const candidateWords = shuffled.slice(0, targetWordCount);
    const { grid, words: placements } = buildGrid(candidateWords, size, difficulty);
    if (placements.length === 0) {
      continue;
    }
    const quality = buildQualityMetrics(size, placements);
    if (!passesQualityThreshold(difficulty, quality)) {
      continue;
    }

    // All empty cells become the hidden word — find a theme word of exactly that length.
    const sortedEmpty = collectEmptyCells(grid).sort((a, b) => toGridKey(a) - toGridKey(b));
    const hiddenWord = findHiddenWordForLength(theme.words, sortedEmpty.length);
    if (!hiddenWord) {
      continue;
    }

    selectedPlacements = placements;
    selectedHiddenWord = hiddenWord;
    selectedHiddenPositions = sortedEmpty;
    selectedQuality = quality;
    selectedSignature = buildDiversitySignature(size, placements);
    break;
  }

  if (!selectedPlacements || !selectedHiddenWord || !selectedHiddenPositions || !selectedQuality || !selectedSignature) {
    return null;
  }

  const entry: Omit<WordSearchCatalogEntry, 'id'> = {
    schemaVersion: 1,
    difficulty,
    rows: size,
    cols: size,
    language,
    themeId: theme.themeId,
    words: selectedPlacements.map((placement) => ({
      id: placement.id,
      word: placement.word,
      start: { ...placement.start },
      direction: placement.direction,
    })),
    hiddenWord: {
      word: selectedHiddenWord,
      clue: theme.themeId,
      positions: selectedHiddenPositions,
    },
    diversitySignature: selectedSignature,
    quality: selectedQuality,
  };

  const dedupeKey = `${language}:${theme.themeId}:${difficulty}:${size}:${selectedSignature}`;

  return {
    dedupeKey,
    entry,
    label: `${size}x${size} ${difficulty} (${language})`,
    score: buildDifficultyRatedScore(difficulty, selectedQuality.score),
  };
}
