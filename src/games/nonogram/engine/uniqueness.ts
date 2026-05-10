import type { NonogramPuzzle } from '../types';
import { countNonogramSolutions } from '../gameplay/rules/solver';

export function hasUniqueNonogramSolution(puzzle: NonogramPuzzle): boolean {
  return countNonogramSolutions(puzzle) === 1;
}
