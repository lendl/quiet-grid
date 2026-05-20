export type GameIdTemplate = '__GAME_ID__';

export type GameDifficultyTemplate = 'easy' | 'medium' | 'hard' | 'expert';

export type AnalyzerModeTemplate = 'engine-solution' | 'loss-state';

export interface GamePuzzleTemplate {
  id: string;
  difficulty: GameDifficultyTemplate;
  size: number;
}

export interface ActiveSessionTemplate {
  gameId: GameIdTemplate;
  puzzle: GamePuzzleTemplate;
  score: number;
  mistakes: number;
  loss: boolean;
  elapsedSeconds: number;
}

export interface SessionResultTemplate {
  gameId: GameIdTemplate;
  difficulty: GameDifficultyTemplate;
  status: 'solved' | 'failed';
  score: number;
  accuracy: number;
  elapsedSeconds: number;
  streak: number;
}

export interface CanonicalMoveTemplate {
  key: string;
  title: string;
  summary: string;
}

export interface SupportActionTemplate {
  key: string;
  title: string;
  optional: true;
}
