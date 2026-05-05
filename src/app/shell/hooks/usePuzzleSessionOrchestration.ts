import { useCallback } from 'react';
import type { MutableRefObject } from 'react';
import type { RootStackParamList } from '../../navigation/types';
import type { ActivePuzzle } from '../activePuzzleTypes';
import type { PuzzleOutcome } from '../types';
import type { PuzzlePlayContractBase } from '../playContract';
import { saveSolvedOutcome } from '../../completion/utils';
import type { CompletionVariant } from '../../completion/types';

interface UsePuzzleSessionOrchestrationArgs<TSession, THud> {
  sessionRef: MutableRefObject<TSession | null>;
  finalizedRef: MutableRefObject<boolean>;
  contract: PuzzlePlayContractBase<TSession, THud>;
  getCurrentElapsedSeconds: () => number;
  pauseTimer: () => number;
  setRunning: (running: boolean) => void;
  clearActivePuzzle: () => Promise<void>;
  saveActivePuzzle: (value: ActivePuzzle) => Promise<void>;
  buildCompletionParams: (
    outcome: PuzzleOutcome,
    variant: CompletionVariant,
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
  clearActivePuzzle,
  saveActivePuzzle,
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
    await clearActivePuzzle();
    const savedOutcome = await saveSolvedOutcome(solvedState);

    if (showCompletionScreen) {
      onShowCompletion(buildCompletionParams(savedOutcome.outcome, savedOutcome.variant));
    }

    return true;
  }, [
    buildCompletionParams,
    clearActivePuzzle,
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
      await saveActivePuzzle(contract.serializeSession({ session: currentSession, elapsedSeconds }));
    }
    setRunning(false);
    onExit();
  }, [contract, onExit, pauseTimer, saveActivePuzzle, sessionRef, setRunning]);

  return {
    finishSolvedSession,
    saveAndExit,
  };
}
