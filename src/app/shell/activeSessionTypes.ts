import type { TakuzuActiveSession } from '../../games/takuzu/activePuzzle';
import type { MinesweeperActiveSession } from '../../games/minesweeper/activePuzzle';
import type { NonogramActiveSession } from '../../games/nonogram/activePuzzle';
import type { SudokuActiveSession } from '../../games/sudoku/activePuzzle';

export type { TakuzuActiveSession } from '../../games/takuzu/activePuzzle';
export type { MinesweeperActiveSession } from '../../games/minesweeper/activePuzzle';
export type { NonogramActiveSession } from '../../games/nonogram/activePuzzle';
export type { SudokuActiveSession } from '../../games/sudoku/activePuzzle';

export type ActiveSession =
  | TakuzuActiveSession
  | MinesweeperActiveSession
  | NonogramActiveSession
  | SudokuActiveSession;
