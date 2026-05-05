import type { ActivePuzzle } from './activePuzzleTypes';
import type { PuzzleDifficulty, PuzzleOutcome } from './types';

export interface RestoredPuzzleSession<TSession> {
  session: TSession;
  elapsedSeconds: number;
}

export type PuzzleSolvedState = Omit<PuzzleOutcome, 'streak'> & {
  solved: true;
};

export interface PuzzlePlayContractBase<TSession = unknown, THud = unknown> {
  createSession(input: { difficulty: PuzzleDifficulty }): TSession | null;
  canResume(activePuzzle: ActivePuzzle | null): boolean;
  restoreSession(activePuzzle: ActivePuzzle): RestoredPuzzleSession<TSession>;
  serializeSession(input: { session: TSession; elapsedSeconds: number }): ActivePuzzle;
  getPersistenceKey(input: { session: TSession; elapsedBucket: number }): string | null;
  getHudState(input: { session: TSession; elapsedSeconds: number }): THud;
  getSolvedState(input: { session: TSession; elapsedSeconds: number }): PuzzleSolvedState | null;
  isInProgress(session: TSession): boolean;
}

export interface PuzzlePlayContract<TSession, TActivePuzzle extends ActivePuzzle, THud>
  extends PuzzlePlayContractBase<TSession, THud> {
  canResume(activePuzzle: ActivePuzzle | null): activePuzzle is TActivePuzzle;
  restoreSession(activePuzzle: TActivePuzzle): RestoredPuzzleSession<TSession>;
  serializeSession(input: { session: TSession; elapsedSeconds: number }): TActivePuzzle;
}
