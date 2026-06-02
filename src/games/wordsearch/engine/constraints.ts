import type { PuzzleDifficulty } from '../../shared/types';
import type { WordSearchDirection } from '../types';

export interface WordSearchDifficultyConfig {
  sizeOptions: readonly number[];
  wordCount: { min: number; max: number };
  allowedDirections: readonly WordSearchDirection[];
  wordLengthProfile: {
    min: number;
    max: number;
    preferred: number;
  };
  letterFrequencyProfile: {
    vowelsWeight: number;
    commonConsonantsWeight: number;
    rareConsonantsWeight: number;
  };
  overlapFrequency: number;
  clustering: number;
}

export const WORD_SEARCH_ALLOWED_SIZES = [6, 8, 10, 12, 16, 20] as const;

export const WORD_SEARCH_DIFFICULTY_CONFIG: Record<PuzzleDifficulty, WordSearchDifficultyConfig> = {
  easy: {
    sizeOptions: [6, 8],
    wordCount: { min: 5, max: 9 },
    allowedDirections: ['right', 'down'],
    wordLengthProfile: { min: 3, max: 6, preferred: 4 },
    letterFrequencyProfile: { vowelsWeight: 0.45, commonConsonantsWeight: 0.45, rareConsonantsWeight: 0.10 },
    overlapFrequency: 0.10,
    clustering: 0.20,
  },
  medium: {
    sizeOptions: [8, 10],
    wordCount: { min: 7, max: 12 },
    allowedDirections: ['right', 'down', 'down-right', 'up-right'],
    wordLengthProfile: { min: 4, max: 7, preferred: 5 },
    letterFrequencyProfile: { vowelsWeight: 0.40, commonConsonantsWeight: 0.48, rareConsonantsWeight: 0.12 },
    overlapFrequency: 0.20,
    clustering: 0.30,
  },
  hard: {
    sizeOptions: [10, 12],
    wordCount: { min: 10, max: 16 },
    allowedDirections: ['right', 'left', 'down', 'up', 'down-right', 'down-left', 'up-right', 'up-left'],
    wordLengthProfile: { min: 5, max: 9, preferred: 6 },
    letterFrequencyProfile: { vowelsWeight: 0.38, commonConsonantsWeight: 0.48, rareConsonantsWeight: 0.14 },
    overlapFrequency: 0.30,
    clustering: 0.45,
  },
  expert: {
    sizeOptions: [12, 16],
    wordCount: { min: 14, max: 22 },
    allowedDirections: ['right', 'left', 'down', 'up', 'down-right', 'down-left', 'up-right', 'up-left'],
    wordLengthProfile: { min: 6, max: 12, preferred: 8 },
    letterFrequencyProfile: { vowelsWeight: 0.36, commonConsonantsWeight: 0.48, rareConsonantsWeight: 0.16 },
    overlapFrequency: 0.45,
    clustering: 0.60,
  },
};

export const WORD_SEARCH_MIN_THEMES_PER_LANGUAGE = 50;
export const WORD_SEARCH_MIN_WORDS_PER_THEME = 50;

export interface WordSearchQualityThreshold {
  minScore: number;
  minOverlapRatio: number;
  minDirectionEntropy: number;
  maxDeadZoneRatio: number;
}

export const WORD_SEARCH_QUALITY_THRESHOLDS: Record<PuzzleDifficulty, WordSearchQualityThreshold> = {
  easy: {
    minScore: 0.44,
    minOverlapRatio: 0.08,
    minDirectionEntropy: 0.25,
    maxDeadZoneRatio: 0.52,
  },
  medium: {
    minScore: 0.48,
    minOverlapRatio: 0.10,
    minDirectionEntropy: 0.35,
    maxDeadZoneRatio: 0.50,
  },
  hard: {
    minScore: 0.52,
    minOverlapRatio: 0.12,
    minDirectionEntropy: 0.42,
    maxDeadZoneRatio: 0.48,
  },
  expert: {
    minScore: 0.56,
    minOverlapRatio: 0.14,
    minDirectionEntropy: 0.48,
    maxDeadZoneRatio: 0.46,
  },
};
