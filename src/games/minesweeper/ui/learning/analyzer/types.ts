import type { PuzzleAnalysisPayload, PuzzleAnalysisStep, PuzzleLossAnalysisSource } from '../../../../../app/analysis/types';
import type { MinesweeperBoard, MinesweeperPuzzle } from '../../../types';
import type { MinesweeperNextMoveCell } from '../../../gameplay/analysis/nextMove';

export interface MinesweeperLossAnalysisPayload {
  puzzle: MinesweeperPuzzle;
  board: MinesweeperBoard;
}

export interface MinesweeperLossAnalysisSource extends PuzzleLossAnalysisSource {
  puzzleTypeId: 'minesweeper';
  payload: MinesweeperLossAnalysisPayload;
}

export interface MinesweeperAnalysisStep extends PuzzleAnalysisStep {
  beforeState: MinesweeperBoard;
  afterState: MinesweeperBoard;
  safeTargetCells: MinesweeperNextMoveCell[];
  mineTargetCells: MinesweeperNextMoveCell[];
}

export interface MinesweeperAnalysisPayload extends PuzzleAnalysisPayload {
  puzzleTypeId: 'minesweeper';
  steps: MinesweeperAnalysisStep[];
  payload: {
    labels: {
      evidence: string;
      safe: string;
      mine: string;
    };
  };
}
