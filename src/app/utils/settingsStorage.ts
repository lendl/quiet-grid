import AsyncStorage from '@react-native-async-storage/async-storage';
import type { GameId } from '../../games/shared/types';
import { gameIds } from '../../games/shared/types';
import { DEFAULT_THEME_MODE, isThemeMode } from '../theme';
import {
  BETA_GAMES_ENABLED_KEY,
  HOW_TO_PLAY_AUTO_SHOW_KEY,
  HOW_TO_PLAY_SEEN_KEY,
  LANGUAGE_KEY,
  SHOW_TIMER_IN_PLAY_KEY,
  THEME_KEY,
  WELCOME_SEEN_KEY,
} from './storageKeys';

import type { ThemeMode } from '../theme';
export type LanguageSetting = 'en' | 'nl' | 'de' | 'fr' | 'es';

type SeenHowToPlayMap = Partial<Record<GameId, boolean>>;

function parseSeenHowToPlay(value: string | null): SeenHowToPlayMap {
  if (!value) {
    return {};
  }

  try {
    const parsed = JSON.parse(value) as unknown;
    if (!parsed || typeof parsed !== 'object') {
      return {};
    }

    const map = parsed as Record<string, unknown>;
    const seen: SeenHowToPlayMap = {};
    gameIds.forEach((gameId) => {
      if (map[gameId] === true) {
        seen[gameId] = true;
      }
    });
    if (map.binary === true) {
      seen.takuzu = true;
    }

    return seen;
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
    return value === 'en'
      || value === 'nl'
      || value === 'de'
      || value === 'fr'
      || value === 'es'
      ? value
      : null;
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

export async function loadHowToPlayAutoShow(): Promise<boolean> {
  try {
    const value = await AsyncStorage.getItem(HOW_TO_PLAY_AUTO_SHOW_KEY);
    return value !== 'false';
  } catch {
    return true;
  }
}

export async function saveHowToPlayAutoShow(enabled: boolean): Promise<void> {
  try {
    await AsyncStorage.setItem(HOW_TO_PLAY_AUTO_SHOW_KEY, String(enabled));
  } catch {
    // Keep app stable if how-to-play preference save fails.
  }
}

export async function hasSeenGameHowToPlay(gameId: GameId): Promise<boolean> {
  try {
    const stored = await AsyncStorage.getItem(HOW_TO_PLAY_SEEN_KEY);
    return parseSeenHowToPlay(stored)[gameId] === true;
  } catch {
    return false;
  }
}

export async function markGameHowToPlaySeen(gameId: GameId): Promise<void> {
  try {
    const stored = await AsyncStorage.getItem(HOW_TO_PLAY_SEEN_KEY);
    const seen = parseSeenHowToPlay(stored);
    seen[gameId] = true;
    await AsyncStorage.setItem(HOW_TO_PLAY_SEEN_KEY, JSON.stringify(seen));
  } catch {
    // Keep app stable if how-to-play progress save fails.
  }
}

export async function loadBetaGamesEnabled(): Promise<boolean> {
  try {
    const value = await AsyncStorage.getItem(BETA_GAMES_ENABLED_KEY);
    return value === 'true';
  } catch {
    return false;
  }
}

export async function saveBetaGamesEnabled(enabled: boolean): Promise<void> {
  try {
    await AsyncStorage.setItem(BETA_GAMES_ENABLED_KEY, String(enabled));
  } catch {
    // Keep app stable if beta games preference save fails.
  }
}

export async function shouldAutoShowHowToPlay(gameId: GameId): Promise<boolean> {
  const [autoShowEnabled, alreadySeen] = await Promise.all([
    loadHowToPlayAutoShow(),
    hasSeenGameHowToPlay(gameId),
  ]);

  return autoShowEnabled && !alreadySeen;
}
