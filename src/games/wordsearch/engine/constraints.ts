import type { PuzzleDifficulty } from '../../shared/types';
import type { WordSearchDirection } from '../types';

export interface WordSearchGridSize {
  rows: number;
  cols: number;
}

export interface WordSearchDifficultyConfig {
  sizeOptions: readonly WordSearchGridSize[];
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
  maxBends: number;
  // Empirical ratio of unique cells covered to total letter placements.
  // Lower for high-overlap difficulties; used to estimate how many words
  // are needed to fill a grid to a specific target coverage.
  fillEfficiency: number;
}

export const WORD_SEARCH_DIFFICULTY_CONFIG: Record<PuzzleDifficulty, WordSearchDifficultyConfig> = {
  easy: {
    maxBends: 0,
    fillEfficiency: 0.96,
    sizeOptions: [
      { rows: 8, cols: 8 },
      { rows: 8, cols: 10 },
      { rows: 10, cols: 8 },
      { rows: 10, cols: 10 },
    ],
    wordCount: { min: 5, max: 24 },
    allowedDirections: ['right', 'down'],
    wordLengthProfile: { min: 3, max: 8, preferred: 5 },
    letterFrequencyProfile: { vowelsWeight: 0.45, commonConsonantsWeight: 0.45, rareConsonantsWeight: 0.10 },
    overlapFrequency: 0.10,
    clustering: 0.20,
  },
  medium: {
    maxBends: 0,
    fillEfficiency: 0.98,
    sizeOptions: [
      { rows: 10, cols: 10 },
      { rows: 10, cols: 12 },
      { rows: 12, cols: 10 },
      { rows: 12, cols: 12 },
    ],
    wordCount: { min: 12, max: 30 },
    allowedDirections: ['right', 'left', 'down', 'up', 'down-right', 'up-right'],
    wordLengthProfile: { min: 4, max: 9, preferred: 6 },
    letterFrequencyProfile: { vowelsWeight: 0.40, commonConsonantsWeight: 0.48, rareConsonantsWeight: 0.12 },
    overlapFrequency: 0.20,
    clustering: 0.30,
  },
  hard: {
    maxBends: 1,
    fillEfficiency: 0.99,
    sizeOptions: [
      { rows: 12, cols: 12 },
      { rows: 12, cols: 14 },
      { rows: 14, cols: 12 },
      { rows: 14, cols: 14 },
    ],
    wordCount: { min: 18, max: 40 },
    allowedDirections: ['right', 'left', 'down', 'up', 'down-right', 'down-left', 'up-right', 'up-left'],
    wordLengthProfile: { min: 4, max: 10, preferred: 7 },
    letterFrequencyProfile: { vowelsWeight: 0.38, commonConsonantsWeight: 0.48, rareConsonantsWeight: 0.14 },
    overlapFrequency: 0.35,
    clustering: 0.45,
  },
  expert: {
    maxBends: 1,
    fillEfficiency: 0.78,
    sizeOptions: [
      { rows: 14, cols: 14 },
      { rows: 14, cols: 16 },
      { rows: 16, cols: 14 },
      { rows: 16, cols: 16 },
    ],
    wordCount: { min: 24, max: 48 },
    allowedDirections: ['right', 'left', 'down', 'up', 'down-right', 'down-left', 'up-right', 'up-left'],
    wordLengthProfile: { min: 5, max: 13, preferred: 9 },
    letterFrequencyProfile: { vowelsWeight: 0.36, commonConsonantsWeight: 0.48, rareConsonantsWeight: 0.16 },
    overlapFrequency: 0.50,
    clustering: 0.65,
  },
};

export const WORD_SEARCH_MIN_THEMES_PER_LANGUAGE = 15;
export const WORD_SEARCH_MIN_WORDS_PER_THEME = 50;

export interface WordSearchQualityThreshold {
  minScore: number;
  minOverlapRatio: number;
  minDirectionEntropy: number;
  maxDeadZoneRatio: number;
}

export const WORD_SEARCH_QUALITY_THRESHOLDS: Record<PuzzleDifficulty, WordSearchQualityThreshold> = {
  easy: {
    minScore: 0.40,
    minOverlapRatio: 0.08,
    minDirectionEntropy: 0.20,
    maxDeadZoneRatio: 0.56,
  },
  medium: {
    minScore: 0.48,
    minOverlapRatio: 0.00,
    minDirectionEntropy: 0.35,
    maxDeadZoneRatio: 0.50,
  },
  hard: {
    minScore: 0.52,
    minOverlapRatio: 0.00,
    minDirectionEntropy: 0.42,
    maxDeadZoneRatio: 0.48,
  },
  expert: {
    minScore: 0.56,
    minOverlapRatio: 0.00,
    minDirectionEntropy: 0.48,
    maxDeadZoneRatio: 0.46,
  },
};
