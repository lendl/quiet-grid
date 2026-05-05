import type { BinaryActivePuzzle } from '../../games/binary/activePuzzle';
import type { MinesweeperActivePuzzle } from '../../games/minesweeper/activePuzzle';

export type { BinaryActivePuzzle } from '../../games/binary/activePuzzle';
export type { MinesweeperActivePuzzle } from '../../games/minesweeper/activePuzzle';

export type ActivePuzzle = BinaryActivePuzzle | MinesweeperActivePuzzle;
