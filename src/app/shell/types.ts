import type { PuzzleDifficulty, PuzzleTypeId } from '../../games/shared/types';

export type { PuzzleDifficulty, PuzzleTypeId };

export interface PuzzleOutcome {
  puzzleTypeId: PuzzleTypeId;
  difficulty: PuzzleDifficulty;
  solved: boolean;
  score: number;
  accuracy: number;
  elapsedSeconds: number;
  streak: number;
}

export interface PuzzleSessionEnvelope<TPayload = unknown> {
  puzzleTypeId: PuzzleTypeId;
  version: number;
  payload: TPayload;
}
