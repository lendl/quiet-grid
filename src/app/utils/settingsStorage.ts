import AsyncStorage from '@react-native-async-storage/async-storage';
import type { PuzzleTypeId } from '../shell/types';
import { DEFAULT_THEME_MODE, isThemeMode } from '../theme';
import {
  LANGUAGE_KEY,
  PUZZLE_TUTORIALS_SEEN_KEY,
  SHOW_TIMER_IN_PLAY_KEY,
  THEME_KEY,
  TUTORIALS_ENABLED_KEY,
  WELCOME_SEEN_KEY,
} from './storageKeys';

import type { ThemeMode } from '../theme';
export type LanguageSetting = 'en' | 'nl';

type SeenTutorialsMap = Partial<Record<PuzzleTypeId, boolean>>;

function parseSeenTutorials(value: string | null): SeenTutorialsMap {
  if (!value) {
    return {};
  }

  try {
    const parsed = JSON.parse(value) as unknown;
    if (!parsed || typeof parsed !== 'object') {
      return {};
    }

    const tutorialMap = parsed as Record<string, unknown>;

    return {
      takuzu: tutorialMap.takuzu === true || tutorialMap.binary === true ? true : undefined,
      minesweeper: tutorialMap.minesweeper === true ? true : undefined,
      nonogram: tutorialMap.nonogram === true ? true : undefined,
    };
  } catch {
    return {};
  }
}

export async function loadTheme(): Promise<ThemeMode> {
  try {
    const value = await AsyncStorage.getItem(THEME_KEY);
    return isThemeMode(value) ? value : DEFAULT_THEME_MODE;
  } catch {
    return DEFAULT_THEME_MODE;
  }
}

export async function saveTheme(theme: ThemeMode): Promise<void> {
  try {
    await AsyncStorage.setItem(THEME_KEY, theme);
  } catch {
    // Keep app stable if theme save fails.
  }
}

export async function loadLanguageSetting(): Promise<LanguageSetting | null> {
  try {
    const value = await AsyncStorage.getItem(LANGUAGE_KEY);
    return value === 'en' || value === 'nl' ? value : null;
  } catch {
    return null;
  }
}

export async function saveLanguageSetting(setting: LanguageSetting): Promise<void> {
  try {
    await AsyncStorage.setItem(LANGUAGE_KEY, setting);
  } catch {
    // Keep app stable if language save fails.
  }
}

export async function loadShowTimerInPlay(): Promise<boolean> {
  try {
    const value = await AsyncStorage.getItem(SHOW_TIMER_IN_PLAY_KEY);
    return value !== 'false';
  } catch {
    return true;
  }
}

export async function saveShowTimerInPlay(enabled: boolean): Promise<void> {
  try {
    await AsyncStorage.setItem(SHOW_TIMER_IN_PLAY_KEY, String(enabled));
  } catch {
    // Keep app stable if timer preference save fails.
  }
}

export async function hasSeenWelcome(): Promise<boolean> {
  try {
    return (await AsyncStorage.getItem(WELCOME_SEEN_KEY)) === 'true';
  } catch {
    return false;
  }
}

export async function markWelcomeSeen(): Promise<void> {
  try {
    await AsyncStorage.setItem(WELCOME_SEEN_KEY, 'true');
  } catch {
    // Keep app stable if onboarding save fails.
  }
}

export async function loadTutorialsEnabled(): Promise<boolean> {
  try {
    const value = await AsyncStorage.getItem(TUTORIALS_ENABLED_KEY);
    return value !== 'false';
  } catch {
    return true;
  }
}

export async function saveTutorialsEnabled(enabled: boolean): Promise<void> {
  try {
    await AsyncStorage.setItem(TUTORIALS_ENABLED_KEY, String(enabled));
  } catch {
    // Keep app stable if tutorial preference save fails.
  }
}

export async function hasSeenPuzzleTutorial(puzzleTypeId: PuzzleTypeId): Promise<boolean> {
  try {
    const stored = await AsyncStorage.getItem(PUZZLE_TUTORIALS_SEEN_KEY);
    return parseSeenTutorials(stored)[puzzleTypeId] === true;
  } catch {
    return false;
  }
}

export async function markPuzzleTutorialSeen(puzzleTypeId: PuzzleTypeId): Promise<void> {
  try {
    const stored = await AsyncStorage.getItem(PUZZLE_TUTORIALS_SEEN_KEY);
    const seenTutorials = parseSeenTutorials(stored);
    seenTutorials[puzzleTypeId] = true;
    await AsyncStorage.setItem(PUZZLE_TUTORIALS_SEEN_KEY, JSON.stringify(seenTutorials));
  } catch {
    // Keep app stable if tutorial progress save fails.
  }
}

export async function shouldAutoShowTutorial(puzzleTypeId: PuzzleTypeId): Promise<boolean> {
  const [tutorialsEnabled, tutorialSeen] = await Promise.all([
    loadTutorialsEnabled(),
    hasSeenPuzzleTutorial(puzzleTypeId),
  ]);

  return tutorialsEnabled && !tutorialSeen;
}
