import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Animated,
  Easing,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useLanguage } from '../../../../../app/context/LanguageContext';
import { useTheme } from '../../../../../app/context/ThemeContext';
import {
  createBoundedGridLayout,
  getGridCellRect,
} from '../../../../../app/shell/boardLayout';
import { createSharedBoardRenderTokens } from '../../../../../app/shell/renderTokens';
import { withAlpha } from '../../../../../app/utils/color';
import type {
  Grid,
  TakuzuNextMoveCell,
  TakuzuNextMoveTargetCell,
} from '../../../types';

const FRAME_PADDING = 6;
const GRID_PADDING = 1;
const GAP = 1;
const SPIN_DURATION_MS = 420;
const SHELL_INSET = 2;
const SHELL_RADIUS = 8;
const SHELL_INNER_RADIUS = 6;
const BOARD_INSET = 1;
const BOARD_RADIUS = 6;
const BOARD_SURFACE_RADIUS = 4;
const CELL_RADIUS = 4;
const CELL_FACE_INSET = 1;

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
  nextMoveEvidenceCells?: TakuzuNextMoveCell[];
  nextMoveTargetCells?: TakuzuNextMoveTargetCell[];
  nextMoveHighlightRows?: number[];
  nextMoveHighlightCols?: number[];
  size: number;
  onCellPress: (row: number, col: number) => void;
  containerWidth: number;
  containerHeight: number;
}

function buildSpinningKeys(event: LineAnimationEvent | null, size: number): Set<string> {
  const keys = new Set<string>();
  if (!event) {
    return keys;
  }

  event.rows.forEach((rowIndex) => {
    for (let colIndex = 0; colIndex < size; colIndex += 1) {
      keys.add(`${rowIndex}:${colIndex}`);
    }
  });

  event.cols.forEach((colIndex) => {
    for (let rowIndex = 0; rowIndex < size; rowIndex += 1) {
      keys.add(`${rowIndex}:${colIndex}`);
    }
  });

  return keys;
}

function buildCellKeySet(cells: ReadonlyArray<{ row: number; col: number }>): Set<string> {
  return new Set(cells.map(({ row, col }) => `${row}:${col}`));
}

