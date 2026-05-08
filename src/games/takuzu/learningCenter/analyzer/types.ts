import type { PuzzleAnalysisPayload, PuzzleAnalysisStep, PuzzleLossAnalysisSource } from '../../../../app/analysis/types';
import type { Grid, Puzzle, TakuzuNextMoveRuleKey } from '../../types';

export interface TakuzuLossAnalysisPayload {
  puzzle: Puzzle;
  board: Grid;
}

export interface TakuzuLossAnalysisSource extends PuzzleLossAnalysisSource {
  puzzleTypeId: 'takuzu';
  payload: TakuzuLossAnalysisPayload;
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
