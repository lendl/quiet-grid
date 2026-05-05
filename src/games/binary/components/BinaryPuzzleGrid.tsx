import React, { useMemo } from 'react';
import { View, StyleSheet } from 'react-native';
import type { BinaryNextMoveCell, BinaryNextMoveTargetCell, Grid } from '../types';
import BinaryGridCell from './BinaryGridCell';
import { useTheme } from '../../../app/context/ThemeContext';
import type { Theme } from '../../../app/theme';
import { withAlpha } from '../../../app/utils/color';

const GAP = 1;
const FRAME_PADDING = 6;

interface LineAnimationEvent {
  id: number;
  rows: number[];
  cols: number[];
}

interface PuzzleGridProps {
  board: Grid;
  isGiven: boolean[][];
  finishedCells: boolean[][];
  lineAnimationEvent: LineAnimationEvent | null;
  nextMoveEvidenceCells?: BinaryNextMoveCell[];
  nextMoveTargetCells?: BinaryNextMoveTargetCell[];
  nextMoveHighlightRows?: number[];
  nextMoveHighlightCols?: number[];
  size: number;
  onCellPress: (row: number, col: number) => void;
  containerWidth: number;
  containerHeight: number;
}

function BinaryPuzzleGrid({
  board,
  isGiven,
  finishedCells,
  lineAnimationEvent,
  nextMoveEvidenceCells = [],
  nextMoveTargetCells = [],
  nextMoveHighlightRows = [],
  nextMoveHighlightCols = [],
  size,
  onCellPress,
  containerWidth,
  containerHeight,
}: PuzzleGridProps) {
  const { theme, isDark } = useTheme();

  const cellSize = useMemo(() => {
    const horizontalCellSize = Math.floor(
      (containerWidth - FRAME_PADDING * 2 - (size + 1) * GAP) / size,
    );
    const verticalCellSize = Math.floor(
      (containerHeight - FRAME_PADDING * 2 - (size + 1) * GAP) / size,
    );
    return Math.max(0, Math.min(horizontalCellSize, verticalCellSize));
  }, [containerHeight, containerWidth, size]);
  const s = useMemo(() => makeStyles(theme, isDark), [theme, isDark]);
  const spinningCellKeys = useMemo(() => {
    const keys = new Set<string>();
    if (!lineAnimationEvent) return keys;

    lineAnimationEvent.rows.forEach((rowIndex) => {
      for (let colIndex = 0; colIndex < size; colIndex += 1) {
        keys.add(`${rowIndex}:${colIndex}`);
      }
    });

    lineAnimationEvent.cols.forEach((colIndex) => {
      for (let rowIndex = 0; rowIndex < size; rowIndex += 1) {
        keys.add(`${rowIndex}:${colIndex}`);
      }
    });

    return keys;
  }, [lineAnimationEvent, size]);
  const nextMoveEvidenceKeys = useMemo(() => new Set(
    nextMoveEvidenceCells.map(({ row, col }) => `${row}:${col}`),
  ), [nextMoveEvidenceCells]);
  const nextMoveTargetKeys = useMemo(() => new Set(
    nextMoveTargetCells.map(({ row, col }) => `${row}:${col}`),
  ), [nextMoveTargetCells]);
  const highlightedRows = useMemo(() => new Set(nextMoveHighlightRows), [nextMoveHighlightRows]);
  const highlightedCols = useMemo(() => new Set(nextMoveHighlightCols), [nextMoveHighlightCols]);

  return (
    <View style={s.boardShell}>
      <View style={s.grid}>
        {board.map((row, r) => (
          <View key={r} style={s.row}>
            {row.map((value, c) => (
              <BinaryGridCell
                key={c}
                value={value}
                given={isGiven[r][c]}
                finished={finishedCells[r][c]}
                cellSize={cellSize}
                spinEventId={spinningCellKeys.has(`${r}:${c}`) ? lineAnimationEvent?.id ?? null : null}
                nextMoveLineHighlighted={highlightedRows.has(r) || highlightedCols.has(c)}
                nextMoveEvidence={nextMoveEvidenceKeys.has(`${r}:${c}`)}
                nextMoveTarget={nextMoveTargetKeys.has(`${r}:${c}`)}
                onPress={() => onCellPress(r, c)}
              />
            ))}
          </View>
        ))}
      </View>
    </View>
  );
}

const makeStyles = (theme: Theme, isDark: boolean) => StyleSheet.create({
  boardShell: {
    padding: FRAME_PADDING,
    borderRadius: 22,
    backgroundColor: withAlpha(theme.surfaceElevated, isDark ? 0.92 : 0.98),
    borderWidth: 1,
    borderColor: withAlpha(theme.border, isDark ? 0.88 : 0.82),
  },
  grid: {
    overflow: 'hidden',
    borderRadius: 16,
    backgroundColor: withAlpha(theme.text, isDark ? 0.05 : 0.08),
    gap: GAP,
    padding: 1,
  },
  row: {
    flexDirection: 'row',
    gap: GAP,
  },
});

export default React.memo(BinaryPuzzleGrid);
