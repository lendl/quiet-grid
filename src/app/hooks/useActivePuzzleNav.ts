import { useCallback, useState } from 'react';
import { useFocusEffect, useNavigation, type NavigationProp } from '@react-navigation/native';
import type { ActiveSession } from '../shell/activeSessionTypes';
import type { RootStackParamList } from '../navigation/types';
import { resumeActiveSession } from '../utils/gameNavigation';
import { loadActiveSessionState } from '../utils/activeSessionStateStorage';

type RootNavigation = NavigationProp<RootStackParamList>;

export function useActivePuzzleNav() {
  const navigation = useNavigation<RootNavigation>();
  const [activeSession, setActiveSession] = useState<ActiveSession | null>(null);

  const refreshActivePuzzle = useCallback(async () => {
    const session = await loadActiveSessionState();
    setActiveSession(session);
    return session;
  }, []);

  useFocusEffect(useCallback(() => {
    void refreshActivePuzzle();
    return undefined;
  }, [refreshActivePuzzle]));

  const continuePuzzle = useCallback(async () => {
    const session = activeSession ?? await refreshActivePuzzle();
    if (!session) {
      return false;
    }

    resumeActiveSession(navigation, session);
    return true;
  }, [activeSession, navigation, refreshActivePuzzle]);

  return {
    activePuzzle: activeSession,
    refreshActivePuzzle,
    continuePuzzle,
  };
}
