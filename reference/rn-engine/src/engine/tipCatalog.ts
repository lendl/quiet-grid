import type { TakuzuNextMoveRuleKey } from '../games/takuzu/core';

// Keep this file in sync with src/games/takuzu/content/howToPlayTips.ts.

export type EngineTipKey = TakuzuNextMoveRuleKey;

export interface TipDefinition {
  key: EngineTipKey;
  weight: number;
  level: number;
}

export const TIP_KEYS: EngineTipKey[] = [
  'find-pairs',
  'avoid-trios',
  'complete-lines',
  'eliminate-filled-lines',
  'eliminate-impossible-combinations',
];

export const TIP_DEFINITIONS: TipDefinition[] = [
  { key: 'find-pairs', weight: 4, level: 1 },
  { key: 'avoid-trios', weight: 5, level: 1 },
  { key: 'complete-lines', weight: 7, level: 2 },
  { key: 'eliminate-filled-lines', weight: 11, level: 3 },
  { key: 'eliminate-impossible-combinations', weight: 14, level: 4 },
];

export const TIP_LEVELS: Record<EngineTipKey, number> = Object.fromEntries(
  TIP_DEFINITIONS.map(tip => [tip.key, tip.level]),
) as Record<EngineTipKey, number>;
