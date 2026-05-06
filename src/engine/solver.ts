import type { Grid } from '../games/takuzu/core';
import { countSolutions as countTakuzuSolutions } from '../games/takuzu/core';

export function countSolutions(puzzle: Grid, maxCount = 2): number {
  return countTakuzuSolutions(puzzle, maxCount);
}
