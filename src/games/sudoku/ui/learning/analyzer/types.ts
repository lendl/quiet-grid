import type { PuzzleAnalysisPayload, PuzzleAnalysisStep, PuzzleAnalysisSource } from '../../../../../app/analysis/types';
import type { SudokuHintTargetCell, SudokuNextMoveHint } from '../../../gameplay/analysis/nextMove';
import type { SudokuPlaySession } from '../../../gameplay/playContract';
import type { SudokuBoard } from '../../../types';

export interface SudokuAnalysisSourcePayload {
  puzzle: SudokuPlaySession['puzzle'];
  board: SudokuBoard;
}

export interface SudokuAnalysisSource extends PuzzleAnalysisSource {
  gameId: 'sudoku';
  payload: SudokuAnalysisSourcePayload;
}

export interface SudokuAnalysisState {
  board: SudokuBoard;
}

export interface SudokuAnalysisStep extends PuzzleAnalysisStep {
  ruleKey?: SudokuNextMoveHint['ruleKey'];
  beforeState: SudokuAnalysisState;
  afterState: SudokuAnalysisState;
  placementTargets: SudokuHintTargetCell[];
  eliminationTargets: SudokuHintTargetCell[];
  highlightBoxes: number[];
}

export interface SudokuAnalysisPayload extends PuzzleAnalysisPayload {
  gameId: 'sudoku';
  steps: SudokuAnalysisStep[];
  payload: {
    givens: SudokuBoard;
    labels: {
      evidence: string;
      place: string;
      eliminate: string;
    };
  };
}
