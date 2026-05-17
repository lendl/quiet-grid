import type { NavigatorScreenParams } from '@react-navigation/native';
import type {
  PuzzleDifficulty,
  PuzzleOutcome,
  PuzzleTypeId,
} from '../shell/types';
import type { CompletionVariant } from '../completion/types';
import type { LossReason } from '../loss/types';
import type { SupportInfoKey } from '../content/supportInfo';
import type { PuzzleAnalysisPayload, PuzzleAnalysisSource } from '../analysis/types';

export interface PuzzlePlayRouteParams {
  puzzleTypeId: PuzzleTypeId;
  difficulty?: PuzzleDifficulty;
  resume?: boolean;
}

export type TransitionDirection = 'forward' | 'backward' | 'none';

export interface CompletionRouteParams {
  variant: CompletionVariant;
  outcome: PuzzleOutcome;
}

export interface LossRouteParams {
  reason: LossReason;
  puzzleTypeId: PuzzleTypeId;
  difficulty: PuzzleDifficulty;
  elapsedSeconds: number;
  analysisSource?: PuzzleAnalysisSource;
}

export interface PuzzleRouteParams {
  puzzleTypeId: PuzzleTypeId;
  initialTab?: keyof PuzzleTabParamList;
  initialDirection?: TransitionDirection;
}

export type TutorialEntryPoint = 'startup' | 'howToPlay';

export type MainTabParamList = {
  Games: undefined;
  Stats: { puzzleTypeId?: PuzzleTypeId } | undefined;
  Settings: undefined;
  Support: undefined;
};

export type PuzzleTabParamList = {
  Game: { puzzleTypeId: PuzzleTypeId; transitionDirection?: TransitionDirection };
  Stats: { puzzleTypeId: PuzzleTypeId; transitionDirection?: TransitionDirection };
  Rules: { puzzleTypeId: PuzzleTypeId; transitionDirection?: TransitionDirection };
  Tutorial: { puzzleTypeId: PuzzleTypeId; transitionDirection?: TransitionDirection };
};

export type RootStackParamList = {
  Welcome: undefined;
  MainTabs: NavigatorScreenParams<MainTabParamList> | undefined;
  SupportInfo: { infoKey: SupportInfoKey };
  Puzzle: PuzzleRouteParams;
  PuzzlePlay: PuzzlePlayRouteParams;
  Completion: CompletionRouteParams;
  Loss: LossRouteParams;
  Analysis: { analysis: PuzzleAnalysisPayload };
  HowToPlay: { puzzleTypeId: PuzzleTypeId };
  Tutorial: { puzzleTypeId: PuzzleTypeId; entry: TutorialEntryPoint };
};
