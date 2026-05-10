export const engineDefinitionTemplate = {
  id: '__GAME_ID__',
  title: '__GAME_TITLE__',
  catalogPath: '__PATH_TO_PUZZLES_ALL__',
  entryIdPrefix: '__ENTRY_PREFIX__',
  listAllowedSizes: () => [5],
  listAllowedDifficulties: () => ['easy', 'medium', 'hard', 'expert'] as const,
  pickTargetDifficulty: () => 'easy',
  generateOne: '__WIRE_GENERATOR__',
  getEntryDedupeKey: '__DEFINE_DEDUPE_KEY__',
  reclassifyEntries: '__WIRE_RECLASSIFIER__',
} as const;
