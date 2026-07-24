import type { PuzzleDifficulty } from '../../shared/types';
import type { WordSearchCatalogEntry } from '../platform/codecs/codec';
import type { WordSearchLanguage } from '../types';
import { WORD_SEARCH_DIFFICULTY_CONFIG } from './constraints';
import { buildFullCoverageGrid, type PlacementResult } from './placement';
import { hasCoverageViolation, hasDuplicateOccurrence } from './generationChecks';
import { buildHiddenWordPool, pickHiddenWord, reserveHiddenWordCells, type ReservedHiddenWord } from './hiddenWord';
import { buildQualityMetrics, passesQualityThreshold, buildDifficultyRatedScore } from './quality';
import { toGridKey } from './gridUtils';
import { wordSearchSeedCorpus } from './seedCorpus';

// The hidden word's reserved cells act as random obstacles the full-coverage
// tiling must route around. A single unlucky scatter (or an unusually long
// hidden word) can make an otherwise-easy grid unsolvable even though the
// same theme/size succeeds most of the time with a different hidden word
// (confirmed by direct measurement: the same pool/grid went from ~91% success
// with zero reserved cells down to ~50% with 14). Retrying just this cheap
// step a few times before giving up recovers most of that bad luck without
// burning a full outer CLI attempt, which would also re-pick language and
// theme for no reason.
const MAX_HIDDEN_WORD_ATTEMPTS = 5;

export interface WordSearchGenerationStats {
  hiddenWordUnavailable: number;
  emptyWordPool: number;
  placementFailed: number;
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
  const normalizedThemeWords = [...new Set(theme.words.map(normalizeWordToken))];

  const hiddenWordPool = buildHiddenWordPool(normalizedThemeWords);

  let hiddenWord: string | undefined;
  let reservedHiddenWord: ReservedHiddenWord | undefined;
  let placementResult: PlacementResult | undefined;

  for (let attempt = 0; attempt < MAX_HIDDEN_WORD_ATTEMPTS; attempt += 1) {
    const candidateHiddenWord = pickHiddenWord(hiddenWordPool, rows, cols);
    if (!candidateHiddenWord) {
      stats.hiddenWordUnavailable += 1;
      return null;
    }
    const candidateReserved = reserveHiddenWordCells(candidateHiddenWord, rows, cols);
    const candidateReservedCells = new Set(candidateReserved.positions.map((cell) => toGridKey(cell)));

    // Every theme word is eligible for every difficulty — the only hard
    // constraint is that a word must physically fit a straight line in the
    // grid. Difficulty comes from direction freedom and overlap density
    // (constraints.ts), not from filtering which words are usable.
    const candidateWordPool = normalizedThemeWords.filter((word) => (
      word !== candidateHiddenWord
      && word.length >= 3
      && word.length <= maxFit
    ));
    if (candidateWordPool.length === 0) {
      stats.emptyWordPool += 1;
      return null;
    }

    const candidateResult = buildFullCoverageGrid(rows, cols, candidateWordPool, candidateReservedCells, config);
    if (candidateResult) {
      hiddenWord = candidateHiddenWord;
      reservedHiddenWord = candidateReserved;
      placementResult = candidateResult;
      break;
    }
  }

  if (!placementResult || !hiddenWord || !reservedHiddenWord) {
    stats.placementFailed += 1;
    return null;
  }
  const { grid, placements } = placementResult;

  // Gates below run cheapest-first: non-domination (O(placements^2)) →
  // quality metrics (O(placements)) → hidden-word overlay and gap check
  // (O(rows*cols)) → duplicate-occurrence ghost scan (O(8 * rows * cols *
  // word-lengths), the most expensive check, last.

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
