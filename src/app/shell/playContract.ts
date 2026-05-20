import type { ActiveSession } from './activeSessionTypes';
import type { PuzzleDifficulty, SessionResult } from './types';

export interface RestoredPuzzleSession<TSession> {
  session: TSession;
  elapsedSeconds: number;
}

export type PuzzleSolvedState = Omit<SessionResult, 'streak'> & {
  status: 'solved';
};

export interface PuzzlePlayContractBase<TSession = unknown, THud = unknown> {
  createSession(input: { difficulty: PuzzleDifficulty }): TSession | null;
  canResume(activeSession: ActiveSession | null): boolean;
  restoreSession(activeSession: ActiveSession): RestoredPuzzleSession<TSession>;
  serializeSession(input: { session: TSession; elapsedSeconds: number }): ActiveSession;
  getPersistenceKey(input: { session: TSession; elapsedBucket: number }): string | null;
  getHudState(input: { session: TSession; elapsedSeconds: number }): THud;
  getSolvedState(input: { session: TSession; elapsedSeconds: number }): PuzzleSolvedState | null;
  isInProgress(session: TSession): boolean;
  hasMeaningfulProgress(session: TSession): boolean;
}

export interface PuzzlePlayContract<TSession, TActiveSession extends ActiveSession, THud>
  extends PuzzlePlayContractBase<TSession, THud> {
  canResume(activeSession: ActiveSession | null): activeSession is TActiveSession;
  restoreSession(activeSession: TActiveSession): RestoredPuzzleSession<TSession>;
  serializeSession(input: { session: TSession; elapsedSeconds: number }): TActiveSession;
}
