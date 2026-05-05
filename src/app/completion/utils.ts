import type { PuzzleOutcome } from '../shell/types';
import type { RootStackParamList } from '../navigation/types';
import type { SaveGameResultOutcome } from '../utils/statsStorage';
import { saveGameResult } from '../utils/statsStorage';
import type { CompletionVariant } from './types';
import type { PuzzleSolvedState } from '../shell/playContract';

export interface SavedSolvedOutcome {
  outcome: PuzzleOutcome;
  variant: CompletionVariant;
}

export function getCompletionVariant(outcome: SaveGameResultOutcome): CompletionVariant {
  if (outcome.isFirstSolvedScore) return 'first-score';
  if (outcome.isNewBestScore) return 'new-high-score';
  return 'finished';
}

export async function buildSolvedCompletionParams(
  buildCompletionParams: (
    outcome: PuzzleOutcome,
    variant: CompletionVariant,
  ) => RootStackParamList['Completion'],
  solvedState: PuzzleSolvedState,
): Promise<RootStackParamList['Completion']> {
  const savedOutcome = await saveSolvedOutcome(solvedState);
  return buildCompletionParams(savedOutcome.outcome, savedOutcome.variant);
}

export async function saveSolvedOutcome(
  solvedState: PuzzleSolvedState,
): Promise<SavedSolvedOutcome> {
  const outcome = await saveGameResult({
    puzzleTypeId: solvedState.puzzleTypeId,
    difficulty: solvedState.difficulty,
    solved: true,
    score: solvedState.score,
  });

  return {
    outcome: {
      ...solvedState,
      streak: outcome.stats.streaks[solvedState.puzzleTypeId] ?? 0,
    },
    variant: getCompletionVariant(outcome),
  };
}
