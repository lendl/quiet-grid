import type { GameId, PuzzleDifficulty } from '../../games/shared/types';

export type { PuzzleDifficulty };
export type PuzzleTypeId = GameId;

export interface SessionResult {
  gameId: GameId;
  difficulty: PuzzleDifficulty;
  status: 'solved' | 'failed';
  score: number;
  accuracy: number;
  elapsedSeconds: number;
  streak: number;
}

export interface PersistedSessionEnvelope<TPayload = unknown> {
  gameId: GameId;
  version: number;
  payload: TPayload;
}
