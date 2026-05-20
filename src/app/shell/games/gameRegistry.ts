import { takuzuDefinition } from '../../../games/takuzu/definition';
import { minesweeperDefinition } from '../../../games/minesweeper/definition';
import type { GameDefinition } from './gameDefinition';

export const gameRegistry = [
  takuzuDefinition,
  minesweeperDefinition,
] as const satisfies readonly GameDefinition[];

export function getGameDefinition(id: string): GameDefinition {
  const match = gameRegistry.find((definition) => definition.id === id);
  if (!match) {
    throw new Error(`Unknown game: ${id}`);
  }

  return match;
}
