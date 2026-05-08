import React, { useMemo } from 'react';
import type { PuzzleAnalysisRendererArgs } from '../../../../app/analysis/types';
import { makeEmptyBooleanGrid } from '../../../../app/utils/activePuzzleStateStorage';
import TakuzuPuzzleGrid from '../../components/TakuzuPuzzleGrid';
import type { TakuzuAnalysisPayload } from './types';

function noop() {}

export default function TakuzuAnalysisStepView({
  analysis,
  stepIndex,
  containerWidth,
  containerHeight,
}: PuzzleAnalysisRendererArgs) {
  const takuzuAnalysis = analysis as TakuzuAnalysisPayload;
  const step = takuzuAnalysis.steps[stepIndex];
  const finishedCells = useMemo(
    () => makeEmptyBooleanGrid(takuzuAnalysis.payload.size),
    [takuzuAnalysis.payload.size],
  );

  if (!step) {
    return null;
  }

  return (
    <TakuzuPuzzleGrid
      board={step.afterState}
      isGiven={takuzuAnalysis.payload.isGiven}
      finishedCells={finishedCells}
      lineAnimationEvent={null}
      nextMoveEvidenceCells={step.evidenceCells}
      nextMoveTargetCells={step.targetCells}
      nextMoveHighlightRows={step.highlightRows}
      nextMoveHighlightCols={step.highlightCols}
      size={takuzuAnalysis.payload.size}
      onCellPress={noop}
      containerWidth={containerWidth}
      containerHeight={containerHeight}
    />
  );
}
