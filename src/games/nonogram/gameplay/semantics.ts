import {
  nonogramCanonicalMoves,
  type NonogramCanonicalMoveKey,
} from '../types';

export {
  nonogramCanonicalMoves,
  type NonogramCanonicalMoveKey,
};

export const nonogramGameSemantics = {
  analyzerMode: 'approved-move-path-teaching',
  canonicalMoves: nonogramCanonicalMoves,
  supportActions: ['mark-empty'] as const,
  proofModel: {
    hypotheticalBranching: false,
    approvedMovesOnly: true,
  },
  mistakePolicy: {
    hasMistakes: false,
    detection: 'none',
    affectsScore: false,
    endsRun: false,
  },
  hasLossCondition: false,
  engineConstraints: {
    templateGrid: true,
    permutationGeneration: true,
    boundedDeduction: true,
  },
} as const;
