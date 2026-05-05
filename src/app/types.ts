// src/app/types.ts
// Shared TypeScript types for the Quiet Grid app

import type { PuzzleDifficulty, PuzzleOutcome, PuzzleSessionEnvelope, PuzzleTypeId } from './shell/types';
export type {
  CompletionRouteParams,
  PuzzlePlayRouteParams,
  RootStackParamList,
  TutorialEntryPoint,
} from './navigation/types';
export type { CompletionVariant } from './completion/types';

/** Available puzzle difficulties */
export type Difficulty = PuzzleDifficulty;

export type { PuzzleOutcome, PuzzleSessionEnvelope, PuzzleTypeId };

export interface PuzzleLayout {
  rows: number;
  cols: number;
}

// ─── Stats ────────────────────────────────────────────────────────────────────

/** Per-difficulty stats */
export interface DiffStats {
  played: number;
  solved: number;
  bestScore: number | null;
}

/** Full stats object stored in AsyncStorage */
export interface AppStats {
  puzzles: Record<string, Record<Difficulty, DiffStats>>;
  streaks: Record<string, number>;
}

