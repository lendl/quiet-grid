import type { ActiveSessionTemplate, GamePuzzleTemplate } from '../types';

export interface GamePlaySessionTemplate {
  puzzle: GamePuzzleTemplate;
  score: number;
  mistakes: number;
  loss: boolean;
}

export const initialPlaySessionTemplate: GamePlaySessionTemplate = {
  puzzle: {
    id: '__PUZZLE_ID__',
    difficulty: 'easy',
    size: 0,
  },
  score: 0,
  mistakes: 0,
  loss: false,
};

export function toActiveSessionTemplate(
  session: GamePlaySessionTemplate,
  elapsedSeconds = 0,
): ActiveSessionTemplate {
  return {
    gameId: '__GAME_ID__',
    puzzle: session.puzzle,
    score: session.score,
    mistakes: session.mistakes,
    loss: session.loss,
    elapsedSeconds,
  };
}

export function isSessionLost(session: GamePlaySessionTemplate | ActiveSessionTemplate): boolean {
  return session.loss;
}
