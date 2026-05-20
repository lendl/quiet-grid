import AsyncStorage from '@react-native-async-storage/async-storage';
import type { AppStats, Difficulty, GameId } from '../types';
import { clearActiveSessionState } from './activeSessionStateStorage';
import { DEFAULT_STATS, ensurePuzzleStats, mergeStats } from './statsUtils';
import { STATS_KEY } from './storageKeys';

export interface SaveGameResultInput {
  gameId: GameId;
  difficulty: Difficulty;
  status: 'solved' | 'failed';
  score?: number;
}

export interface SaveGameResultOutcome {
  stats: AppStats;
  isFirstSolvedScore: boolean;
  isNewBestScore: boolean;
}

export async function loadStats(): Promise<AppStats> {
  try {
    const raw = await AsyncStorage.getItem(STATS_KEY);
    if (raw) return mergeStats(JSON.parse(raw));
  } catch {
    // Keep app stable if stats load fails.
  }
  return mergeStats(null);
}

export async function saveGameResult({
  gameId,
  difficulty,
  status,
  score = 0,
}: SaveGameResultInput): Promise<SaveGameResultOutcome> {
  const solved = status === 'solved';
  const stats = await loadStats();
  const gameStats = ensurePuzzleStats(stats, gameId);
  const previousSolvedCount = gameStats[difficulty].solved;
  const previousBestScore = gameStats[difficulty].bestScore;
  const isFirstSolvedScore = solved && previousSolvedCount === 0;
  const isNewBestScore = solved && previousBestScore !== null && score > previousBestScore;

  gameStats[difficulty].played++;

  if (solved) {
    gameStats[difficulty].solved++;
    if (gameStats[difficulty].bestScore === null || score > gameStats[difficulty].bestScore) {
      gameStats[difficulty].bestScore = score;
    }
    stats.streaks[gameId] = (stats.streaks[gameId] ?? 0) + 1;
  } else {
    stats.streaks[gameId] = 0;
  }

  try {
    await AsyncStorage.setItem(STATS_KEY, JSON.stringify(stats));
  } catch {
    // Keep app stable if stats save fails.
  }

  return { stats, isFirstSolvedScore, isNewBestScore };
}

export async function clearStats(): Promise<void> {
  try {
    await AsyncStorage.removeItem(STATS_KEY);
  } catch {
    // Keep app stable if cleanup fails.
  }
}

export async function clearPlayerData(): Promise<void> {
  await Promise.all([clearStats(), clearActiveSessionState()]);
}

export { DEFAULT_STATS };
