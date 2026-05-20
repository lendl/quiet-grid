import { useCallback, useEffect, useRef, useState } from 'react';
import type { Dispatch, MutableRefObject, SetStateAction } from 'react';
import type { RootStackParamList } from '../../navigation/types';
import type { FailureReason } from '../../loss/types';
import type { ActiveSession } from '../activeSessionTypes';
import type { PuzzlePlayContractBase } from '../playContract';
import type { GameId } from '../../../games/shared/types';
import type { PuzzleDifficulty } from '../types';
import { saveGameResult } from '../../utils/statsStorage';
import { usePuzzleExitToHome } from './usePuzzleExitToHome';
import { usePuzzlePlaySession } from './usePuzzlePlaySession';
import { usePuzzleSessionOrchestration } from './usePuzzleSessionOrchestration';
import { getGameAnalysisAdapter } from '../../analysisRegistry';

interface UsePuzzleControllerBootstrapArgs<TSession, THud> {
  isFocused: boolean;
  menuOpen: boolean;
  gameId: GameId;
  difficulty: PuzzleDifficulty;
  resumeRequested: boolean;
  contract: PuzzlePlayContractBase<TSession, THud>;
  onMissing: () => void | Promise<void>;
  onFreshMissing?: () => void | Promise<void>;
  saveActiveSession: (value: ActiveSession) => Promise<void>;
  clearActiveSession: () => Promise<void>;
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
  finishLossSession: (reason: FailureReason, sessionOverride?: TSession | null) => Promise<void>;
  completeExitToHome: (sessionOverride?: TSession | null) => Promise<void>;
  loadFreshSession: () => Promise<TSession | null>;
}

export function usePuzzleControllerBootstrap<TSession, THud>({
  isFocused,
  menuOpen,
  gameId,
  difficulty,
  resumeRequested,
  contract,
  onMissing,
  onFreshMissing,
  saveActiveSession,
  clearActiveSession,
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
    menuOpen,
    difficulty,
    session,
    contract,
    resumeRequested,
    onMissing,
    persistEnabled: !loading && running && session !== null,
    save: saveActiveSession,
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
    clearActiveSession,
    saveActiveSession,
    buildCompletionParams,
    onShowCompletion,
    onExit,
  });

  const finishLossSession = useCallback(async (
    reason: FailureReason,
    sessionOverride?: TSession | null,
  ) => {
    if (finalizedRef.current) return;

    const elapsedSeconds = pauseTimer();
    const currentSession = sessionOverride ?? sessionRef.current;
    const analysisSource = getGameAnalysisAdapter(gameId)?.buildAnalysisSource(currentSession);
    finalizedRef.current = true;
    setRunning(false);
    await clearActiveSession();

    // Save unsolved stats
    await saveGameResult({
      gameId,
      difficulty,
      status: 'failed',
    });

    onShowLoss({
      reason,
      gameId,
      difficulty,
      elapsedSeconds,
      analysisSource: analysisSource ?? undefined,
    });
  }, [
    clearActiveSession,
    difficulty,
    finalizedRef,
    gameId,
    onShowLoss,
    pauseTimer,
    sessionRef,
    setRunning,
  ]);

  const completeExitToHome = usePuzzleExitToHome({
    sessionRef,
    contract,
    pauseTimer,
    setRunning,
    saveActiveSession,
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
