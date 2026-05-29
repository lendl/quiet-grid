import React from 'react';
import type { PuzzleAnalysisAdapter } from '../../../../../app/analysis/types';
import SudokuAnalysisStepView from './SudokuAnalysisStepView';
import {
  buildSudokuAnalysis,
  buildSudokuAnalysisSource,
  supportsSudokuAnalysis,
} from './buildAnalysis';

export const sudokuAnalysisAdapter: PuzzleAnalysisAdapter = {
  buildAnalysisSource: buildSudokuAnalysisSource,
  supportsAnalysis: supportsSudokuAnalysis,
  buildAnalysis: buildSudokuAnalysis,
  renderAnalysisStep: (args) => React.createElement(SudokuAnalysisStepView, args),
};
