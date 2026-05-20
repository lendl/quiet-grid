import { gameRegistry } from '../../shell/games/gameRegistry';
import type { AppStats, Difficulty, DiffStats, GameId } from '../../types';
import {
  STATS_DIFFICULTIES,
  getMergedPuzzleStats,
  getMergedPuzzleStreak,
  getPuzzleStats,
  getPuzzleStreak,
  getStatsSummary,
} from '../../utils/statsUtils';

export type StatsScope =
  | { kind: 'all' }
  | { kind: 'game'; gameId: GameId };

export interface StatsSummaryItem {
  label: string;
  value: string | number;
}

export interface StatsDifficultyRow {
  difficulty: Difficulty;
  label: string;
  stats: DiffStats;
  winRate: number;
}

export interface StatsOverviewModel {
  summary: {
    totalSolved: number;
    streak: number;
    winRate: number;
  };
  difficultyRows: StatsDifficultyRow[];
}

function getMergedDifficultyLabel(difficulty: Difficulty): string {
  for (const definition of gameRegistry) {
    const label = definition.content.difficultyLabels[difficulty];
    if (label) return label;
  }

  return difficulty.charAt(0).toUpperCase() + difficulty.slice(1);
}

function getScopeGameIds(scope: StatsScope): GameId[] {
  return scope.kind === 'all'
    ? gameRegistry.map((definition) => definition.id)
    : [scope.gameId];
}

export function buildStatsOverview(stats: AppStats, scope: StatsScope): StatsOverviewModel {
  const gameIds = getScopeGameIds(scope);
  const scopedStats = scope.kind === 'all'
    ? getMergedPuzzleStats(stats, gameIds)
    : getPuzzleStats(stats, scope.gameId);
  const streak = scope.kind === 'all'
    ? getMergedPuzzleStreak(stats, gameIds)
    : getPuzzleStreak(stats, scope.gameId);
  const summary = getStatsSummary(scopedStats);
  const scopedDefinition = scope.kind === 'game'
    ? gameRegistry.find((definition) => definition.id === scope.gameId)
    : undefined;

  const difficultyRows = STATS_DIFFICULTIES.map((difficulty) => {
    const diffStats = scopedStats[difficulty];
    const label = scopedDefinition?.content.difficultyLabels[difficulty]
      ?? getMergedDifficultyLabel(difficulty);
    const winRate = diffStats.played > 0
      ? Math.round((diffStats.solved / diffStats.played) * 100)
      : 0;

    return {
      difficulty,
      label,
      stats: diffStats,
      winRate,
    };
  });

  return {
    summary: {
      totalSolved: summary.totalSolved,
      streak,
      winRate: summary.winRate,
    },
    difficultyRows,
  };
}
