import type { NonogramCellState } from '../types';
import type { NonogramPlaySession } from './activePuzzle';
import {
  applyNonogramAction,
  type NonogramActionResult,
} from './rules/board';

export type NonogramAction =
  | { type: 'toggle-cell'; index: number }
  | { type: 'paint-cell'; index: number; state: NonogramCellState }
  | { type: 'paint-cells'; indices: number[]; state: NonogramCellState }
  | { type: 'replace-session'; session: NonogramPlaySession };

export { applyNonogramAction };
export type { NonogramActionResult };
