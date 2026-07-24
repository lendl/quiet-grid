import type { PuzzleDifficulty } from '../../../shared/types';
import type { SudokuBitmaskState } from './bitmask';
import type { SudokuCanonicalMove } from './moves';
import type { SudokuTechnique } from './techniques';

export interface SudokuTechniqueDispatcher {
  technique: SudokuTechnique;
  tier: PuzzleDifficulty;
  findMove(state: SudokuBitmaskState): SudokuCanonicalMove | null;
}

export interface SudokuTechniqueDispatcherState {
  state: SudokuBitmaskState;
}
