import React, { useMemo } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import type { PuzzleAnalysisRendererArgs } from '../../../../../app/analysis/types';
import { useTheme } from '../../../../../app/context/ThemeContext';
import type { Theme } from '../../../../../app/theme';
import { makeEmptyBooleanGrid } from '../../../../../app/utils/activeSessionStateStorage';
import { withAlpha } from '../../../../../app/utils/color';
import SudokuPuzzleGrid from '../../play/components/SudokuPuzzleGrid';
import type { SudokuAnalysisPayload } from './types';
import { createEmptySudokuNotes } from '../../../types';

function noop() {}

const makeStyles = (theme: Theme) => StyleSheet.create({
  container: {
    width: '100%',
    gap: 12,
  },
  legendRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    justifyContent: 'center',
  },
  legendPill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
  },
  legendDot: {
    width: 10,
    height: 10,
    borderRadius: 999,
  },
  legendText: {
    fontSize: 12,
    fontWeight: '700',
    color: theme.textSecondary,
  },
});

export default function SudokuAnalysisStepView({
  analysis,
  stepIndex,
  containerWidth,
  containerHeight,
}: PuzzleAnalysisRendererArgs) {
  const sudokuAnalysis = analysis as SudokuAnalysisPayload;
  const step = sudokuAnalysis.steps[stepIndex];
  const { theme, isDark } = useTheme();
  const styles = useMemo(() => makeStyles(theme), [theme]);
  const finishedCells = useMemo(() => makeEmptyBooleanGrid(9), []);
  const selectedCell = step?.placementTargets[0]
    ? { row: step.placementTargets[0].row, col: step.placementTargets[0].col }
    : null;

  if (!step) {
    return null;
  }

  return (
    <View style={[styles.container, { maxWidth: containerWidth }]}>
      <SudokuPuzzleGrid
        board={step.beforeState.board}
        givens={sudokuAnalysis.payload.givens}
        notes={createEmptySudokuNotes(9, 9)}
        finishedCells={finishedCells}
        selectedCell={selectedCell}
        validatedUnitKeys={[]}
        penalizedUnitKeys={[]}
        boardFeedbackEffects={null}
        interactive={false}
        nextMoveEvidenceCells={step.evidenceCells}
        nextMoveTargetCells={[...step.placementTargets, ...step.eliminationTargets]}
        nextMoveHighlightRows={step.highlightRows}
        nextMoveHighlightCols={step.highlightCols}
        nextMoveHighlightBoxes={step.highlightBoxes}
        containerWidth={containerWidth}
        containerHeight={containerHeight}
        onCellPress={noop}
      />

      <View style={styles.legendRow}>
        <View
          style={[
            styles.legendPill,
            {
              backgroundColor: withAlpha(theme.primary, isDark ? 0.16 : 0.08),
              borderColor: withAlpha(theme.primary, isDark ? 0.5 : 0.36),
            },
          ]}
        >
          <View style={[styles.legendDot, { backgroundColor: theme.primary }]} />
          <Text style={styles.legendText}>{sudokuAnalysis.payload.labels.evidence}</Text>
        </View>
        <View
          style={[
            styles.legendPill,
            {
              backgroundColor: withAlpha(theme.success, isDark ? 0.22 : 0.12),
              borderColor: withAlpha(theme.success, isDark ? 0.58 : 0.4),
            },
          ]}
        >
          <View style={[styles.legendDot, { backgroundColor: theme.success }]} />
          <Text style={styles.legendText}>{sudokuAnalysis.payload.labels.place}</Text>
        </View>
        <View
          style={[
            styles.legendPill,
            {
              backgroundColor: withAlpha(theme.difficultyHard, isDark ? 0.22 : 0.12),
              borderColor: withAlpha(theme.difficultyHard, isDark ? 0.58 : 0.4),
            },
          ]}
        >
          <View style={[styles.legendDot, { backgroundColor: theme.difficultyHard }]} />
          <Text style={styles.legendText}>{sudokuAnalysis.payload.labels.eliminate}</Text>
        </View>
      </View>
    </View>
  );
}
