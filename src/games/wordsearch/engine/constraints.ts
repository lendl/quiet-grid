import type { PuzzleDifficulty } from '../../shared/types';
import type { WordSearchDirection } from '../types';

export interface WordSearchSizeRange {
  rowsMin: number;
  rowsMax: number;
  colsMin: number;
  colsMax: number;
}

export interface WordSearchDifficultyConfig {
  sizeRange: WordSearchSizeRange;
  wordCount: { min: number; max: number };
  allowedDirections: readonly WordSearchDirection[];
  wordLengthProfile: { min: number; max: number; preferred: number };
  // Placement scoring weight for reusing an already-placed letter versus
  // covering a fresh cell. Higher values favor denser interlocking.
  overlapFrequency: number;
}

export const WORD_SEARCH_DIFFICULTY_CONFIG: Record<PuzzleDifficulty, WordSearchDifficultyConfig> = {
  easy: {
    sizeRange: { rowsMin: 8, rowsMax: 10, colsMin: 8, colsMax: 10 },
    wordCount: { min: 5, max: 30 },
    allowedDirections: ['right', 'down'],
    wordLengthProfile: { min: 3, max: 7, preferred: 4 },
    overlapFrequency: 0.15,
  },
  medium: {
    sizeRange: { rowsMin: 10, rowsMax: 12, colsMin: 10, colsMax: 12 },
    wordCount: { min: 8, max: 40 },
    allowedDirections: ['right', 'left', 'down', 'up', 'down-right', 'up-right'],
    wordLengthProfile: { min: 4, max: 8, preferred: 6 },
    overlapFrequency: 0.28,
  },
  hard: {
    sizeRange: { rowsMin: 12, rowsMax: 14, colsMin: 12, colsMax: 14 },
    wordCount: { min: 10, max: 55 },
    allowedDirections: ['right', 'left', 'down', 'up', 'down-right', 'down-left', 'up-right', 'up-left'],
    wordLengthProfile: { min: 5, max: 10, preferred: 7 },
    overlapFrequency: 0.40,
  },
  expert: {
    sizeRange: { rowsMin: 14, rowsMax: 16, colsMin: 14, colsMax: 16 },
    wordCount: { min: 12, max: 70 },
    allowedDirections: ['right', 'left', 'down', 'up', 'down-right', 'down-left', 'up-right', 'up-left'],
    wordLengthProfile: { min: 6, max: 13, preferred: 9 },
    overlapFrequency: 0.55,
  },
};

export const WORD_SEARCH_MIN_THEMES_PER_LANGUAGE = 15;
export const WORD_SEARCH_MIN_WORDS_PER_THEME = 50;

export interface WordSearchQualityThreshold {
  minScore: number;
  minOverlapRatio: number;
  minDirectionEntropy: number;
}

export const WORD_SEARCH_QUALITY_THRESHOLDS: Record<PuzzleDifficulty, WordSearchQualityThreshold> = {
  easy: { minScore: 0.20, minOverlapRatio: 0.02, minDirectionEntropy: 0.0 },
  medium: { minScore: 0.25, minOverlapRatio: 0.04, minDirectionEntropy: 0.2 },
  hard: { minScore: 0.30, minOverlapRatio: 0.06, minDirectionEntropy: 0.3 },
  expert: { minScore: 0.35, minOverlapRatio: 0.08, minDirectionEntropy: 0.35 },
};
