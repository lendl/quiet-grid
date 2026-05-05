import type { Difficulty } from '../types';

export const ACCURACY_DROP_STEP = 10;
export const ACCURACY_SCORE_COST = 500;
export const MAX_SCORE = 10000;

export const TIME_TO_ZERO_SECONDS: Record<Difficulty, number> = {
  easy: 300,
  medium: 450,
  hard: 600,
  expert: 900,
};

function computeRawScore(difficulty: Difficulty, timeSeconds: number, accuracyDrops: number): number {
  const timePenalty = (Math.max(0, timeSeconds) * MAX_SCORE) / TIME_TO_ZERO_SECONDS[difficulty];
  const accuracyPenalty = accuracyDrops * ACCURACY_SCORE_COST;
  return MAX_SCORE - timePenalty - accuracyPenalty;
}

export function computeFinalScore(difficulty: Difficulty, timeSeconds: number, accuracyDrops: number): number {
  return Math.max(0, Math.round(computeRawScore(difficulty, timeSeconds, accuracyDrops)));
}

export function computeAccuracyPct(accuracyDrops: number): number {
  return Math.max(0, 100 - accuracyDrops * ACCURACY_DROP_STEP);
}
