import { useCallback, useState } from 'react';
import { useFocusEffect, useNavigation, type NavigationProp } from '@react-navigation/native';
import type { ActivePuzzle } from '../shell/activePuzzleTypes';
import type { RootStackParamList } from '../navigation/types';
import { resumeActivePuzzle } from '../utils/gameNavigation';
import { loadActivePuzzleState } from '../utils/activePuzzleStateStorage';

type RootNavigation = NavigationProp<RootStackParamList>;

export function useActivePuzzleNav() {
  const navigation = useNavigation<RootNavigation>();
  const [activePuzzle, setActivePuzzle] = useState<ActivePuzzle | null>(null);

  const refreshActivePuzzle = useCallback(async () => {
    const puzzle = await loadActivePuzzleState();
    setActivePuzzle(puzzle);
    return puzzle;
  }, []);

  useFocusEffect(useCallback(() => {
    void refreshActivePuzzle();
    return undefined;
  }, [refreshActivePuzzle]));

  const continuePuzzle = useCallback(async () => {
    const puzzle = activePuzzle ?? await refreshActivePuzzle();
    if (!puzzle) {
      return false;
    }

    resumeActivePuzzle(navigation, puzzle);
    return true;
  }, [activePuzzle, navigation, refreshActivePuzzle]);

  return {
    activePuzzle,
    refreshActivePuzzle,
    continuePuzzle,
  };
}
