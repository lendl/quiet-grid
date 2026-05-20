import type { TakuzuActiveSession } from '../../games/takuzu/activePuzzle';
import type { MinesweeperActiveSession } from '../../games/minesweeper/activePuzzle';

export type { TakuzuActiveSession } from '../../games/takuzu/activePuzzle';
export type { MinesweeperActiveSession } from '../../games/minesweeper/activePuzzle';

export type ActiveSession = TakuzuActiveSession | MinesweeperActiveSession;
