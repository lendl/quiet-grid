import type { Grid, TakuzuNextMoveHint } from '../../types';
import { findPairs } from './techniqueModules/findPairs';
import { avoidTrios } from './techniqueModules/avoidTrios';
import { completeLines } from './techniqueModules/completeLines';
import { eliminateFilledLines } from './techniqueModules/eliminateFilledLines';
import { eliminateImpossibleCombinations } from './techniqueModules/eliminateImpossibleCombinations';

export function getTakuzuProgressHint(board: Grid): TakuzuNextMoveHint | null {
  return (
    findPairs(board) ??
    avoidTrios(board) ??
    completeLines(board) ??
    eliminateFilledLines(board) ??
    eliminateImpossibleCombinations(board)
  );
}
