export const playContractTemplate = {
  id: '__GAME_ID__',
  canonicalMoves: [
    {
      key: '__MOVE_KEY__',
      title: '__MOVE_TITLE__',
      summary: '__WHY_THIS_MOVE_WORKS__',
    },
  ],
  supportActions: [
    {
      key: '__SUPPORT_ACTION_KEY__',
      title: '__SUPPORT_ACTION_TITLE__',
      optional: true,
    },
  ],
  analyzerMode: 'loss-state',
  hasMistakePolicy: true,
  hasLossCondition: false,
} as const;
