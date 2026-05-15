import type { Theme } from '../../theme';
import { withAlpha } from '../../utils/color';

export interface SharedBoardRenderTokens {
  panelFill: string;
  panelBorder: string;
  subtleGridFill: string;
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
    panelFill: withAlpha(theme.surfaceElevated, isDark ? 0.92 : 0.98),
    panelBorder: withAlpha(theme.border, isDark ? 0.88 : 0.82),
    subtleGridFill: withAlpha(theme.text, isDark ? 0.05 : 0.08),
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
