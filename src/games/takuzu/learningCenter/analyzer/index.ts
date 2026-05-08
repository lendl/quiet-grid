import React from 'react';
import type { PuzzleAnalysisAdapter } from '../../../../app/analysis/types';
import TakuzuAnalysisStepView from './TakuzuAnalysisStepView';
import {
  buildTakuzuAnalysis,
  buildTakuzuLossAnalysisSource,
  supportsTakuzuLossAnalysis,
} from './buildAnalysis';

export const takuzuAnalysisAdapter: PuzzleAnalysisAdapter = {
  buildLossAnalysisSource: buildTakuzuLossAnalysisSource,
  supportsLossAnalysis: supportsTakuzuLossAnalysis,
  buildAnalysis: buildTakuzuAnalysis,
  renderAnalysisStep: (args) => React.createElement(TakuzuAnalysisStepView, args),
};