function TakuzuPuzzleGrid({
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
  const { resolvedLanguage } = useLanguage();
  const { theme, isDark } = useTheme();
  const tokens = useMemo(() => createSharedBoardRenderTokens(theme, isDark), [theme, isDark]);
  const layout = useMemo(() => createBoundedGridLayout({
    containerWidth,
    containerHeight,
    rows: size,
    cols: size,
    gap: GAP,
    padding: FRAME_PADDING + GRID_PADDING,
    minCellSize: 1,
    maxCellSize: Number.MAX_SAFE_INTEGER,
  }), [containerHeight, containerWidth, size]);
  const rotation = useRef(new Animated.Value(0)).current;
  const spinTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const handledLineAnimationIdRef = useRef<number | null>(null);
  const [activeSpin, setActiveSpin] = useState<{
    id: number;
    keys: string[];
  } | null>(null);
  const rotate = useMemo(() => rotation.interpolate({
    inputRange: [0, 1],
    outputRange: ['0deg', '360deg'],
  }), [rotation]);

  useEffect(() => {
    if (!lineAnimationEvent) {
      handledLineAnimationIdRef.current = null;
      return undefined;
    }
    if (handledLineAnimationIdRef.current === lineAnimationEvent.id) {
      return undefined;
    }

    const keys = Array.from(buildSpinningKeys(lineAnimationEvent, size));
    if (keys.length === 0) {
      return undefined;
    }

    handledLineAnimationIdRef.current = lineAnimationEvent.id;
    setActiveSpin({
      id: lineAnimationEvent.id,
      keys,
    });
    if (spinTimeoutRef.current) {
      clearTimeout(spinTimeoutRef.current);
      spinTimeoutRef.current = null;
    }
    rotation.stopAnimation();
    rotation.setValue(0);
    const animation = Animated.timing(rotation, {
      toValue: 1,
      duration: SPIN_DURATION_MS,
      easing: Easing.inOut(Easing.ease),
      useNativeDriver: true,
    });

    animation.start();
    spinTimeoutRef.current = setTimeout(() => {
      setActiveSpin((current) => (current?.id === lineAnimationEvent.id ? null : current));
      rotation.stopAnimation();
      rotation.setValue(0);
      spinTimeoutRef.current = null;
    }, SPIN_DURATION_MS);

    return () => {
      animation.stop();
      if (spinTimeoutRef.current) {
        clearTimeout(spinTimeoutRef.current);
        spinTimeoutRef.current = null;
      }
    };
  }, [lineAnimationEvent, rotation, size]);

  const activeSpinKeys = useMemo(
    () => new Set(activeSpin?.keys ?? []),
    [activeSpin],
  );
  const evidenceKeys = useMemo(() => buildCellKeySet(nextMoveEvidenceCells), [nextMoveEvidenceCells]);
  const targetKeys = useMemo(() => buildCellKeySet(nextMoveTargetCells), [nextMoveTargetCells]);
  const highlightedRows = useMemo(() => new Set(nextMoveHighlightRows), [nextMoveHighlightRows]);
  const highlightedCols = useMemo(() => new Set(nextMoveHighlightCols), [nextMoveHighlightCols]);

  return (
    <View style={styles.boardArea}>
      <View
        style={[
          styles.boardFrame,
          {
            width: layout.frameWidth,
            height: layout.frameHeight,
          },
        ]}
      >
        <View pointerEvents="none" style={StyleSheet.absoluteFill}>
          <View
            style={[
              styles.fieldKitShellBorder,
              { borderColor: withAlpha(theme.primaryLight, isDark ? 0.44 : 0.32) },
            ]}
          />
          <View
            style={[
              styles.fieldKitShellFill,
              { backgroundColor: tokens.panelFill },
            ]}
          />
          <View
            style={[
              styles.fieldKitBoardBorder,
              { borderColor: withAlpha(theme.primary, isDark ? 0.3 : 0.22) },
            ]}
          />
          <View
            style={[
              styles.fieldKitBoardSurface,
              { backgroundColor: withAlpha(theme.panelSurfaceElevated, isDark ? 0.34 : 0.48) },
            ]}
          />
          {board.map((row, rowIndex) => row.map((value, colIndex) => {
            const key = `${rowIndex}:${colIndex}`;
            const rect = getGridCellRect(layout, rowIndex, colIndex);
            const locked = isGiven[rowIndex][colIndex] || finishedCells[rowIndex][colIndex];
            const showTarget = targetKeys.has(key);
            const showEvidence = evidenceKeys.has(key);
            const showHighlight = highlightedRows.has(rowIndex) || highlightedCols.has(colIndex);

            return (
              <View
                key={key}
                style={[
                  styles.cellFrame,
                  {
                    left: rect.x,
                    top: rect.y,
                    width: rect.width,
                    height: rect.height,
                    borderColor: showTarget
                      ? withAlpha(theme.primaryLight, isDark ? 0.92 : 0.72)
                      : showEvidence
                        ? withAlpha(theme.primary, isDark ? 0.64 : 0.42)
                        : withAlpha(theme.border, isDark ? 0.84 : 0.62),
                    backgroundColor: locked ? tokens.cellSunkenFill : tokens.cellRaisedFill,
                  },
                ]}
              >
                <View
                  style={[
                    styles.cellFace,
                    {
                      backgroundColor: showTarget
                        ? withAlpha(theme.primary, isDark ? 0.26 : 0.18)
                        : showEvidence
                          ? withAlpha(theme.primary, isDark ? 0.14 : 0.1)
                          : showHighlight
                            ? withAlpha(theme.primary, isDark ? 0.06 : 0.04)
                            : locked
                              ? tokens.cellSunkenFill
                              : tokens.cellRaisedFill,
                    },
                  ]}
                />
                {showTarget ? (
                  <View style={[styles.infoBadge, { backgroundColor: theme.primary }]}>
                    <Text style={[styles.infoBadgeText, { color: tokens.onPrimary }]}>i</Text>
                  </View>
                ) : null}
                {value !== null && !activeSpinKeys.has(key) ? (
                  <Text
                    style={[
                      styles.cellValue,
                      {
                        color: withAlpha(theme.text, locked ? (isDark ? 0.92 : 0.84) : (isDark ? 0.98 : 0.9)),
                        fontWeight: locked ? '800' : '700',
                        fontSize: layout.cellSize * 0.46,
                      },
                    ]}
                  >
                    {String(value)}
                  </Text>
                ) : null}
              </View>
            );
          }))}
        </View>
        <View pointerEvents="box-none" style={StyleSheet.absoluteFill}>
          {board.map((row, rowIndex) => row.map((value, colIndex) => {
            const key = `${rowIndex}:${colIndex}`;
            const rect = getGridCellRect(layout, rowIndex, colIndex);
            const locked = isGiven[rowIndex][colIndex] || finishedCells[rowIndex][colIndex];
            const showSpin = activeSpinKeys.has(key) && value !== null;

            return (
              <React.Fragment key={key}>
                <Pressable
                  disabled={locked}
                  style={[
                    styles.hitCell,
                    {
                      left: rect.x,
                      top: rect.y,
                      width: rect.width,
                      height: rect.height,
                    },
                  ]}
                  accessibilityRole="button"
                  accessibilityLabel={resolvedLanguage === 'nl'
                    ? `Cel ${rowIndex + 1}-${colIndex + 1}`
                    : `Cell ${rowIndex + 1}-${colIndex + 1}`}
                  onPress={() => onCellPress(rowIndex, colIndex)}
                />
                {showSpin ? (
                  <Animated.View
                    pointerEvents="none"
                    style={[
                      styles.spinOverlay,
                      {
                        left: rect.x,
                        top: rect.y,
                        width: rect.width,
                        height: rect.height,
                        transform: [{ rotate }],
                      },
                    ]}
                  >
                    <Animated.Text
                      style={{
                        color: withAlpha(theme.text, isDark ? 0.98 : 0.88),
                        fontWeight: locked ? '800' : '700',
                        fontSize: layout.cellSize * 0.46,
                        fontFamily: 'monospace',
                      }}
                    >
                      {String(value)}
                    </Animated.Text>
                  </Animated.View>
                ) : null}
              </React.Fragment>
            );
          }))}
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  boardArea: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  boardFrame: {
    position: 'relative',
  },
  fieldKitShellBorder: {
    ...StyleSheet.absoluteFillObject,
    borderWidth: 1,
    borderRadius: SHELL_RADIUS,
  },
  fieldKitShellFill: {
    ...StyleSheet.absoluteFillObject,
    top: SHELL_INSET,
    right: SHELL_INSET,
    bottom: SHELL_INSET,
    left: SHELL_INSET,
    borderRadius: SHELL_INNER_RADIUS,
  },
  fieldKitBoardBorder: {
    ...StyleSheet.absoluteFillObject,
    borderWidth: 1,
    borderRadius: BOARD_RADIUS,
  },
  fieldKitBoardSurface: {
    ...StyleSheet.absoluteFillObject,
    top: BOARD_INSET,
    right: BOARD_INSET,
    bottom: BOARD_INSET,
    left: BOARD_INSET,
    borderRadius: BOARD_SURFACE_RADIUS,
  },
  cellFrame: {
    position: 'absolute',
    borderWidth: 1,
    borderRadius: CELL_RADIUS,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cellFace: {
    ...StyleSheet.absoluteFillObject,
    margin: CELL_FACE_INSET,
    borderRadius: CELL_RADIUS - CELL_FACE_INSET,
  },
  cellValue: {
    fontFamily: 'monospace',
    textAlign: 'center',
  },
  infoBadge: {
    position: 'absolute',
    top: 2,
    right: 2,
    minWidth: 14,
    height: 14,
    paddingHorizontal: 3,
    borderRadius: 7,
    alignItems: 'center',
    justifyContent: 'center',
  },
  infoBadgeText: {
    fontSize: 9,
    fontWeight: '900',
    lineHeight: 10,
  },
  hitCell: {
    position: 'absolute',
  },
  spinOverlay: {
    position: 'absolute',
    alignItems: 'center',
    justifyContent: 'center',
  },
});

export default React.memo(TakuzuPuzzleGrid);
