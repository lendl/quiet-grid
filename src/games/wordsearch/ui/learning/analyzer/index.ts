import React from 'react';
import type { PuzzleAnalysisAdapter } from '../../../../../app/analysis/types';
import WordSearchAnalysisStepView from './WordSearchAnalysisStepView';
import {
  buildWordSearchAnalysis,
  buildWordSearchAnalysisSource,
  supportsWordSearchAnalysis,
} from './buildAnalysis';

export const wordSearchAnalysisAdapter: PuzzleAnalysisAdapter = {
  buildAnalysisSource: buildWordSearchAnalysisSource,
  supportsAnalysis: supportsWordSearchAnalysis,
  buildAnalysis: buildWordSearchAnalysis,
  renderAnalysisStep: (args) => React.createElement(WordSearchAnalysisStepView, args),
};
