import { useCallback } from 'react';
import type { MutableRefObject } from 'react';
import type { RootStackParamList } from '../../navigation/types';
import type { ActiveSession } from '../activeSessionTypes';
import type { SessionResult } from '../types';
import type { PuzzlePlayContractBase } from '../playContract';
import { saveSolvedResult } from '../../completion/utils';
import type { SolvedResultVariant } from '../../completion/types';

interface UsePuzzleSessionOrchestrationArgs<TSession, THud> {
  sessionRef: MutableRefObject<TSession | null>;
  finalizedRef: MutableRefObject<boolean>;
  contract: PuzzlePlayContractBase<TSession, THud>;
  getCurrentElapsedSeconds: () => number;
  pauseTimer: () => number;
  setRunning: (running: boolean) => void;
  clearActiveSession: () => Promise<void>;
  saveActiveSession: (value: ActiveSession) => Promise<void>;
  buildCompletionParams: (
    result: SessionResult,
    variant: SolvedResultVariant,
  ) => RootStackParamList['Completion'];
  onShowCompletion: (params: RootStackParamList['Completion']) => void;
  onExit: () => void;
}

export function usePuzzleSessionOrchestration<TSession, THud>({
  sessionRef,
  finalizedRef,
  contract,
  getCurrentElapsedSeconds,
  pauseTimer,
  setRunning,
  clearActiveSession,
  saveActiveSession,
  buildCompletionParams,
  onShowCompletion,
  onExit,
}: UsePuzzleSessionOrchestrationArgs<TSession, THud>) {
  const finishSolvedSession = useCallback(async (
    solvedSession?: TSession,
    showCompletionScreen = true,
  ): Promise<boolean> => {
    const currentSession = solvedSession ?? sessionRef.current;
    if (!currentSession) return false;
    if (finalizedRef.current) return true;

    const previewSolvedState = contract.getSolvedState({
      session: currentSession,
      elapsedSeconds: getCurrentElapsedSeconds(),
    });
    if (!previewSolvedState) return false;

    const elapsedSeconds = pauseTimer();
    const solvedState = contract.getSolvedState({
      session: currentSession,
      elapsedSeconds,
    });
    if (!solvedState) return false;

    finalizedRef.current = true;
    setRunning(false);
    await clearActiveSession();
    const savedResult = await saveSolvedResult(solvedState);

    if (showCompletionScreen) {
      onShowCompletion(buildCompletionParams(savedResult.result, savedResult.variant));
    }

    return true;
  }, [
    buildCompletionParams,
    clearActiveSession,
    contract,
    finalizedRef,
    getCurrentElapsedSeconds,
    onShowCompletion,
    pauseTimer,
    sessionRef,
    setRunning,
  ]);

  const saveAndExit = useCallback(async (session?: TSession | null) => {
    const currentSession = session ?? sessionRef.current;
    const elapsedSeconds = pauseTimer();
    if (currentSession && contract.isInProgress(currentSession)) {
      await saveActiveSession(contract.serializeSession({ session: currentSession, elapsedSeconds }));
    }
    setRunning(false);
    onExit();
  }, [contract, onExit, pauseTimer, saveActiveSession, sessionRef, setRunning]);

  return {
    finishSolvedSession,
    saveAndExit,
  };
}
