import type { GameId } from '../../../games/shared/types';
import type { PuzzleDifficulty, SessionResult } from '../types';
import type { HowToPlayContent } from './howToPlayContent';
import type { LossContent } from './lossContent';
import type { PuzzlePlayAdapterBase } from './playAdapter';

export interface GameDefinition<TSession = unknown> {
  id: GameId;
  title: string;
  shortTitle: string;
  emoji: string;
  tagline: string;
  supports: {
    tutorial: boolean;
    learning: boolean;
    scoring: boolean;
  };
  difficulties: readonly PuzzleDifficulty[];
  createResult(session: TSession): SessionResult;
  playAdapter: PuzzlePlayAdapterBase;
  content: {
    howToPlay: HowToPlayContent;
    loss: LossContent;
    difficultyLabels: Record<PuzzleDifficulty, string>;
    difficultyDescriptions: Record<PuzzleDifficulty, string>;
  };
  screens: {
    tutorial?: unknown;
  };
}
