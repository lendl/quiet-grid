import type { Grid } from '../games/binary/core';
import { countSolutions as countBinarySolutions } from '../games/binary/core';

export function countSolutions(puzzle: Grid, maxCount = 2): number {
  return countBinarySolutions(puzzle, maxCount);
}
