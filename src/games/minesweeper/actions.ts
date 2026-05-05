import {
  revealMinesweeperCell,
  toggleMinesweeperFlag,
} from './rules';
import type { MinesweeperPlaySession } from './playContract';

export type MinesweeperAction =
  | { type: 'reveal-cell'; row: number; col: number }
  | { type: 'toggle-flag'; row: number; col: number };

export type MinesweeperActionEffect =
  | { type: 'won' }
  | { type: 'lost' };

export interface MinesweeperActionResult {
  session: MinesweeperPlaySession;
  changed: boolean;
  effects: MinesweeperActionEffect[];
}

export function applyMinesweeperAction(
  session: MinesweeperPlaySession,
  action: MinesweeperAction,
): MinesweeperActionResult {
  const board = action.type === 'reveal-cell'
    ? revealMinesweeperCell(session.board, action.row, action.col)
    : toggleMinesweeperFlag(session.board, action.row, action.col);

  if (board === session.board) {
    return { session, changed: false, effects: [] };
  }

  const effects: MinesweeperActionEffect[] = [];
  if (board.status === 'won') effects.push({ type: 'won' });
  if (board.status === 'lost') effects.push({ type: 'lost' });

  return {
    session: { ...session, board },
    changed: true,
    effects,
  };
}
