import type { PuzzleAnalysisPayload, PuzzleAnalysisStep, PuzzleAnalysisSource } from '../../../../../app/analysis/types';
import type { MinesweeperBoard, MinesweeperPuzzle } from '../../../types';
import type { MinesweeperNextMoveCell } from '../../../gameplay/analysis/nextMove';

export interface MinesweeperAnalysisSourcePayload {
  puzzle: MinesweeperPuzzle;
  board: MinesweeperBoard;
}

export interface MinesweeperAnalysisSource extends PuzzleAnalysisSource {
  gameId: 'minesweeper';
  payload: MinesweeperAnalysisSourcePayload;
}

export interface MinesweeperAnalysisStep extends PuzzleAnalysisStep {
  beforeState: MinesweeperBoard;
  afterState: MinesweeperBoard;
  safeTargetCells: MinesweeperNextMoveCell[];
  mineTargetCells: MinesweeperNextMoveCell[];
}

export interface MinesweeperAnalysisPayload extends PuzzleAnalysisPayload {
  gameId: 'minesweeper';
  steps: MinesweeperAnalysisStep[];
  payload: {
    labels: {
      evidence: string;
      safe: string;
      mine: string;
    };
  };
}
