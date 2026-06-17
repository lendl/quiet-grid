import AsyncStorage from '@react-native-async-storage/async-storage';
import type { AppStats, Difficulty, GameId } from '../types';
import { clearActiveSessionState } from './activeSessionStateStorage';
import { DEFAULT_STATS, ensureDifficultyStreaks, ensurePuzzleStats, mergeStats } from './statsUtils';
import { STATS_KEY } from './storageKeys';

export interface SaveGameResultInput {
  gameId: GameId;
  difficulty: Difficulty;
  status: 'solved' | 'failed';
  score?: number;
  elapsedSeconds?: number;
}

export interface SaveGameResultOutcome {
  stats: AppStats;
  isFirstSolvedScore: boolean;
  isNewBestScore: boolean;
  isNewBestTime: boolean;
  difficultyStreak: number;
  previousBestTime: number | null;
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
  elapsedSeconds,
}: SaveGameResultInput): Promise<SaveGameResultOutcome> {
  const solved = status === 'solved';
  const stats = await loadStats();
  const gameStats = ensurePuzzleStats(stats, gameId);
  const difficultyStreaks = ensureDifficultyStreaks(stats, gameId);

  const previousSolvedCount = gameStats[difficulty].solved;
  const previousBestScore = gameStats[difficulty].bestScore;
  const previousBestTime = gameStats[difficulty].bestTime;

  const isFirstSolvedScore = solved && previousSolvedCount === 0;
  const isNewBestScore = solved && previousBestScore !== null && score > previousBestScore;
  const isNewBestTime = solved
    && elapsedSeconds !== undefined
    && (previousBestTime === null || elapsedSeconds < previousBestTime);

  gameStats[difficulty].played++;

  if (solved) {
    gameStats[difficulty].solved++;
    if (gameStats[difficulty].bestScore === null || score > gameStats[difficulty].bestScore) {
      gameStats[difficulty].bestScore = score;
    }
    if (elapsedSeconds !== undefined) {
      if (gameStats[difficulty].bestTime === null || elapsedSeconds < gameStats[difficulty].bestTime) {
        gameStats[difficulty].bestTime = elapsedSeconds;
      }
    }
    stats.streaks[gameId] = (stats.streaks[gameId] ?? 0) + 1;
    difficultyStreaks[difficulty] = (difficultyStreaks[difficulty] ?? 0) + 1;
  } else {
    stats.streaks[gameId] = 0;
    difficultyStreaks[difficulty] = 0;
  }

  try {
    await AsyncStorage.setItem(STATS_KEY, JSON.stringify(stats));
  } catch {
    // Keep app stable if stats save fails.
  }

  return {
    stats,
    isFirstSolvedScore,
    isNewBestScore,
    isNewBestTime,
    difficultyStreak: difficultyStreaks[difficulty],
    previousBestTime,
  };
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
