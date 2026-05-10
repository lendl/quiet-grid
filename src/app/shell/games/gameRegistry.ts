import { takuzuDefinition } from '../../../games/takuzu/definition';
import { minesweeperDefinition } from '../../../games/minesweeper/definition';
import { nonogramDefinition } from '../../../games/nonogram/definition';
import type { PuzzleDefinition } from './gameDefinition';

export const puzzleRegistry = [
  takuzuDefinition,
  minesweeperDefinition,
  nonogramDefinition,
] as const satisfies readonly PuzzleDefinition[];

export function getPuzzleDefinition(id: string): PuzzleDefinition {
  const match = puzzleRegistry.find((definition) => definition.id === id);
  if (!match) {
    throw new Error(`Unknown puzzle type: ${id}`);
  }

  return match;
}
