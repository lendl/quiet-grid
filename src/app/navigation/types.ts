import type {
  PuzzleDifficulty,
  PuzzleOutcome,
  PuzzleTypeId,
} from '../shell/types';
import type { CompletionVariant } from '../completion/types';
import type { LossReason } from '../loss/types';
import type { SupportInfoKey } from '../content/supportInfo';

export interface PuzzlePlayRouteParams {
  puzzleTypeId: PuzzleTypeId;
  difficulty?: PuzzleDifficulty;
  resume?: boolean;
}

export interface CompletionRouteParams {
  variant: CompletionVariant;
  outcome: PuzzleOutcome;
}

export interface LossRouteParams {
  reason: LossReason;
  puzzleTypeId: PuzzleTypeId;
  difficulty: PuzzleDifficulty;
  elapsedSeconds: number;
}

export interface PuzzleRouteParams {
  puzzleTypeId: PuzzleTypeId;
}

export type TutorialEntryPoint = 'startup' | 'howToPlay';

export type RootStackParamList = {
  Welcome: undefined;
  Home: undefined;
  Settings: undefined;
  Support: undefined;
  SupportInfo: { infoKey: SupportInfoKey };
  PuzzleTypePicker: undefined;
  Puzzle: PuzzleRouteParams;
  PuzzlePlay: PuzzlePlayRouteParams;
  Completion: CompletionRouteParams;
  Loss: LossRouteParams;
  Stats: { puzzleTypeId?: PuzzleTypeId } | undefined;
  HowToPlay: { puzzleTypeId: PuzzleTypeId };
  Tutorial: { puzzleTypeId: PuzzleTypeId; entry: TutorialEntryPoint };
};
