export interface Theme {
  background: string;
  surface: string;
  surfaceElevated: string;
  border: string;
  text: string;
  textSecondary: string;
  textMuted: string;
  primary: string;
  primaryLight: string;
  onPrimary: string;
  filled: string;
  filledBackground: string;
  given: string;
  givenText: string;
  success: string;
  panelSurface: string;
  panelSurfaceElevated: string;
  gridFrame: string;
  gridCellBackground: string;
  gridCellBorder: string;
  gridCellGivenText: string;
  gridCellEntryText: string;
  difficultyEasy: string;
  difficultyMedium: string;
  difficultyHard: string;
  difficultyExpert: string;
}

export type ThemeMode = 'dark' | 'light' | 'pencil';
export const DEFAULT_THEME_MODE: ThemeMode = 'dark';

export const darkTheme: Theme = {
  background: '#0d1117',
  surface: '#161b22',
  surfaceElevated: '#1f242d',
  border: '#30363d',
  text: '#f0f6fc',
  textSecondary: '#8b949e',
  textMuted: '#6e7681',
  primary: '#7c3aed',
  primaryLight: '#a78bfa',
  onPrimary: '#ffffff',
  filled: '#8b949e',
  filledBackground: '#161b22',
  given: '#161b22',
  givenText: '#f0f6fc',
  success: '#3fb950',
  panelSurface: '#161b22',
  panelSurfaceElevated: '#1f242d',
  gridFrame: '#30363d',
  gridCellBackground: '#161b22',
  gridCellBorder: '#30363d',
  gridCellGivenText: '#f0f6fc',
  gridCellEntryText: '#8b949e',
  difficultyEasy: '#4ade80',
  difficultyMedium: '#facc15',
  difficultyHard: '#fb923c',
  difficultyExpert: '#f87171',
};

export const lightTheme: Theme = {
  background: '#f6f8fa',
  surface: '#ffffff',
  surfaceElevated: '#f3f4f6',
  border: '#d0d7de',
  text: '#1f2328',
  textSecondary: '#57606a',
  textMuted: '#8c959f',
  primary: '#7c3aed',
  primaryLight: '#a78bfa',
  onPrimary: '#ffffff',
  filled: '#57606a',
  filledBackground: '#ffffff',
  given: '#ffffff',
  givenText: '#1f2328',
  success: '#1a7f37',
  panelSurface: '#ffffff',
  panelSurfaceElevated: '#f6f8fa',
  gridFrame: '#d0d7de',
  gridCellBackground: '#ffffff',
  gridCellBorder: '#d0d7de',
  gridCellGivenText: '#1f2328',
  gridCellEntryText: '#57606a',
  difficultyEasy: '#4ade80',
  difficultyMedium: '#facc15',
  difficultyHard: '#fb923c',
  difficultyExpert: '#f87171',
};


export const pencilTheme: Theme = {
  background: '#f7f7f7',          // soft paper white
  surface: '#ffffff',             // pure white card/panel
  surfaceElevated: '#f0f0f0',     // light gray for elevation
  border: '#c8c8c8',              // pencil-outline gray
  text: '#1a1a1a',                // near-black graphite
  textSecondary: '#4d4d4d',       // softer pencil
  textMuted: '#7a7a7a',           // light graphite

  primary: '#000000',             // pencil black (no color)
  primaryLight: '#4d4d4d',        // lighter graphite
  onPrimary: '#ffffff',

  filled: '#4d4d4d',              // pencil shading
  filledBackground: '#e6e6e6',    // light shaded cell

  given: '#ffffff',               // given cells look like clean paper
  givenText: '#1a1a1a',           // strong graphite

  success: '#4d4d4d',             // no green — pencil theme stays grayscale

  panelSurface: '#ffffff',
  panelSurfaceElevated: '#f0f0f0',

  gridFrame: '#b3b3b3',           // grid lines like pencil
  gridCellBackground: '#ffffff',
  gridCellBorder: '#d0d0d0',

  gridCellGivenText: '#1a1a1a',
  gridCellEntryText: '#4d4d4d',

  difficultyEasy: '#4d4d4d',      // all difficulty colors become grayscale
  difficultyMedium: '#7a7a7a',
  difficultyHard: '#999999',
  difficultyExpert: '#b3b3b3',
};

export function isThemeMode(value: string | null | undefined): value is ThemeMode {
  return value === 'dark' || value === 'light' || value === 'pencil';
}

export function getTheme(mode: ThemeMode): Theme {
  switch (mode) {
    case 'light':
      return lightTheme;
    case 'pencil':
      return pencilTheme;
    case 'dark':
    default:
      return darkTheme;
  }
}
