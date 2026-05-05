import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { AppStateStatus } from 'react-native';
import { AppState } from 'react-native';
import { getElapsedSeconds } from './gameTimerUtils';

interface UseGameTimerArgs {
  enabled: boolean;
}

export { getElapsedSeconds };

export function useGameTimer({ enabled }: UseGameTimerArgs) {
  const [elapsed, setElapsed] = useState(0);
  const [appState, setAppState] = useState<AppStateStatus>(AppState.currentState);
  const sessionStartedAtRef = useRef<number | null>(null);
  const elapsedBeforeSessionRef = useRef(0);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const getCurrentElapsedSeconds = useCallback(() => (
    getElapsedSeconds(sessionStartedAtRef.current, elapsedBeforeSessionRef.current)
  ), []);

  const pauseTimer = useCallback(() => {
    const paused = getCurrentElapsedSeconds();
    sessionStartedAtRef.current = null;
    elapsedBeforeSessionRef.current = paused;
    setElapsed(paused);
    return paused;
  }, [getCurrentElapsedSeconds]);

  const resumeTimer = useCallback(() => {
    if (sessionStartedAtRef.current !== null) return;
    sessionStartedAtRef.current = Date.now();
  }, []);

  const resetTimer = useCallback(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
    sessionStartedAtRef.current = null;
    elapsedBeforeSessionRef.current = 0;
    setElapsed(0);
  }, []);

  const setElapsedBeforeSession = useCallback((value: number) => {
    sessionStartedAtRef.current = null;
    elapsedBeforeSessionRef.current = value;
    setElapsed(value);
  }, []);

  useEffect(() => {
    const subscription = AppState.addEventListener('change', setAppState);
    return () => {
      subscription.remove();
    };
  }, []);

  const isSessionVisible = useMemo(() => enabled && appState === 'active', [appState, enabled]);

  useEffect(() => {
    if (!isSessionVisible) {
      pauseTimer();
      if (timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }
      return;
    }

    resumeTimer();
    timerRef.current = setInterval(() => {
      setElapsed(getCurrentElapsedSeconds());
    }, 1000);

    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }
    };
  }, [getCurrentElapsedSeconds, isSessionVisible, pauseTimer, resumeTimer]);

  return {
    elapsed,
    appState,
    isSessionVisible,
    getCurrentElapsedSeconds,
    pauseTimer,
    resumeTimer,
    resetTimer,
    setElapsedBeforeSession,
  };
}
