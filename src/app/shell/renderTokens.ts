import type { Theme } from '../theme';
import { withAlpha } from '../utils/color';

export interface SharedBoardRenderTokens {
  panelFill: string;
  panelBorder: string;
  subtleGridFill: string;
  boardBorder: string;
  boardInset: string;
  cellRaisedFill: string;
  cellSunkenFill: string;
  cellBevelLight: string;
  cellBevelDark: string;
  text: string;
  textSecondary: string;
  textMuted: string;
  primary: string;
  primaryLight: string;
  success: string;
  danger: string;
  onPrimary: string;
}

export function createSharedBoardRenderTokens(
  theme: Theme,
  isDark: boolean,
): SharedBoardRenderTokens {
  return {
    panelFill: withAlpha(theme.panelSurface, isDark ? 0.98 : 1),
    panelBorder: withAlpha(theme.border, isDark ? 0.88 : 0.82),
    subtleGridFill: withAlpha(theme.panelSurfaceElevated, isDark ? 0.96 : 0.98),
    boardBorder: withAlpha(theme.text, isDark ? 0.18 : 0.12),
    boardInset: withAlpha(theme.background, isDark ? 0.4 : 0.55),
    cellRaisedFill: withAlpha(theme.surfaceElevated, isDark ? 0.98 : 0.94),
    cellSunkenFill: withAlpha(theme.background, isDark ? 0.9 : 1),
    cellBevelLight: withAlpha(theme.onPrimary, isDark ? 0.12 : 0.68),
    cellBevelDark: withAlpha(theme.text, isDark ? 0.26 : 0.16),
    text: theme.text,
    textSecondary: theme.textSecondary,
    textMuted: theme.textMuted,
    primary: theme.primary,
    primaryLight: theme.primaryLight,
    success: theme.success,
    danger: theme.difficultyExpert,
    onPrimary: theme.onPrimary,
  };
}
