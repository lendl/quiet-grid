import React, { useMemo } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import type { PuzzleAnalysisRendererArgs } from '../../../../../app/analysis/types';
import { useTheme } from '../../../../../app/context/ThemeContext';
import type { Theme } from '../../../../../app/theme';
import { withAlpha } from '../../../../../app/utils/color';
import MinesweeperBoard from '../../play/components/MinesweeperBoard';
import type { MinesweeperAnalysisPayload } from './types';

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

export default function MinesweeperAnalysisStepView({
  analysis,
  stepIndex,
  containerWidth,
}: PuzzleAnalysisRendererArgs) {
  const minesweeperAnalysis = analysis as MinesweeperAnalysisPayload;
  const step = minesweeperAnalysis.steps[stepIndex];
  const { theme, isDark } = useTheme();
  const styles = useMemo(() => makeStyles(theme), [theme]);

  if (!step) {
    return null;
  }

  return (
    <View style={[styles.container, { maxWidth: containerWidth }]}>
      <MinesweeperBoard
        board={step.afterState}
        onReveal={noop}
        onToggleFlag={noop}
        nextMoveEvidenceCells={step.evidenceCells}
        nextMoveSafeTargetCells={step.safeTargetCells}
        nextMoveMineTargetCells={step.mineTargetCells}
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
          <Text style={styles.legendText}>{minesweeperAnalysis.payload.labels.evidence}</Text>
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
          <Text style={styles.legendText}>{minesweeperAnalysis.payload.labels.safe}</Text>
        </View>
        <View
          style={[
            styles.legendPill,
            {
              backgroundColor: withAlpha(theme.difficultyExpert, isDark ? 0.22 : 0.12),
              borderColor: withAlpha(theme.difficultyExpert, isDark ? 0.58 : 0.4),
            },
          ]}
        >
          <View style={[styles.legendDot, { backgroundColor: theme.difficultyExpert }]} />
          <Text style={styles.legendText}>{minesweeperAnalysis.payload.labels.mine}</Text>
        </View>
      </View>
    </View>
  );
}
