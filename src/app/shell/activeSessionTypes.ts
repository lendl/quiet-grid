import type { TakuzuActivePuzzle } from '../../games/takuzu/activePuzzle';
import type { MinesweeperActivePuzzle } from '../../games/minesweeper/activePuzzle';

export type { TakuzuActivePuzzle } from '../../games/takuzu/activePuzzle';
export type { MinesweeperActivePuzzle } from '../../games/minesweeper/activePuzzle';

export type ActivePuzzle = TakuzuActivePuzzle | MinesweeperActivePuzzle;
