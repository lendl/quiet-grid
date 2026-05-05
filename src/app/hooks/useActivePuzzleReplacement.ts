import { useCallback, useRef, useState } from 'react';
import type { StackNavigationProp } from '@react-navigation/stack';
import type { ActivePuzzle } from '../shell/activePuzzleTypes';
import type { RootStackParamList } from '../navigation/types';
import { clearActivePuzzleState, loadActivePuzzleState } from '../utils/activePuzzleStateStorage';
import { resumeActivePuzzle } from '../utils/gameNavigation';
import { saveGameResult } from '../utils/statsStorage';
import { getActivePuzzleDifficulty } from '../utils/activePuzzle';

type RootNavigation = StackNavigationProp<RootStackParamList>;
type PendingAction = (() => void) | null;

interface UseActivePuzzleReplacementOptions {
  navigation: RootNavigation;
}

export function useActivePuzzleReplacement({ navigation }: UseActivePuzzleReplacementOptions) {
  const [activePuzzle, setActivePuzzle] = useState<ActivePuzzle | null>(null);
  const [dialogVisible, setDialogVisible] = useState(false);
  const pendingActionRef = useRef<PendingAction>(null);

  const syncActivePuzzle = useCallback(async () => {
    const puzzle = await loadActivePuzzleState();
    setActivePuzzle(puzzle);
    return puzzle;
  }, []);

  const dismissDialog = useCallback(() => {
    setDialogVisible(false);
    pendingActionRef.current = null;
  }, []);

  const requestStart = useCallback((action: () => void) => {
    if (!activePuzzle) {
      action();
      return;
    }

    pendingActionRef.current = action;
    setDialogVisible(true);
  }, [activePuzzle]);

  const handleContinue = useCallback(() => {
    if (!activePuzzle) {
      dismissDialog();
      return;
    }

    dismissDialog();
    resumeActivePuzzle(navigation, activePuzzle);
  }, [activePuzzle, dismissDialog, navigation]);

  const handleGiveUpAndStartNew = useCallback(async () => {
    if (!activePuzzle) {
      dismissDialog();
      return;
    }

    const queuedAction = pendingActionRef.current;
    const difficulty = getActivePuzzleDifficulty(activePuzzle);

    await saveGameResult({
      puzzleTypeId: activePuzzle.puzzleTypeId,
      difficulty,
      solved: false,
    });
    await clearActivePuzzleState();
    setActivePuzzle(null);
    dismissDialog();
    queuedAction?.();
  }, [activePuzzle, dismissDialog]);

  return {
    activePuzzle,
    setActivePuzzle,
    dialogVisible,
    syncActivePuzzle,
    requestStart,
    handleContinue,
    handleGiveUpAndStartNew,
    dismissDialog,
  };
}
