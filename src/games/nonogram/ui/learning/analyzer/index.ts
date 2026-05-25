import React from 'react';
import type { PuzzleAnalysisAdapter } from '../../../../../app/analysis/types';
import NonogramAnalysisStepView from './NonogramAnalysisStepView';
import {
  buildNonogramAnalysis,
  buildNonogramAnalysisSource,
  supportsNonogramAnalysis,
} from '../../../gameplay/analysis';

export const nonogramAnalysisAdapter: PuzzleAnalysisAdapter = {
  buildAnalysisSource: buildNonogramAnalysisSource,
  supportsAnalysis: supportsNonogramAnalysis,
  buildAnalysis: buildNonogramAnalysis,
  renderAnalysisStep: (args) => React.createElement(NonogramAnalysisStepView, args),
};
