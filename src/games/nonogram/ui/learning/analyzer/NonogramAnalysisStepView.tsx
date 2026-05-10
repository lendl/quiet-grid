import React from 'react';
import { View } from 'react-native';
import type { PuzzleAnalysisRendererArgs } from '../../../../../app/analysis/types';
import type { NonogramAnalysisPayload } from './types';
import NonogramBoard from '../../play/components/NonogramBoard';

export default function NonogramAnalysisStepView({
  analysis,
  stepIndex,
}: PuzzleAnalysisRendererArgs) {
  const typedAnalysis = analysis as NonogramAnalysisPayload;
  const step = typedAnalysis.steps[stepIndex];
  if (!step) {
    return null;
  }

  return (
    <View style={{ width: '100%', alignItems: 'center' }}>
      <NonogramBoard
        puzzle={typedAnalysis.payload.puzzle}
        cells={step.beforeState}
        interactive={false}
        nextMoveEvidenceCells={step.evidenceCells}
        nextMoveTargetCells={step.targetCells}
        nextMoveHighlightRows={step.highlightRows}
        nextMoveHighlightCols={step.highlightCols}
      />
    </View>
  );
}
