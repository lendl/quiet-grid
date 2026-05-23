import React, { useMemo } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import type { PuzzleAnalysisRendererArgs } from '../../../../../app/analysis/types';
import ZoomableBoardSurface from '../../../../../app/components/ZoomableBoardSurface';
import { createBoundedGridLayout } from '../../../../../app/shell/boardLayout';
import { useTheme } from '../../../../../app/context/ThemeContext';
import type { Theme } from '../../../../../app/theme';
import { withAlpha } from '../../../../../app/utils/color';
import MinesweeperBoard from '../../play/components/MinesweeperBoard';
import {
  MINESWEEPER_BOARD_CELL_GAP,
  MINESWEEPER_FRAME_BORDER_WIDTH,
  MINESWEEPER_FRAME_PADDING,
} from '../../play/components/boardStyles';
import type { MinesweeperAnalysisPayload } from './types';

function noop() {}
const ANALYZER_TARGET_CELL_SIZE = 38;

const makeStyles = (theme: Theme) => StyleSheet.create({
  container: {
    width: '100%',
    gap: 12,
  },
  boardViewport: {
    width: '100%',
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
  containerHeight,
}: PuzzleAnalysisRendererArgs) {
  const minesweeperAnalysis = analysis as MinesweeperAnalysisPayload;
  const step = minesweeperAnalysis.steps[stepIndex];
  const { theme, isDark } = useTheme();
  const styles = useMemo(() => makeStyles(theme), [theme]);

  if (!step) {
    return null;
  }

  const focusedCells = step.safeTargetCells.length > 0
    ? step.safeTargetCells
    : step.mineTargetCells.length > 0
      ? step.mineTargetCells
      : step.targetCells;
  const focusLayout = useMemo(() => createBoundedGridLayout({
    containerWidth,
    containerHeight,
    rows: step.afterState.rows,
    cols: step.afterState.cols,
    gap: MINESWEEPER_BOARD_CELL_GAP,
    padding: MINESWEEPER_FRAME_PADDING,
    borderWidth: MINESWEEPER_FRAME_BORDER_WIDTH,
    minCellSize: 1,
    maxCellSize: Number.MAX_SAFE_INTEGER,
  }), [containerHeight, containerWidth, step.afterState.cols, step.afterState.rows]);
  const autoFocus = useMemo(() => {
    if (focusedCells.length === 0) {
      return undefined;
    }

    const averageRow = focusedCells.reduce((sum, cell) => sum + cell.row, 0) / focusedCells.length;
    const averageCol = focusedCells.reduce((sum, cell) => sum + cell.col, 0) / focusedCells.length;

    return {
      key: step.key,
      xRatio: (averageCol + 0.5) / step.afterState.cols,
      yRatio: (averageRow + 0.5) / step.afterState.rows,
      scale: Math.min(2.5, Math.max(1.35, ANALYZER_TARGET_CELL_SIZE / Math.max(focusLayout.cellSize, 1))),
    };
  }, [focusLayout.cellSize, focusedCells, step.afterState.cols, step.afterState.rows, step.key]);

  return (
    <View style={[styles.container, { maxWidth: containerWidth }]}>
      <View style={[styles.boardViewport, { height: containerHeight }]}>
        <ZoomableBoardSurface autoFocus={autoFocus}>
          <MinesweeperBoard
            board={step.afterState}
            containerWidth={containerWidth}
            containerHeight={containerHeight}
            onReveal={noop}
            onToggleFlag={noop}
            nextMoveEvidenceCells={step.evidenceCells}
            nextMoveSafeTargetCells={step.safeTargetCells}
            nextMoveMineTargetCells={step.mineTargetCells}
            focusedCells={focusedCells}
          />
        </ZoomableBoardSurface>
      </View>
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
