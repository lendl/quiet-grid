// src/app/utils/statsUtils.ts
import type { AppStats, Difficulty, DiffStats } from '../types';

const DEFAULT_DIFF_STATS: DiffStats = { played: 0, solved: 0, bestScore: null };

function emptyGameStats(): Record<Difficulty, DiffStats> {
  return {
    easy:   { ...DEFAULT_DIFF_STATS },
    medium: { ...DEFAULT_DIFF_STATS },
    hard:   { ...DEFAULT_DIFF_STATS },
    expert: { ...DEFAULT_DIFF_STATS },
  };
}

function isDiffStats(value: unknown): value is DiffStats {
  if (!value || typeof value !== 'object') return false;
  const d = value as Record<string, unknown>;
  return typeof d.played === 'number'
    && typeof d.solved === 'number'
    && (d.bestScore === null || typeof d.bestScore === 'number');
}

function mergeDiffStats(value: unknown): DiffStats {
  return { ...DEFAULT_DIFF_STATS, ...(isDiffStats(value) ? value : {}) };
}

function mergeGameStats(value: unknown): Record<Difficulty, DiffStats> {
  if (!value || typeof value !== 'object') return emptyGameStats();
  const p = value as Record<string, unknown>;
  return {
    easy:   mergeDiffStats(p.easy),
    medium: mergeDiffStats(p.medium),
    hard:   mergeDiffStats(p.hard),
    expert: mergeDiffStats(p.expert),
  };
}

function safeStreak(value: unknown): number {
  return typeof value === 'number' && value >= 0 ? Math.floor(value) : 0;
}

function normalizePuzzleTypeId(key: string): string {
  return key === 'binary' ? 'takuzu' : key;
}

export const DEFAULT_STATS: AppStats = {
  puzzles: {},
  streaks: {},
};

/**
 * Merges a raw parsed AsyncStorage value into a valid AppStats object.
 * Handles three formats:
 *   1. null/undefined/invalid → DEFAULT_STATS
 *   2. Legacy flat  { easy, medium, hard, expert, streak } → migrated to takuzu.*
 *   3. New format   { takuzu, minesweeper, streaks }
 */
export function mergeStats(raw: unknown): AppStats {
  if (!raw || typeof raw !== 'object') {
    return {
      puzzles: {},
      streaks: {},
    };
  }

  const p = raw as Record<string, unknown>;

  // Legacy format: has top-level 'easy'/'medium'/'hard'/'expert' or 'streak'
  // but no 'takuzu' key.
  if (!('takuzu' in p) && ('easy' in p || 'streak' in p)) {
    return {
      puzzles: {
        takuzu: {
          easy:   mergeDiffStats(p.easy),
          medium: mergeDiffStats(p.medium),
          hard:   mergeDiffStats(p.hard),
          expert: mergeDiffStats(p.expert),
        },
      },
      streaks: {
        takuzu: safeStreak(p.streak),
      },
    };
  }

  if ('puzzles' in p) {
    const rawPuzzles = p.puzzles && typeof p.puzzles === 'object'
      ? p.puzzles as Record<string, unknown>
      : {};
    const rawStreaks = p.streaks && typeof p.streaks === 'object'
      ? p.streaks as Record<string, unknown>
      : {};

    const puzzles = Object.fromEntries(
      Object.entries(rawPuzzles).map(([puzzleTypeId, value]) => [
        normalizePuzzleTypeId(puzzleTypeId),
        mergeGameStats(value),
      ]),
    );
    const streaks = Object.fromEntries(
      Object.entries(rawStreaks).map(([puzzleTypeId, value]) => [
        normalizePuzzleTypeId(puzzleTypeId),
        safeStreak(value),
      ]),
    );

    return { puzzles, streaks };
  }

  // Previous multi-puzzle format
  const rawStreaks = p.streaks && typeof p.streaks === 'object'
    ? p.streaks as Record<string, unknown>
    : {};

  return {
    puzzles: {
      takuzu: mergeGameStats(p.takuzu ?? p.binary),
      minesweeper: mergeGameStats(p.minesweeper),
    },
    streaks: {
      takuzu: safeStreak(rawStreaks.takuzu ?? rawStreaks.binary),
      minesweeper: safeStreak(rawStreaks.minesweeper),
    },
  };
}

/** Returns the difficulty of any active puzzle regardless of puzzle type. */
export function getActivePuzzleDifficultyFromState(
  activePuzzle: { puzzle?: { difficulty: Difficulty } },
): Difficulty {
  if (activePuzzle.puzzle) return activePuzzle.puzzle.difficulty;
  return 'easy'; // fallback; should never reach here with valid data
}

export function getPuzzleStats(stats: AppStats, puzzleTypeId: string): Record<Difficulty, DiffStats> {
  return stats.puzzles[puzzleTypeId] ?? emptyGameStats();
}

export function getPuzzleStreak(stats: AppStats, puzzleTypeId: string): number {
  return stats.streaks[puzzleTypeId] ?? 0;
}

export function ensurePuzzleStats(stats: AppStats, puzzleTypeId: string): Record<Difficulty, DiffStats> {
  if (!stats.puzzles[puzzleTypeId]) {
    stats.puzzles[puzzleTypeId] = emptyGameStats();
  }

  if (stats.streaks[puzzleTypeId] === undefined) {
    stats.streaks[puzzleTypeId] = 0;
  }

  return stats.puzzles[puzzleTypeId];
}
