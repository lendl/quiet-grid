export {
  clearActivePuzzleState,
  loadActivePuzzleState,
  makeEmptyBooleanGrid,
  saveActivePuzzleState,
} from './activePuzzleStateStorage';

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
  hasSeenPuzzleTutorial,
  hasSeenWelcome,
  loadTheme,
  loadTutorialsEnabled,
  markPuzzleTutorialSeen,
  markWelcomeSeen,
  saveTheme,
  saveTutorialsEnabled,
  shouldAutoShowTutorial,
} from './settingsStorage';
