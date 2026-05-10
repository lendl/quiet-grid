import React from 'react';
import type { PuzzleAnalysisAdapter } from '../../../../../app/analysis/types';
import NonogramAnalysisStepView from './NonogramAnalysisStepView';
import {
  buildNonogramAnalysis,
  buildNonogramLossAnalysisSource,
  supportsNonogramLossAnalysis,
} from './buildAnalysis';

export const nonogramAnalysisAdapter: PuzzleAnalysisAdapter = {
  buildLossAnalysisSource: buildNonogramLossAnalysisSource,
  supportsLossAnalysis: supportsNonogramLossAnalysis,
  buildAnalysis: buildNonogramAnalysis,
  renderAnalysisStep: (args) => React.createElement(NonogramAnalysisStepView, args),
};
