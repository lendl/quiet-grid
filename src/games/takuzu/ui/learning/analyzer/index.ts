import React from 'react';
import type { PuzzleAnalysisAdapter } from '../../../../../app/analysis/types';
import TakuzuAnalysisStepView from './TakuzuAnalysisStepView';
import {
  buildTakuzuAnalysis,
  buildTakuzuAnalysisSource,
  supportsTakuzuAnalysis,
} from './buildAnalysis';

export const takuzuAnalysisAdapter: PuzzleAnalysisAdapter = {
  buildAnalysisSource: buildTakuzuAnalysisSource,
  supportsAnalysis: supportsTakuzuAnalysis,
  buildAnalysis: buildTakuzuAnalysis,
  renderAnalysisStep: (args) => React.createElement(TakuzuAnalysisStepView, args),
};
