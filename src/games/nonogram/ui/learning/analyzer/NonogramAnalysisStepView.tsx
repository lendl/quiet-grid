import React from 'react';
import { StyleSheet, View } from 'react-native';
import type { PuzzleAnalysisRendererArgs } from '../../../../../app/analysis/types';
import NonogramPuzzleGrid from '../../play/components/NonogramPuzzleGrid';
import type { NonogramBoard } from '../../../types';
import type { NonogramAnalysisPayload } from './types';

export default function NonogramAnalysisStepView({
  analysis,
  stepIndex,
  containerWidth,
  containerHeight,
}: PuzzleAnalysisRendererArgs) {
  const styles = React.useMemo(() => makeStyles(), []);
  const nonogramAnalysis = analysis as NonogramAnalysisPayload;
  const step = nonogramAnalysis.steps[stepIndex];

  if (!step) {
    return null;
  }

  return (
    <View style={styles.container}>
      <NonogramPuzzleGrid
        puzzle={nonogramAnalysis.payload.puzzle}
        board={step.afterState as NonogramBoard}
        containerWidth={containerWidth}
        containerHeight={containerHeight}
        interactive={false}
        nextMoveEvidenceCells={step.evidenceCells}
        nextMoveTargetCells={step.targetCells as Array<{ row: number; col: number; value: 0 | 1 }>}
        nextMoveHighlightRows={step.highlightRows}
        nextMoveHighlightCols={step.highlightCols}
      />
    </View>
  );
}

const makeStyles = () => StyleSheet.create({
  container: {
    width: '100%',
    alignItems: 'center',
    justifyContent: 'center',
  },
});
