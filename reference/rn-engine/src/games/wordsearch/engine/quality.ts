import type { PuzzleDifficulty } from '../../shared/types';
import type { WordSearchDirection } from '../types';
import { toGridKey } from './gridUtils';
import { WORD_SEARCH_QUALITY_THRESHOLDS } from './constraints';

export interface WordSearchPlacementLike {
  word: string;
  direction: WordSearchDirection;
  positions: readonly { row: number; col: number }[];
}

export interface WordSearchQualityMetrics {
  overlapRatio: number;
  directionEntropy: number;
  score: number;
}

function clamp01(value: number): number {
  if (value < 0) return 0;
  if (value > 1) return 1;
  return value;
}

function calculateDirectionEntropy(placements: readonly WordSearchPlacementLike[]): number {
  if (placements.length === 0) return 0;
  const counts = new Map<WordSearchDirection, number>();
  placements.forEach((p) => counts.set(p.direction, (counts.get(p.direction) ?? 0) + 1));
  const total = placements.length;
  let entropy = 0;
  counts.forEach((count) => {
    const probability = count / total;
    entropy += -(probability * Math.log2(probability));
  });
  const maxEntropy = Math.log2(Math.max(2, counts.size));
  return maxEntropy <= 0 ? 0 : clamp01(entropy / maxEntropy);
}

export function buildQualityMetrics(placements: readonly WordSearchPlacementLike[]): WordSearchQualityMetrics {
  const totalWordLetters = placements.reduce((sum, p) => sum + p.word.length, 0);
  const occupied = new Set<number>();
  placements.forEach((p) => p.positions.forEach((cell) => occupied.add(toGridKey(cell))));
  const overlapRatio = totalWordLetters === 0
    ? 0
    : clamp01((totalWordLetters - occupied.size) / totalWordLetters);
  const directionEntropy = calculateDirectionEntropy(placements);
  const score = clamp01((overlapRatio * 0.5) + (directionEntropy * 0.5));
  return { overlapRatio, directionEntropy, score };
}

export function passesQualityThreshold(
  difficulty: PuzzleDifficulty,
  quality: WordSearchQualityMetrics,
): boolean {
  const threshold = WORD_SEARCH_QUALITY_THRESHOLDS[difficulty];
  return quality.score >= threshold.minScore
    && quality.overlapRatio >= threshold.minOverlapRatio
    && quality.directionEntropy >= threshold.minDirectionEntropy;
}

export function buildDifficultyRatedScore(difficulty: PuzzleDifficulty, qualityScore: number): number {
  const threshold = WORD_SEARCH_QUALITY_THRESHOLDS[difficulty];
  const normalized = clamp01((qualityScore - threshold.minScore) / Math.max(0.001, 1 - threshold.minScore));
  const tierBase: Record<PuzzleDifficulty, number> = { easy: 0, medium: 25, hard: 50, expert: 75 };
  return Number((tierBase[difficulty] + (normalized * 24.9)).toFixed(1));
}
