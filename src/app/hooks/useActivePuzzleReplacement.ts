import { useCallback, useRef, useState } from 'react';
import type { NavigationProp } from '@react-navigation/native';
import type { ActiveSession } from '../shell/activeSessionTypes';
import type { RootStackParamList } from '../navigation/types';
import { clearActiveSessionState, loadActiveSessionState } from '../utils/activeSessionStateStorage';
import { resumeActiveSession } from '../utils/gameNavigation';
import { saveGameResult } from '../utils/statsStorage';
import { getActivePuzzleDifficulty } from '../utils/activePuzzle';

type RootNavigation = NavigationProp<RootStackParamList>;
type PendingAction = (() => void) | null;

interface UseActivePuzzleReplacementOptions {
  navigation: RootNavigation;
}

export function useActivePuzzleReplacement({ navigation }: UseActivePuzzleReplacementOptions) {
  const [activeSession, setActiveSession] = useState<ActiveSession | null>(null);
  const [dialogVisible, setDialogVisible] = useState(false);
  const pendingActionRef = useRef<PendingAction>(null);

  const syncActivePuzzle = useCallback(async () => {
    const session = await loadActiveSessionState();
    setActiveSession(session);
    return session;
  }, []);

  const dismissDialog = useCallback(() => {
    setDialogVisible(false);
    pendingActionRef.current = null;
  }, []);

  const requestStart = useCallback((action: () => void) => {
    if (!activeSession) {
      action();
      return;
    }

    pendingActionRef.current = action;
    setDialogVisible(true);
  }, [activeSession]);

  const handleContinue = useCallback(() => {
    if (!activeSession) {
      dismissDialog();
      return;
    }

    dismissDialog();
    resumeActiveSession(navigation, activeSession);
  }, [activeSession, dismissDialog, navigation]);

  const handleGiveUpAndStartNew = useCallback(async () => {
    if (!activeSession) {
      dismissDialog();
      return;
    }

    const queuedAction = pendingActionRef.current;
    const difficulty = getActivePuzzleDifficulty(activeSession);

    await saveGameResult({
      gameId: activeSession.gameId,
      difficulty,
      status: 'failed',
    });
    await clearActiveSessionState();
    setActiveSession(null);
    dismissDialog();
    queuedAction?.();
  }, [activeSession, dismissDialog]);

  return {
    activePuzzle: activeSession,
    setActivePuzzle: setActiveSession,
    dialogVisible,
    syncActivePuzzle,
    requestStart,
    handleContinue,
    handleGiveUpAndStartNew,
    dismissDialog,
  };
}
