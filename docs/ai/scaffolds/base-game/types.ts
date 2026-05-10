export type GameDifficultyTemplate = 'easy' | 'medium' | 'hard' | 'expert';

export type AnalyzerModeTemplate = 'engine-solution' | 'loss-state';

export interface GamePuzzleTemplate {
  id: string;
  difficulty: GameDifficultyTemplate;
  size: number;
}

export interface ActivePuzzleTemplate {
  puzzle: GamePuzzleTemplate;
  score: number;
  mistakes: number;
  loss: boolean;
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
