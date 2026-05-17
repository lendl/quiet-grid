import { useCallback, useMemo } from 'react';
import type { ThemeMode } from '../theme';
import { useTheme } from '../context/ThemeContext';

const ORDER: ThemeMode[] = ['dark', 'light', 'pencil'];

function getNextThemeMode(themeMode: ThemeMode): ThemeMode {
  const currentIndex = ORDER.indexOf(themeMode);
  return ORDER[(currentIndex + 1) % ORDER.length];
}

export function useMenuThemeCycle() {
  const { themeMode, setThemeMode } = useTheme();

  const iconName = useMemo(() => {
    switch (themeMode) {
      case 'light':
        return 'sunny-outline' as const;
      case 'pencil':
        return 'pencil-outline' as const;
      case 'dark':
      default:
        return 'moon-outline' as const;
    }
  }, [themeMode]);

  const cycleTheme = useCallback(() => {
    setThemeMode(getNextThemeMode(themeMode));
  }, [setThemeMode, themeMode]);

  return {
    themeMode,
    iconName,
    cycleTheme,
  };
}
