export const takuzuCanonicalMoves = [
  'find-pairs',
  'avoid-trios',
  'complete-lines',
  'eliminate-filled-lines',
  'eliminate-impossible-combinations',
] as const;

export type TakuzuCanonicalMoveKey = typeof takuzuCanonicalMoves[number];

export function isTakuzuCanonicalMoveKey(value: string): value is TakuzuCanonicalMoveKey {
  return takuzuCanonicalMoves.includes(value as TakuzuCanonicalMoveKey);
}

export const takuzuGameSemantics = {
  analyzerMode: 'full-solution-teaching',
  canonicalMoves: takuzuCanonicalMoves,
  supportActions: [] as const,
  mistakePolicy: {
    hasMistakes: true,
    detection: 'completed-line-validation',
    affectsScore: true,
    tracksPenalizedLines: true,
    endsRun: false,
  },
} as const;
