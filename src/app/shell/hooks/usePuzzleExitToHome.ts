import { useCallback } from 'react';
import type { MutableRefObject } from 'react';
import type { ActiveSession } from '../activeSessionTypes';
import type { PuzzlePlayContractBase } from '../playContract';

interface UsePuzzleExitToHomeArgs<TSession, THud> {
  sessionRef: MutableRefObject<TSession | null>;
  contract: PuzzlePlayContractBase<TSession, THud>;
  pauseTimer: () => number;
  setRunning: (running: boolean) => void;
  saveActiveSession: (value: ActiveSession) => Promise<void>;
  onExit: () => void;
}

export function usePuzzleExitToHome<TSession, THud>({
  sessionRef,
  contract,
  pauseTimer,
  setRunning,
  saveActiveSession,
  onExit,
}: UsePuzzleExitToHomeArgs<TSession, THud>) {
  return useCallback(async (sessionOverride?: TSession | null) => {
    const currentSession = sessionOverride ?? sessionRef.current;
    const elapsedSeconds = pauseTimer();

    if (
      currentSession
      && contract.isInProgress(currentSession)
      && contract.hasMeaningfulProgress(currentSession)
    ) {
      await saveActiveSession(contract.serializeSession({ session: currentSession, elapsedSeconds }));
    }

    setRunning(false);
    onExit();
  }, [contract, onExit, pauseTimer, saveActiveSession, sessionRef, setRunning]);
}
