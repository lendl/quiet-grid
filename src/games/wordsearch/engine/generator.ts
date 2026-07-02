import type { PuzzleDifficulty } from '../../shared/types';
import type { WordSearchCatalogEntry } from '../platform/codecs/codec';
import type { WordSearchLanguage } from '../types';
import { WORD_SEARCH_DIFFICULTY_CONFIG } from './constraints';
import { buildFullCoverageGrid } from './placement';
import { hasCoverageViolation, hasDuplicateOccurrence } from './generationChecks';
import { buildHiddenWordPool, pickHiddenWord, reserveHiddenWordCells } from './hiddenWord';
import { buildQualityMetrics, passesQualityThreshold, buildDifficultyRatedScore } from './quality';
import { toGridKey } from './gridUtils';
import { wordSearchSeedCorpus } from './seedCorpus';

export interface WordSearchGenerationStats {
  hiddenWordUnavailable: number;
  emptyWordPool: number;
  placementFailed: number;
  wordCountOutOfBand: number;
  coverageViolation: number;
  qualityBelowThreshold: number;
  incompleteCoverage: number;
  duplicateOccurrence: number;
  accepted: number;
}

function createEmptyStats(): WordSearchGenerationStats {
  return {
    hiddenWordUnavailable: 0,
    emptyWordPool: 0,
    placementFailed: 0,
    wordCountOutOfBand: 0,
    coverageViolation: 0,
    qualityBelowThreshold: 0,
    incompleteCoverage: 0,
    duplicateOccurrence: 0,
    accepted: 0,
  };
}

let stats: WordSearchGenerationStats = createEmptyStats();

// Aggregate rejection-gate counters for the current process — never
// per-attempt logging. Callers (the CLI) print this once after a batch run
// to see WHERE yield is being lost, per the generate-advisor's debugging
// guidance. Not test/debug-only scaffolding to be deleted later — this is
// the engine's standing diagnostic surface for tuning thresholds.
export function getWordSearchGenerationStats(): WordSearchGenerationStats {
  return { ...stats };
}

export function resetWordSearchGenerationStats(): void {
  stats = createEmptyStats();
}

function randomFrom<T>(items: readonly T[]): T {
  return items[Math.floor(Math.random() * items.length)]!;
}

function normalizeWordToken(value: string): string {
  return value.normalize('NFKD').replace(/[^A-Za-z]/g, '').toUpperCase();
}

function pickLanguage(preferredLanguages?: readonly WordSearchLanguage[]): WordSearchLanguage {
  if (preferredLanguages && preferredLanguages.length > 0) {
    const supported = preferredLanguages.filter((language) => language in wordSearchSeedCorpus);
    if (supported.length > 0) return randomFrom(supported);
  }
  const languages = Object.keys(wordSearchSeedCorpus) as WordSearchLanguage[];
  return randomFrom(languages);
}

function pickTheme(language: WordSearchLanguage, preferredThemeIds?: readonly string[]) {
  const themes = wordSearchSeedCorpus[language] ?? wordSearchSeedCorpus.en;
  if (preferredThemeIds && preferredThemeIds.length > 0) {
    const preferred = themes.filter((theme) => preferredThemeIds.includes(theme.themeId));
    if (preferred.length > 0) return randomFrom(preferred);
  }
  return randomFrom(themes);
}

function buildDiversitySignature(
  placements: readonly { word: string; start: { row: number; col: number }; direction: string }[],
  hiddenPositions: readonly { row: number; col: number }[],
): string {
  const wordAnchors = placements
    .map((p) => `${p.word}:${p.start.row},${p.start.col},${p.direction}`)
    .sort()
    .join('|');
  const hiddenAnchor = hiddenPositions.map((c) => `${c.row},${c.col}`).sort().join(',');
  return `${wordAnchors}::${hiddenAnchor}`;
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
  const config = WORD_SEARCH_DIFFICULTY_CONFIG[difficulty];
  const language = pickLanguage(options?.preferredLanguages);
  const theme = pickTheme(language, options?.preferredThemeIds);

  const maxFit = Math.max(rows, cols);
  const lengthProfile = config.wordLengthProfile;
  const normalizedThemeWords = [...new Set(theme.words.map(normalizeWordToken))];

  const hiddenWordPool = buildHiddenWordPool(normalizedThemeWords);
  const hiddenWord = pickHiddenWord(hiddenWordPool, rows, cols);
  if (!hiddenWord) {
    stats.hiddenWordUnavailable += 1;
    return null;
  }
  const reservedHiddenWord = reserveHiddenWordCells(hiddenWord, rows, cols);
  const reservedCells = new Set(reservedHiddenWord.positions.map((cell) => toGridKey(cell)));

  const wordPool = normalizedThemeWords.filter((word) => (
    word !== hiddenWord
    && word.length >= lengthProfile.min
    && word.length <= Math.min(lengthProfile.max, maxFit)
  ));
  if (wordPool.length === 0) {
    stats.emptyWordPool += 1;
    return null;
  }

  const result = buildFullCoverageGrid(rows, cols, wordPool, reservedCells, config);
  if (!result) {
    stats.placementFailed += 1;
    return null;
  }
  const { grid, placements } = result;

  // Gates below run cheapest-first: word count band (O(1)) → non-domination
  // (O(placements^2)) → quality metrics (O(placements)) → hidden-word overlay
  // and gap check (O(rows*cols)) → duplicate-occurrence ghost scan
  // (O(8 * rows * cols * word-lengths), the most expensive check, last.

  if (placements.length < config.wordCount.min || placements.length > config.wordCount.max) {
    stats.wordCountOutOfBand += 1;
    return null;
  }

  if (hasCoverageViolation(placements)) {
    stats.coverageViolation += 1;
    return null;
  }

  const quality = buildQualityMetrics(placements);
  if (!passesQualityThreshold(difficulty, quality)) {
    stats.qualityBelowThreshold += 1;
    return null;
  }

  // Overlay the hidden word's letters onto its reserved cells to produce the
  // final grid, then confirm the whole grid is covered with no gaps left.
  reservedHiddenWord.positions.forEach((cell, index) => {
    grid[cell.row][cell.col] = hiddenWord[index]!;
  });
  const hasGap = grid.some((row) => row.some((cell) => cell === '' || cell === '#'));
  if (hasGap) {
    stats.incompleteCoverage += 1;
    return null;
  }

  if (hasDuplicateOccurrence(grid, [...placements, { word: hiddenWord, positions: reservedHiddenWord.positions }])) {
    stats.duplicateOccurrence += 1;
    return null;
  }

  stats.accepted += 1;
  const diversitySignature = buildDiversitySignature(placements, reservedHiddenWord.positions);

  const entry: Omit<WordSearchCatalogEntry, 'id'> = {
    schemaVersion: 1,
    difficulty,
    rows,
    cols,
    language,
    themeId: theme.themeId,
    words: placements.map((placement) => ({
      id: placement.id,
      word: placement.word,
      start: { ...placement.start },
      direction: placement.direction,
    })),
    hiddenWord: {
      word: hiddenWord,
      clue: theme.themeId,
      positions: reservedHiddenWord.positions,
    },
    diversitySignature,
    quality,
  };

  const dedupeKey = `${language}:${theme.themeId}:${difficulty}:${rows}x${cols}:${diversitySignature}`;

  return {
    dedupeKey,
    entry,
    label: `${rows}x${cols} ${difficulty} (${language})`,
    score: buildDifficultyRatedScore(difficulty, quality.score),
  };
}
