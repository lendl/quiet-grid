import { useCallback, useEffect, useRef, useState } from 'react';
import type { Dispatch, MutableRefObject, SetStateAction } from 'react';
import type { RootStackParamList } from '../../navigation/types';
import type { LossReason } from '../../loss/types';
import type { ActivePuzzle } from '../activePuzzleTypes';
import type { PuzzlePlayContractBase } from '../playContract';
import type { PuzzleDifficulty, PuzzleTypeId } from '../types';
import { saveGameResult } from '../../utils/statsStorage';
import { usePuzzleExitToHome } from './usePuzzleExitToHome';
import { usePuzzlePlaySession } from './usePuzzlePlaySession';
import { usePuzzleSessionOrchestration } from './usePuzzleSessionOrchestration';
import { getPuzzleAnalysisAdapter } from '../../analysisRegistry';

interface UsePuzzleControllerBootstrapArgs<TSession, THud> {
  isFocused: boolean;
  puzzleTypeId: PuzzleTypeId;
  difficulty: PuzzleDifficulty;
  resumeRequested: boolean;
  contract: PuzzlePlayContractBase<TSession, THud>;
  onMissing: () => void | Promise<void>;
  onFreshMissing?: () => void | Promise<void>;
  saveActivePuzzle: (value: ActivePuzzle) => Promise<void>;
  clearActivePuzzle: () => Promise<void>;
  onShowCompletion: (params: RootStackParamList['Completion']) => void;
  onShowLoss: (params: RootStackParamList['Loss']) => void;
  onBeforeLoad: () => void;
  onCleanup?: () => void;
  onExit?: () => void;
}

export interface PuzzleControllerBootstrapResult<TSession, THud> {
  session: TSession | null;
  setSession: Dispatch<SetStateAction<TSession | null>>;
  loading: boolean;
  setLoading: Dispatch<SetStateAction<boolean>>;
  running: boolean;
  setRunning: Dispatch<SetStateAction<boolean>>;
  sessionRef: MutableRefObject<TSession | null>;
  finalizedRef: MutableRefObject<boolean>;
  getCurrentElapsedSeconds: () => number;
  pauseTimer: () => number;
  resetTimer: () => void;
  createFreshSession: () => TSession | null;
  restoreRequestedPuzzle: () => Promise<TSession | null>;
  hudState: THud | null;
  finishSolvedSession: (solvedSession?: TSession, showCompletionScreen?: boolean) => Promise<boolean>;
  finishLossSession: (reason: 'forfeit' | 'rule-based') => Promise<void>;
  completeExitToHome: (sessionOverride?: TSession | null) => Promise<void>;
  loadFreshSession: () => Promise<TSession | null>;
}

export function usePuzzleControllerBootstrap<TSession, THud>({
  isFocused,
  puzzleTypeId,
  difficulty,
  resumeRequested,
  contract,
  onMissing,
  onFreshMissing,
  saveActivePuzzle,
  clearActivePuzzle,
  onShowCompletion,
  onShowLoss,
  onBeforeLoad,
  onCleanup,
  onExit = () => {},
}: UsePuzzleControllerBootstrapArgs<TSession, THud>): PuzzleControllerBootstrapResult<TSession, THud> {
  const [session, setSession] = useState<TSession | null>(null);
  const [loading, setLoading] = useState(true);
  const [running, setRunning] = useState(false);

  const sessionRef = useRef<TSession | null>(null);
  const finalizedRef = useRef(false);

  const {
    getCurrentElapsedSeconds,
    pauseTimer,
    resetTimer,
    buildCompletionParams,
    createFreshSession,
    hudState,
    restoreRequestedPuzzle,
  } = usePuzzlePlaySession({
    isFocused,
    running,
    loading,
    difficulty,
    session,
    contract,
    resumeRequested,
    onMissing,
    persistEnabled: !loading && running && session !== null,
    save: saveActivePuzzle,
  });

  useEffect(() => {
    sessionRef.current = session;
  }, [session]);

  const { finishSolvedSession } = usePuzzleSessionOrchestration({
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
  });

  const finishLossSession = useCallback(async (reason: LossReason) => {
    if (finalizedRef.current) return;

    const elapsedSeconds = pauseTimer();
    const currentSession = sessionRef.current;
    const analysisSource = getPuzzleAnalysisAdapter(puzzleTypeId)?.buildLossAnalysisSource(currentSession);
    finalizedRef.current = true;
    setRunning(false);
    await clearActivePuzzle();

    // Save unsolved stats
    await saveGameResult({
      puzzleTypeId,
      difficulty,
      solved: false,
    });

    onShowLoss({
      reason,
      puzzleTypeId,
      difficulty,
      elapsedSeconds,
      analysisSource: analysisSource ?? undefined,
    });
  }, [
    clearActivePuzzle,
    difficulty,
    finalizedRef,
    onShowLoss,
    pauseTimer,
    puzzleTypeId,
    sessionRef,
    setRunning,
  ]);

  const completeExitToHome = usePuzzleExitToHome({
    sessionRef,
    contract,
    pauseTimer,
    setRunning,
    saveActivePuzzle,
    onExit,
  });

  const resetBootstrapState = useCallback(() => {
    setRunning(false);
    setSession(null);
    sessionRef.current = null;
    finalizedRef.current = false;
    resetTimer();
    onBeforeLoad();
  }, [onBeforeLoad, resetTimer]);

  const startFreshSession = useCallback(async (resetFirst: boolean) => {
    if (resetFirst) {
      resetBootstrapState();
    }
    const freshSession = createFreshSession();
    if (!freshSession) {
      setLoading(false);
      if (onFreshMissing) {
        await onFreshMissing();
      }
      return null;
    }

    sessionRef.current = freshSession;
    setSession(freshSession);
    setLoading(false);
    setRunning(contract.isInProgress(freshSession));
    return freshSession;
  }, [contract, createFreshSession, onFreshMissing, resetBootstrapState]);

  const loadFreshSession = useCallback(async () => (
    startFreshSession(true)
  ), [startFreshSession]);

  useEffect(() => {
    let mounted = true;

    async function loadGame(): Promise<void> {
      setLoading(true);
      resetBootstrapState();

      if (resumeRequested) {
        const restored = await restoreRequestedPuzzle();
        if (!mounted || !restored) {
          return;
        }

        sessionRef.current = restored;
        setSession(restored);
        setRunning(contract.isInProgress(restored));
        setLoading(false);

        if (!contract.isInProgress(restored)) {
          await finishSolvedSession(restored);
        }
        return;
      }

      await startFreshSession(false);
    }

    void loadGame();
    return () => {
      mounted = false;
      onCleanup?.();
    };
  }, [
    contract,
    finishSolvedSession,
    onCleanup,
    resetBootstrapState,
    restoreRequestedPuzzle,
    resumeRequested,
    startFreshSession,
  ]);

  return {
    session,
    setSession,
    loading,
    setLoading,
    running,
    setRunning,
    sessionRef,
    finalizedRef,
    getCurrentElapsedSeconds,
    pauseTimer,
    resetTimer,
    createFreshSession,
    restoreRequestedPuzzle,
    hudState,
    finishSolvedSession,
    finishLossSession,
    completeExitToHome,
    loadFreshSession,
  };
}
