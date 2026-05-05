import type { PuzzleDifficulty, PuzzleOutcome, PuzzleTypeId } from '../types';
import type { HowToPlayContent } from './howToPlayContent';
import type { LossContent } from './lossContent';
import type { PuzzlePlayAdapterBase } from './playAdapter';

export interface PuzzleDefinition<TSession = unknown> {
  id: PuzzleTypeId;
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
  createOutcome(session: TSession): PuzzleOutcome;
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
