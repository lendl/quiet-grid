export const gameIds = ['takuzu', 'minesweeper', 'nonogram', 'sudoku'] as const;
export type GameId = typeof gameIds[number];
export type PuzzleDifficulty = 'easy' | 'medium' | 'hard' | 'expert';

export function isGameId(value: unknown): value is GameId {
  return typeof value === 'string' && gameIds.includes(value as GameId);
}
