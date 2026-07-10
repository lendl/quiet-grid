import React, { useCallback, useMemo, useRef } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Gesture, GestureDetector } from 'react-native-gesture-handler';
import { useTheme } from '../../../../../app/context/ThemeContext';
import { createBoundedGridLayout, getGridCellRect } from '../../../../../app/shell/boardLayout';
import { createSharedBoardRenderTokens } from '../../../../../app/shell/renderTokens';
import { withAlpha } from '../../../../../app/utils/color';
import type { WordSearchCellRef, WordSearchPuzzle, WordSearchSelection } from '../../../types';

const FRAME_PADDING = 8;
const GRID_PADDING = 4;
const GAP = 3;
const SHELL_INSET = 2;
const SHELL_RADIUS = 10;
const SHELL_INNER_RADIUS = 8;
const BOARD_INSET = 1;
const BOARD_RADIUS = 8;
const BOARD_SURFACE_RADIUS = 6;
const CELL_RADIUS = 4;
const CELL_FACE_INSET = 1;

interface WordSearchPuzzleGridProps {
  puzzle: WordSearchPuzzle;
  foundWordIds: readonly string[];
  tempSelection: WordSearchSelection | null;
  hiddenWordTargetCell?: WordSearchCellRef | null;
  containerWidth: number;
  containerHeight: number;
  interactive?: boolean;
  allowDrag?: boolean;
  nextMoveEvidenceCells?: readonly WordSearchCellRef[];
  nextMoveTargetCells?: readonly WordSearchCellRef[];
  onCellTap?: (row: number, col: number) => void;
  onSelectionStart?: (row: number, col: number) => void;
  onSelectionMove?: (row: number, col: number) => void;
  onSelectionEnd?: () => void;
}

function buildCellKey(row: number, col: number): string {
  return `${row}:${col}`;
}

function getCellAtPoint(
  layout: ReturnType<typeof createBoundedGridLayout>,
  rows: number,
  cols: number,
  x: number,
  y: number,
): WordSearchCellRef | null {
  const gridX = layout.borderWidth + layout.padding;
  const gridY = layout.borderWidth + layout.padding;
  const offsetX = x - gridX;
  const offsetY = y - gridY;

  if (offsetX < 0 || offsetY < 0) {
    return null;
  }

  const stride = layout.cellSize + layout.gap;
  const col = Math.floor(offsetX / stride);
  const row = Math.floor(offsetY / stride);

  if (row < 0 || row >= rows || col < 0 || col >= cols) {
    return null;
  }

  const rect = getGridCellRect(layout, row, col);
  if (x < rect.x || x > rect.x + rect.width || y < rect.y || y > rect.y + rect.height) {
    return null;
  }

  return { row, col };
}

