import type { ActivePuzzleTemplate } from '../types';

export interface GameSessionTemplate extends ActivePuzzleTemplate {
  startedAt: number;
  lastUpdatedAt: number;
}

export const initialSessionTemplate: GameSessionTemplate = {
  puzzle: {
    id: '__PUZZLE_ID__',
    difficulty: 'easy',
    size: 0,
  },
  score: 0,
  mistakes: 0,
  loss: false,
  startedAt: 0,
  lastUpdatedAt: 0,
};

export function isSessionLost(session: GameSessionTemplate): boolean {
  return session.loss;
}
