import type { TakuzuActivePuzzle } from '../../games/takuzu/activePuzzle';
import type { MinesweeperActivePuzzle } from '../../games/minesweeper/activePuzzle';
import type { NonogramActivePuzzle } from '../../games/nonogram/gameplay/activePuzzle';

export type { TakuzuActivePuzzle } from '../../games/takuzu/activePuzzle';
export type { MinesweeperActivePuzzle } from '../../games/minesweeper/activePuzzle';
export type { NonogramActivePuzzle } from '../../games/nonogram/gameplay/activePuzzle';

export type ActivePuzzle = TakuzuActivePuzzle | MinesweeperActivePuzzle | NonogramActivePuzzle;
