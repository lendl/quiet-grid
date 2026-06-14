import { MD3DarkTheme, MD3LightTheme, type MD3Theme } from 'react-native-paper';
import { type ThemeMode, getTheme } from './index';

export function buildPaperTheme(mode: ThemeMode): MD3Theme {
  const t = getTheme(mode);
  const base = mode === 'dark' ? MD3DarkTheme : MD3LightTheme;

  return {
    ...base,
    colors: {
      ...base.colors,
      primary: t.primary,
      onPrimary: t.onPrimary,
      primaryContainer: t.primaryLight,
      onPrimaryContainer: t.onPrimary,
      secondary: t.primaryLight,
      onSecondary: t.background,
      background: t.background,
      onBackground: t.text,
      surface: t.surface,
      onSurface: t.text,
      surfaceVariant: t.surfaceElevated,
      onSurfaceVariant: t.textSecondary,
      outline: t.border,
      outlineVariant: t.border,
      error: t.difficultyExpert,
      onError: '#ffffff',
      elevation: {
        ...base.colors.elevation,
        level0: 'transparent',
        level1: t.surface,
        level2: t.surfaceElevated,
        level3: t.surfaceElevated,
        level4: t.surfaceElevated,
        level5: t.surfaceElevated,
      },
    },
  };
}
