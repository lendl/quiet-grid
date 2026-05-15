export const puzzleTypeIds = ['takuzu', 'minesweeper'] as const;
export type PuzzleTypeId = typeof puzzleTypeIds[number];
export type PuzzleDifficulty = 'easy' | 'medium' | 'hard' | 'expert';

export function isPuzzleTypeId(value: unknown): value is PuzzleTypeId {
  return typeof value === 'string' && puzzleTypeIds.includes(value as PuzzleTypeId);
}