export default function WordSearchPuzzleGrid({
  puzzle,
  foundWordIds,
  tempSelection,
  hiddenWordTargetCell = null,
  containerWidth,
  containerHeight,
  interactive = true,
  allowDrag = true,
  nextMoveEvidenceCells = [],
  nextMoveTargetCells = [],
  onCellTap,
  onSelectionStart,
  onSelectionMove,
  onSelectionEnd,
}: WordSearchPuzzleGridProps) {
  const { theme, isDark } = useTheme();
  const tokens = useMemo(() => createSharedBoardRenderTokens(theme, isDark), [theme, isDark]);
  const layout = useMemo(() => createBoundedGridLayout({
    containerWidth,
    containerHeight,
    rows: puzzle.rows,
    cols: puzzle.cols,
    gap: GAP,
    padding: FRAME_PADDING + GRID_PADDING,
    minCellSize: 18,
    maxCellSize: 90,
  }), [containerHeight, containerWidth, puzzle.cols, puzzle.rows]);
  const dragCellKeyRef = useRef<string | null>(null);
  const foundCellKeys = useMemo(() => {
    const foundIds = new Set(foundWordIds);
    const keys = new Set<string>();
    puzzle.words.forEach((word) => {
      if (!foundIds.has(word.id)) {
        return;
      }
      word.positions.forEach((cell) => {
        keys.add(buildCellKey(cell.row, cell.col));
      });
    });
    return keys;
  }, [foundWordIds, puzzle.words]);
  const tempSelectionKeys = useMemo(
    () => new Set((tempSelection?.path ?? []).map((cell) => buildCellKey(cell.row, cell.col))),
    [tempSelection?.path],
  );
  const tempSelectionStartKey = tempSelection
    ? buildCellKey(tempSelection.start.row, tempSelection.start.col)
    : null;
  const tempSelectionEndKey = tempSelection
    ? buildCellKey(tempSelection.end.row, tempSelection.end.col)
    : null;
  const hiddenWordTargetKey = hiddenWordTargetCell
    ? buildCellKey(hiddenWordTargetCell.row, hiddenWordTargetCell.col)
    : null;
  const evidenceKeys = useMemo(
    () => new Set(nextMoveEvidenceCells.map((cell) => buildCellKey(cell.row, cell.col))),
    [nextMoveEvidenceCells],
  );
  const targetKeys = useMemo(
    () => new Set(nextMoveTargetCells.map((cell) => buildCellKey(cell.row, cell.col))),
    [nextMoveTargetCells],
  );
  const crosshairStart = tempSelection?.start ?? null;

  const handleTap = useCallback((x: number, y: number) => {
    if (!onCellTap) {
      return;
    }
    const cell = getCellAtPoint(layout, puzzle.rows, puzzle.cols, x, y);
    if (!cell) {
      return;
    }
    onCellTap(cell.row, cell.col);
  }, [layout, onCellTap, puzzle.cols, puzzle.rows]);

  const handleDragStart = useCallback((x: number, y: number) => {
    if (!onSelectionStart) {
      return;
    }
    const cell = getCellAtPoint(layout, puzzle.rows, puzzle.cols, x, y);
    if (!cell) {
      return;
    }
    dragCellKeyRef.current = buildCellKey(cell.row, cell.col);
    onSelectionStart(cell.row, cell.col);
  }, [layout, onSelectionStart, puzzle.cols, puzzle.rows]);

  const handleDragMove = useCallback((x: number, y: number) => {
    if (!onSelectionMove) {
      return;
    }
    const cell = getCellAtPoint(layout, puzzle.rows, puzzle.cols, x, y);
    if (!cell) {
      return;
    }
    const key = buildCellKey(cell.row, cell.col);
    if (dragCellKeyRef.current === key) {
      return;
    }
    dragCellKeyRef.current = key;
    onSelectionMove(cell.row, cell.col);
  }, [layout, onSelectionMove, puzzle.cols, puzzle.rows]);

  const handleDragEnd = useCallback(() => {
    dragCellKeyRef.current = null;
    onSelectionEnd?.();
  }, [onSelectionEnd]);

  const interactionGesture = useMemo(() => {
    if (!interactive) {
      return null;
    }

    const tapGesture = Gesture.Tap()
      .runOnJS(true)
      .maxDistance(10)
      .onEnd((event, success) => {
        if (!success) {
          return;
        }
        handleTap(event.x, event.y);
      });

    if (!allowDrag || !onSelectionStart || !onSelectionMove || !onSelectionEnd) {
      return tapGesture;
    }

    const panGesture = Gesture.Pan()
      .runOnJS(true)
      .minDistance(6)
      .onBegin((event) => {
        handleDragStart(event.x, event.y);
      })
      .onUpdate((event) => {
        handleDragMove(event.x, event.y);
      })
      .onEnd(() => {
        handleDragEnd();
      })
      .onFinalize(() => {
        dragCellKeyRef.current = null;
      });

    return Gesture.Simultaneous(tapGesture, panGesture);
  }, [
    allowDrag,
    handleDragEnd,
    handleDragMove,
    handleDragStart,
    handleTap,
    interactive,
    onSelectionEnd,
    onSelectionMove,
    onSelectionStart,
  ]);

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
              styles.shellBorder,
              { borderColor: withAlpha(theme.primaryLight, isDark ? 0.44 : 0.3) },
            ]}
          />
          <View
            style={[
              styles.shellFill,
              { backgroundColor: tokens.panelFill },
            ]}
          />
          <View
            style={[
              styles.boardBorder,
              { borderColor: withAlpha(theme.primary, isDark ? 0.28 : 0.18) },
            ]}
          />
          <View
            style={[
              styles.boardSurface,
              { backgroundColor: withAlpha(theme.panelSurfaceElevated, isDark ? 0.38 : 0.54) },
            ]}
          />

          {puzzle.grid.map((row, rowIndex) => row.map((letter, colIndex) => {
            const key = buildCellKey(rowIndex, colIndex);
            const rect = getGridCellRect(layout, rowIndex, colIndex);
            const isFound = foundCellKeys.has(key);
            const isTemp = tempSelectionKeys.has(key);
            const isHiddenTarget = hiddenWordTargetKey === key;
            const isEvidence = evidenceKeys.has(key);
            const isTarget = targetKeys.has(key);
            const isSelectionStart = tempSelectionStartKey === key;
            const isSelectionEnd = tempSelectionEndKey === key;
            const isInCrosshair = crosshairStart !== null
              && !isTemp && !isFound && !isSelectionStart && !isSelectionEnd && !isTarget && !isEvidence
              && (rowIndex === crosshairStart.row || colIndex === crosshairStart.col);
            const borderColor = isHiddenTarget
              ? withAlpha(theme.difficultyMedium, isDark ? 0.92 : 0.78)
              : isTarget
              ? withAlpha(theme.success, isDark ? 0.92 : 0.74)
              : isSelectionStart || isSelectionEnd
                ? withAlpha(theme.primaryLight, isDark ? 0.94 : 0.74)
                : isTemp
                  ? withAlpha(theme.primary, isDark ? 0.82 : 0.62)
                  : isFound
                    ? withAlpha(theme.success, isDark ? 0.68 : 0.52)
                    : isEvidence
                      ? withAlpha(theme.primary, isDark ? 0.7 : 0.46)
                      : withAlpha(theme.border, isDark ? 0.95 : 0.85);
            const faceColor = isHiddenTarget
              ? withAlpha(theme.difficultyMedium, isDark ? 0.24 : 0.18)
              : isTarget
              ? withAlpha(theme.success, isDark ? 0.24 : 0.16)
              : isSelectionStart || isSelectionEnd
                ? withAlpha(theme.primary, isDark ? 0.3 : 0.2)
                : isTemp
                  ? withAlpha(theme.primary, isDark ? 0.22 : 0.14)
                  : isFound
                    ? withAlpha(theme.success, isDark ? 0.22 : 0.12)
                    : isEvidence
                      ? withAlpha(theme.primary, isDark ? 0.12 : 0.08)
                      : isInCrosshair
                        ? withAlpha(theme.primary, isDark ? 0.1 : 0.06)
                        : tokens.cellRaisedFill;

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
                    borderColor,
                    backgroundColor: tokens.cellRaisedFill,
                  },
                ]}
              >
                <View
                  style={[
                    styles.cellFace,
                    { backgroundColor: faceColor },
                  ]}
                />
                <Text
                  style={[
                    styles.cellLetter,
                    {
                      fontSize: Math.max(12, layout.cellSize * 0.44),
                      color: theme.text,
                    },
                  ]}
                >
                  {letter}
                </Text>
              </View>
            );
          }))}
        </View>

        {interactionGesture ? (
          <GestureDetector gesture={interactionGesture}>
            <View pointerEvents="box-only" style={StyleSheet.absoluteFill} />
          </GestureDetector>
        ) : null}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  boardArea: {
    width: '100%',
    alignItems: 'center',
    justifyContent: 'center',
  },
  boardFrame: {
    position: 'relative',
  },
  shellBorder: {
    ...StyleSheet.absoluteFill,
    borderWidth: 1,
    borderRadius: SHELL_RADIUS,
  },
  shellFill: {
    ...StyleSheet.absoluteFill,
    top: SHELL_INSET,
    right: SHELL_INSET,
    bottom: SHELL_INSET,
    left: SHELL_INSET,
    borderRadius: SHELL_INNER_RADIUS,
  },
  boardBorder: {
    ...StyleSheet.absoluteFill,
    borderWidth: 1,
    borderRadius: BOARD_RADIUS,
  },
  boardSurface: {
    ...StyleSheet.absoluteFill,
    top: BOARD_INSET,
    right: BOARD_INSET,
    bottom: BOARD_INSET,
    left: BOARD_INSET,
    borderRadius: BOARD_SURFACE_RADIUS,
  },
  cellFrame: {
    position: 'absolute',
    borderWidth: 2,
    borderRadius: CELL_RADIUS,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cellFace: {
    ...StyleSheet.absoluteFill,
    margin: CELL_FACE_INSET,
    borderRadius: CELL_RADIUS - CELL_FACE_INSET,
  },
  cellLetter: {
    fontFamily: 'monospace',
    fontWeight: '800',
    textAlign: 'center',
  },
});
