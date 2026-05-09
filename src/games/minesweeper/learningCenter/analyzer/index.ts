import React from 'react';
import type { PuzzleAnalysisAdapter } from '../../../../app/analysis/types';
import MinesweeperAnalysisStepView from './MinesweeperAnalysisStepView';
import {
  buildMinesweeperAnalysis,
  buildMinesweeperLossAnalysisSource,
  supportsMinesweeperLossAnalysis,
} from './buildAnalysis';

export const minesweeperAnalysisAdapter: PuzzleAnalysisAdapter = {
  buildLossAnalysisSource: buildMinesweeperLossAnalysisSource,
  supportsLossAnalysis: supportsMinesweeperLossAnalysis,
  buildAnalysis: buildMinesweeperAnalysis,
  renderAnalysisStep: (args) => React.createElement(MinesweeperAnalysisStepView, args),
};
