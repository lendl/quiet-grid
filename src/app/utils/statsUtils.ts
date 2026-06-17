// src/app/utils/statsUtils.ts
import type { AppStats, Difficulty, DiffStats } from '../types';

const DEFAULT_DIFF_STATS: DiffStats = { played: 0, solved: 0, bestScore: null, bestTime: null };

export const STATS_DIFFICULTIES: Difficulty[] = ['easy', 'medium', 'hard', 'expert'];

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
    && (d.bestScore === null || typeof d.bestScore === 'number')
    && (d.bestTime === undefined || d.bestTime === null || typeof d.bestTime === 'number');
}

function mergeDiffStats(value: unknown): DiffStats {
  const base = isDiffStats(value) ? value : {};
  return { ...DEFAULT_DIFF_STATS, ...base };
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

function mergeDifficultyStreakRecord(value: unknown): Record<Difficulty, number> {
  const base = value && typeof value === 'object' ? value as Record<string, unknown> : {};
  return {
    easy:   safeStreak(base.easy),
    medium: safeStreak(base.medium),
    hard:   safeStreak(base.hard),
    expert: safeStreak(base.expert),
  };
}

export const DEFAULT_STATS: AppStats = {
  puzzles: {},
  streaks: {},
  difficultyStreaks: {},
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
      difficultyStreaks: {},
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
      streaks: { takuzu: safeStreak(p.streak) },
      difficultyStreaks: {},
    };
  }

  if ('puzzles' in p) {
    const rawPuzzles = p.puzzles && typeof p.puzzles === 'object'
      ? p.puzzles as Record<string, unknown>
      : {};
    const rawStreaks = p.streaks && typeof p.streaks === 'object'
      ? p.streaks as Record<string, unknown>
      : {};
    const rawDifficultyStreaks = p.difficultyStreaks && typeof p.difficultyStreaks === 'object'
      ? p.difficultyStreaks as Record<string, unknown>
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
    const difficultyStreaks = Object.fromEntries(
      Object.entries(rawDifficultyStreaks).map(([gameId, value]) => [
        normalizePuzzleTypeId(gameId),
        mergeDifficultyStreakRecord(value),
      ]),
    );

    return { puzzles, streaks, difficultyStreaks };
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
    difficultyStreaks: {},
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

export function getMergedPuzzleStats(
  stats: AppStats,
  puzzleTypeIds: string[],
): Record<Difficulty, DiffStats> {
  const merged = emptyGameStats();

  for (const puzzleTypeId of puzzleTypeIds) {
    const gameStats = getPuzzleStats(stats, puzzleTypeId);

    for (const difficulty of STATS_DIFFICULTIES) {
      merged[difficulty].played += gameStats[difficulty].played;
      merged[difficulty].solved += gameStats[difficulty].solved;
      merged[difficulty].bestScore = merged[difficulty].bestScore === null
        ? gameStats[difficulty].bestScore
        : gameStats[difficulty].bestScore === null
          ? merged[difficulty].bestScore
          : Math.max(merged[difficulty].bestScore, gameStats[difficulty].bestScore);
    }
  }

  return merged;
}

export function getPuzzleStreak(stats: AppStats, puzzleTypeId: string): number {
  return stats.streaks[puzzleTypeId] ?? 0;
}

export function getMergedPuzzleStreak(stats: AppStats, puzzleTypeIds: string[]): number {
  return puzzleTypeIds.reduce((sum, puzzleTypeId) => sum + getPuzzleStreak(stats, puzzleTypeId), 0);
}

export function getStatsSummary(gameStats: Record<Difficulty, DiffStats>): {
  totalPlayed: number;
  totalSolved: number;
  winRate: number;
} {
  const totalPlayed = STATS_DIFFICULTIES.reduce((sum, difficulty) => sum + gameStats[difficulty].played, 0);
  const totalSolved = STATS_DIFFICULTIES.reduce((sum, difficulty) => sum + gameStats[difficulty].solved, 0);
  const winRate = totalPlayed > 0 ? Math.round((totalSolved / totalPlayed) * 100) : 0;

  return {
    totalPlayed,
    totalSolved,
    winRate,
  };
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

export function getDifficultyStreak(stats: AppStats, gameId: string, difficulty: Difficulty): number {
  return stats.difficultyStreaks[gameId]?.[difficulty] ?? 0;
}

export function ensureDifficultyStreaks(
  stats: AppStats,
  gameId: string,
): Record<Difficulty, number> {
  if (!stats.difficultyStreaks[gameId]) {
    stats.difficultyStreaks[gameId] = { easy: 0, medium: 0, hard: 0, expert: 0 };
  }
  return stats.difficultyStreaks[gameId];
}
