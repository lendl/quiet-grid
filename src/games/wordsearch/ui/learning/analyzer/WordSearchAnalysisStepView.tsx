import React, { useMemo } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import type { PuzzleAnalysisRendererArgs } from '../../../../../app/analysis/types';
import { useTheme } from '../../../../../app/context/ThemeContext';
import type { Theme } from '../../../../../app/theme';
import { withAlpha } from '../../../../../app/utils/color';
import WordSearchPuzzleGrid from '../../play/components/WordSearchPuzzleGrid';
import type { WordSearchAnalysisPayload } from './types';

export default function WordSearchAnalysisStepView({
  analysis,
  stepIndex,
  containerWidth,
  containerHeight,
}: PuzzleAnalysisRendererArgs) {
  const { theme } = useTheme();
  const styles = useMemo(() => makeStyles(theme), [theme]);
  const wordSearchAnalysis = analysis as WordSearchAnalysisPayload;
  const step = wordSearchAnalysis.steps[stepIndex];

  if (!step) {
    return null;
  }

  return (
    <View style={styles.container}>
      <WordSearchPuzzleGrid
        puzzle={wordSearchAnalysis.payload.puzzle}
        foundWordIds={step.afterState.foundWordIds}
        tempSelection={null}
        containerWidth={containerWidth}
        containerHeight={containerHeight}
        interactive={false}
        allowDrag={false}
        nextMoveEvidenceCells={step.evidenceCells}
        nextMoveTargetCells={step.targetCells}
      />
      <View style={styles.explanationCard}>
        <Text style={styles.explanationTitle}>{step.title}</Text>
        <Text style={styles.explanationBody}>{step.body}</Text>
      </View>
    </View>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  container: {
    width: '100%',
    gap: 10,
    alignItems: 'center',
  },
  explanationCard: {
    width: '100%',
    borderRadius: 14,
    borderWidth: 1,
    borderColor: withAlpha(theme.primary, 0.22),
    backgroundColor: withAlpha(theme.surfaceElevated, 0.94),
    paddingHorizontal: 12,
    paddingVertical: 10,
    gap: 4,
  },
  explanationTitle: {
    color: theme.text,
    fontSize: 13,
    lineHeight: 18,
    fontWeight: '800',
  },
  explanationBody: {
    color: theme.textSecondary,
    fontSize: 12,
    lineHeight: 17,
  },
});
