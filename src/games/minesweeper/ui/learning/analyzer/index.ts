import React from 'react';
import type { PuzzleAnalysisAdapter } from '../../../../../app/analysis/types';
import MinesweeperAnalysisStepView from './MinesweeperAnalysisStepView';
import {
  buildMinesweeperAnalysis,
  buildMinesweeperAnalysisSource,
  supportsMinesweeperAnalysis,
} from './buildAnalysis';

export const minesweeperAnalysisAdapter: PuzzleAnalysisAdapter = {
  buildAnalysisSource: buildMinesweeperAnalysisSource,
  supportsAnalysis: supportsMinesweeperAnalysis,
  buildAnalysis: buildMinesweeperAnalysis,
  renderAnalysisStep: (args) => React.createElement(MinesweeperAnalysisStepView, args),
};
