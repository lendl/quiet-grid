import type { SessionResult } from '../shell/types';
import type { RootStackParamList } from '../navigation/types';
import type { SaveGameResultOutcome } from '../utils/statsStorage';
import { saveGameResult } from '../utils/statsStorage';
import type { SolvedResultVariant } from './types';
import type { PuzzleSolvedState } from '../shell/playContract';

export interface SavedSolvedResult {
  result: SessionResult;
  variant: SolvedResultVariant;
}

export function getCompletionVariant(outcome: SaveGameResultOutcome): SolvedResultVariant {
  if (outcome.isFirstSolvedScore) return 'first-score';
  if (outcome.isNewBestScore) return 'new-high-score';
  return 'standard-solve';
}

export async function buildSolvedCompletionParams(
  buildCompletionParams: (
    result: SessionResult,
    variant: SolvedResultVariant,
  ) => RootStackParamList['Completion'],
  solvedState: PuzzleSolvedState,
): Promise<RootStackParamList['Completion']> {
  const savedResult = await saveSolvedResult(solvedState);
  return buildCompletionParams(savedResult.result, savedResult.variant);
}

export async function saveSolvedResult(
  solvedState: PuzzleSolvedState,
): Promise<SavedSolvedResult> {
  const outcome = await saveGameResult({
    gameId: solvedState.gameId,
    difficulty: solvedState.difficulty,
    status: solvedState.status,
    score: solvedState.score,
    elapsedSeconds: solvedState.elapsedSeconds,
  });

  return {
    result: {
      ...solvedState,
      streak: outcome.stats.streaks[solvedState.gameId] ?? 0,
    },
    variant: getCompletionVariant(outcome),
  };
}
