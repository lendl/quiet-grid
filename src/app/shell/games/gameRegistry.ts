import { takuzuDefinition } from '../../../games/takuzu/definition';
import { minesweeperDefinition } from '../../../games/minesweeper/definition';
import { nonogramDefinition } from '../../../games/nonogram/definition';
import { sudokuDefinition } from '../../../games/sudoku/definition';
import { wordSearchDefinition } from '../../../games/wordsearch/definition';
import { chimpTestDefinition } from '../../../games/chimptest/definition';
import type { GameDefinition } from './gameDefinition';

export const gameRegistry = [
  takuzuDefinition,
  minesweeperDefinition,
  nonogramDefinition,
  sudokuDefinition,
  wordSearchDefinition,
  chimpTestDefinition,
] as const satisfies readonly GameDefinition[];

export function getGameDefinition(id: string): GameDefinition {
  const match = gameRegistry.find((definition) => definition.id === id);
  if (!match) {
    throw new Error(`Unknown game: ${id}`);
  }

  return match;
}

