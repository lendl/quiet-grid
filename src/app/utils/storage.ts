export {
  clearActiveSessionState,
  loadActiveSessionState,
  makeEmptyBooleanGrid,
  saveActiveSessionState,
} from './activeSessionStateStorage';

export {
  clearPlayerData,
  clearStats,
  loadStats,
  saveGameResult,
  DEFAULT_STATS,
} from './statsStorage';

export type {
  SaveGameResultInput,
  SaveGameResultOutcome,
} from './statsStorage';

export {
  hasSeenGameHowToPlay,
  hasSeenWelcome,
  loadHowToPlayAutoShow,
  loadTheme,
  markGameHowToPlaySeen,
  markWelcomeSeen,
  saveHowToPlayAutoShow,
  saveTheme,
  shouldAutoShowHowToPlay,
} from './settingsStorage';
