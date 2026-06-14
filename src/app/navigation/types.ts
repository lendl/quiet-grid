import type { NavigatorScreenParams } from '@react-navigation/native';
import type { GameId } from '../../games/shared/types';
import type {
  PuzzleDifficulty,
  PuzzleTypeId,
  SessionResult,
} from '../shell/types';
import type { SolvedResultVariant } from '../completion/types';
import type { FailureReason } from '../loss/types';
import type { SupportInfoKey } from '../content/supportInfo';
import type { PuzzleAnalysisPayload, PuzzleAnalysisSource } from '../analysis/types';

export interface PuzzlePlayRouteParams {
  puzzleTypeId: PuzzleTypeId;
  difficulty?: PuzzleDifficulty;
  resume?: boolean;
}

export type TransitionDirection = 'forward' | 'backward' | 'none';

export interface CompletionRouteParams {
  variant: SolvedResultVariant;
  result: SessionResult;
}

export interface LossRouteParams {
  reason: FailureReason;
  gameId: GameId;
  difficulty: PuzzleDifficulty;
  elapsedSeconds: number;
  analysisSource?: PuzzleAnalysisSource;
}

export interface GameRouteParams {
  gameId: GameId;
  initialTab?: keyof GameTabParamList;
  initialDirection?: TransitionDirection;
}

export type MainTabParamList = {
  Games: undefined;
  Stats: { gameId?: GameId } | undefined;
  Settings: undefined;
  Support: undefined;
};

export type GameTabParamList = {
  Play: { gameId: GameId; transitionDirection?: TransitionDirection };
  Stats: { gameId: GameId; transitionDirection?: TransitionDirection };
  Rules: { gameId: GameId; transitionDirection?: TransitionDirection };
};

export interface TechniqueLessonParams {
  gameId: GameId;
  ruleKey: string;
  title: string;
  board: (number | null)[][];
  givens: (number | null)[][];
  finishedCells: boolean[][];
  evidenceCells: Array<{ row: number; col: number }>;
  targetCells: Array<{ row: number; col: number; digit: number; action: 'place' | 'eliminate' }>;
  highlightRows: number[];
  highlightCols: number[];
  highlightBoxes: number[];
}

export type RootStackParamList = {
  Welcome: undefined;
  MainTabs: NavigatorScreenParams<MainTabParamList> | undefined;
  SupportInfo: { infoKey: SupportInfoKey };
  Game: GameRouteParams;
  PuzzlePlay: PuzzlePlayRouteParams;
  Completion: CompletionRouteParams;
  Loss: LossRouteParams;
  Analysis: { analysis: PuzzleAnalysisPayload };
  HowToPlay: { gameId: GameId; isFirstLaunch?: boolean };
  TechniqueLesson: TechniqueLessonParams;
};
