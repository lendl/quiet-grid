import { useCallback } from 'react';
import type { MutableRefObject } from 'react';
import type { ActivePuzzle } from '../activePuzzleTypes';
import type { PuzzlePlayContractBase } from '../playContract';

interface UsePuzzleExitToHomeArgs<TSession, THud> {
  sessionRef: MutableRefObject<TSession | null>;
  contract: PuzzlePlayContractBase<TSession, THud>;
  pauseTimer: () => number;
  setRunning: (running: boolean) => void;
  saveActivePuzzle: (value: ActivePuzzle) => Promise<void>;
  onExit: () => void;
}

export function usePuzzleExitToHome<TSession, THud>({
  sessionRef,
  contract,
  pauseTimer,
  setRunning,
  saveActivePuzzle,
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
      await saveActivePuzzle(contract.serializeSession({ session: currentSession, elapsedSeconds }));
    }

    setRunning(false);
    onExit();
  }, [contract, onExit, pauseTimer, saveActivePuzzle, sessionRef, setRunning]);
}
