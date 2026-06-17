export const chimpTestGameSemantics = {
  analyzerMode: 'loss-state-reflection',
  canonicalMoves: [] as const,
  supportActions: [] as const,
  proofModel: {
    hypotheticalBranching: false,
    approvedMovesOnly: false,
  },
  mistakePolicy: {
    hasMistakes: false,
    detection: 'none',
    affectsScore: false,
    endsRun: true,
  },
  hasLossCondition: true,
} as const;
