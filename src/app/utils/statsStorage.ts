import AsyncStorage from '@react-native-async-storage/async-storage';
import type { AppStats, Difficulty, PuzzleTypeId } from '../types';
import { clearActivePuzzleState } from './activePuzzleStateStorage';
import { DEFAULT_STATS, ensurePuzzleStats, mergeStats } from './statsUtils';
import { STATS_KEY } from './storageKeys';

export interface SaveGameResultInput {
  puzzleTypeId: PuzzleTypeId;
  difficulty: Difficulty;
  solved: boolean;
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
  puzzleTypeId,
  difficulty,
  solved,
  score = 0,
}: SaveGameResultInput): Promise<SaveGameResultOutcome> {
  const stats = await loadStats();
  const gameStats = ensurePuzzleStats(stats, puzzleTypeId);
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
    stats.streaks[puzzleTypeId] = (stats.streaks[puzzleTypeId] ?? 0) + 1;
  } else {
    stats.streaks[puzzleTypeId] = 0;
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
  await Promise.all([clearStats(), clearActivePuzzleState()]);
}

export { DEFAULT_STATS };
