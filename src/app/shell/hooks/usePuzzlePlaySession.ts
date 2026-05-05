import { useCallback, useMemo } from 'react';
import type { AppStateStatus } from 'react-native';
import type { ActivePuzzle } from '../activePuzzleTypes';
import { useGameTimer } from '../../hooks/useGameTimer';
import { loadActivePuzzleState } from '../../utils/activePuzzleStateStorage';
import type { PuzzlePlayContractBase } from '../playContract';
import type { PuzzleDifficulty } from '../types';
import { usePersistedPuzzleSession } from './usePersistedPuzzleSession';
import { usePuzzleOutcome } from './usePuzzleOutcome';

interface UsePuzzlePlaySessionArgs<TSession, THud> {
  isFocused: boolean;
  running: boolean;
  loading: boolean;
  difficulty: PuzzleDifficulty;
  session: TSession | null;
  contract: PuzzlePlayContractBase<TSession, THud>;
  resumeRequested: boolean;
  onMissing: () => void | Promise<void>;
  persistEnabled: boolean;
  save: (value: ActivePuzzle) => Promise<void>;
}

interface PuzzlePlayTimerResult {
  elapsedSeconds: number;
  appState: AppStateStatus;
  isSessionVisible: boolean;
  getCurrentElapsedSeconds: () => number;
  pauseTimer: () => number;
  resumeTimer: () => void;
  resetTimer: () => void;
  setElapsedBeforeSession: (value: number) => void;
}

export type PuzzlePlaySessionResult<TSession, THud> = PuzzlePlayTimerResult & {
  buildCompletionParams: ReturnType<typeof usePuzzleOutcome>;
  createFreshSession: () => TSession | null;
  hudState: THud | null;
  restoreRequestedPuzzle: () => Promise<TSession | null>;
};

export function usePuzzlePlaySession<TSession, THud>({
  isFocused,
  running,
  loading,
  difficulty,
  session,
  contract,
  resumeRequested,
  onMissing,
  persistEnabled,
  save,
}: UsePuzzlePlaySessionArgs<TSession, THud>): PuzzlePlaySessionResult<TSession, THud> {
  const timer = useGameTimer({ enabled: isFocused && running && !loading });
  const buildCompletionParams = usePuzzleOutcome();
  const elapsedSeconds = typeof timer.elapsed === 'number' ? timer.elapsed : 0;
  const { setElapsedBeforeSession } = timer;

  usePersistedPuzzleSession({
    enabled: persistEnabled && session !== null,
    value: session ? contract.serializeSession({ session, elapsedSeconds }) : null,
    changeKey: () => (session ? contract.getPersistenceKey({
      session,
      elapsedBucket: Math.floor(elapsedSeconds / 5),
    }) : null),
    save,
  });

  const createFreshSession = useCallback(() => (
    contract.createSession({ difficulty })
  ), [contract, difficulty]);

  const restoreRequestedPuzzle = useCallback(async () => {
    if (!resumeRequested) {
      return null;
    }

    const activePuzzle = await loadActivePuzzleState();
    if (!activePuzzle || !contract.canResume(activePuzzle)) {
      await onMissing();
      return null;
    }

    const restoredSession = contract.restoreSession(activePuzzle);
    setElapsedBeforeSession(restoredSession.elapsedSeconds);
    return restoredSession.session;
  }, [contract, onMissing, resumeRequested, setElapsedBeforeSession]);

  const hudState = useMemo(() => (
    session ? contract.getHudState({ session, elapsedSeconds }) : null
  ), [contract, elapsedSeconds, session]);

  return {
    elapsedSeconds,
    appState: timer.appState,
    isSessionVisible: timer.isSessionVisible,
    getCurrentElapsedSeconds: timer.getCurrentElapsedSeconds,
    pauseTimer: timer.pauseTimer,
    resumeTimer: timer.resumeTimer,
    resetTimer: timer.resetTimer,
    setElapsedBeforeSession,
    buildCompletionParams,
    createFreshSession,
    hudState,
    restoreRequestedPuzzle,
  };
}
