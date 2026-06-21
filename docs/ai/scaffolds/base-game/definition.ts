export const baseGameDefinitionTemplate = {
  id: '__GAME_ID__',
  get title() {
    return '__GAME_TITLE__';
  },
  get shortTitle() {
    return '__GAME_SHORT_TITLE__';
  },
  get tagline() {
    return '__GAME_TAGLINE__';
  },
  gestureProfile: '__{ mode: tap } | { mode: swipe } | { mode: zoom, viewport: required|optional }__',
  supports: {
    tutorial: true,
    learning: true,
    scoring: true,
  },
  difficulties: ['easy', 'medium', 'hard', 'expert'] as const,
  playAdapter: '__WIRE_PLAY_ADAPTER__',
  createResult: '__WIRE_RESULT_ADAPTER_OR_THROW__',
  content: {
    get howToPlay() {
      return '__LOAD_FROM_CONTENT_I18N__';
    },
    get loss() {
      return '__LOAD_FROM_CONTENT_I18N__';
    },
    get difficultyLabels() {
      return '__LOAD_FROM_CONTENT_I18N__';
    },
    get difficultyDescriptions() {
      return '__LOAD_FROM_CONTENT_I18N__';
    },
  },
  screens: {
    tutorial: '__OPTIONAL_TUTORIAL_SCREEN__',
  },
} as const;
