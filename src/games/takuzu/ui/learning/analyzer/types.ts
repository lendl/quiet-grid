import type { PuzzleAnalysisPayload, PuzzleAnalysisStep, PuzzleAnalysisSource } from '../../../../../app/analysis/types';
import type { Grid, Puzzle, TakuzuNextMoveRuleKey } from '../../../types';

export interface TakuzuAnalysisSourcePayload {
  puzzle: Puzzle;
  board: Grid;
}

export interface TakuzuAnalysisSource extends PuzzleAnalysisSource {
  puzzleTypeId: 'takuzu';
  payload: TakuzuAnalysisSourcePayload;
}

export interface TakuzuAnalysisStep extends PuzzleAnalysisStep {
  ruleKey?: TakuzuNextMoveRuleKey;
  beforeState: Grid;
  afterState: Grid;
}

export interface TakuzuAnalysisPayload extends PuzzleAnalysisPayload {
  puzzleTypeId: 'takuzu';
  steps: TakuzuAnalysisStep[];
  payload: {
    size: number;
    isGiven: boolean[][];
  };
}
