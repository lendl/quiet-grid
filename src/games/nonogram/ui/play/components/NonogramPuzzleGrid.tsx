import React, { useCallback, useMemo, useRef, useState } from 'react';
import { LayoutChangeEvent, StyleSheet, Text, View } from 'react-native';
import { Gesture, GestureDetector } from 'react-native-gesture-handler';
import { useTheme } from '../../../../../app/context/ThemeContext';
import {
  createNonogramBoardLayout,
  getNonogramCellRect,
  padNonogramClues,
} from '../../../../../app/shell/boardLayout';
import { createSharedBoardRenderTokens } from '../../../../../app/shell/renderTokens';
import { withAlpha } from '../../../../../app/utils/color';
import type {
  NonogramBoard,
  NonogramCellRef,
  NonogramCellValue,
  NonogramDirectState,
  NonogramPuzzle,
} from '../../../types';

const BOARD_FRAME_RADIUS = 8;
const BOARD_INNER_RADIUS = 6;
const BOARD_SURFACE_RADIUS = 4;
const CELL_RADIUS = 4;
const CELL_FACE_INSET = 1;
const SEPARATOR_THICKNESS = 4;

interface NonogramPuzzleGridProps {
  puzzle: NonogramPuzzle;
  board: NonogramBoard;
  containerWidth?: number;
  containerHeight?: number;
  interactive?: boolean;
  allowSwipe?: boolean;
  onCellTap?: (row: number, col: number) => void;
  onCellSwipe?: (cells: readonly NonogramCellRef[], value: NonogramDirectState) => void;
  nextMoveEvidenceCells?: readonly NonogramCellRef[];
  nextMoveTargetCells?: ReadonlyArray<NonogramCellRef & { value: NonogramDirectState }>;
  nextMoveHighlightRows?: readonly number[];
  nextMoveHighlightCols?: readonly number[];
}

function buildCellKey(row: number, col: number): string {
  return `${row}:${col}`;
}

function buildCellKeySet(cells: readonly NonogramCellRef[]): Set<string> {
  return new Set(cells.map(({ row, col }) => buildCellKey(row, col)));
}

function buildTargetValueMap(
  cells: ReadonlyArray<NonogramCellRef & { value: NonogramDirectState }>,
): Map<string, NonogramDirectState> {
  return new Map(cells.map(({ row, col, value }) => [buildCellKey(row, col), value]));
}

function getSwipePaintValue(cell: NonogramCellValue): NonogramDirectState {
  return cell === 1 ? 0 : 1;
}

function getCellAtPoint(
  layout: ReturnType<typeof createNonogramBoardLayout>,
  rows: number,
  cols: number,
  x: number,
  y: number,
): NonogramCellRef | null {
  const gridX = layout.gridX;
  const gridY = layout.gridY;
  const offsetX = x - gridX;
  const offsetY = y - gridY;

  if (offsetX < 0 || offsetY < 0) {
    return null;
  }

  const stride = layout.cellSize + layout.cellGap;
  const col = Math.floor(offsetX / stride);
  const row = Math.floor(offsetY / stride);

  if (row < 0 || row >= rows || col < 0 || col >= cols) {
    return null;
  }

  const rect = getNonogramCellRect(layout, row, col);
  if (x < rect.x || x > rect.x + rect.width || y < rect.y || y > rect.y + rect.height) {
    return null;
  }

  return { row, col };
}

export default function NonogramPuzzleGrid({
  puzzle,
  board,
  containerWidth,
  containerHeight: _containerHeight,
  interactive = true,
  allowSwipe = true,
  onCellTap,
  onCellSwipe,
  nextMoveEvidenceCells = [],
  nextMoveTargetCells = [],
  nextMoveHighlightRows = [],
  nextMoveHighlightCols = [],
}: NonogramPuzzleGridProps) {
  const { theme, isDark } = useTheme();
  const tokens = useMemo(() => createSharedBoardRenderTokens(theme, isDark), [theme, isDark]);
  const [measuredWidth, setMeasuredWidth] = useState(0);
  const effectiveWidth = containerWidth ?? (measuredWidth || 384);
  const layout = useMemo(() => createNonogramBoardLayout({
    containerWidth: effectiveWidth,
    rows: puzzle.rows,
    cols: puzzle.cols,
    rowClues: puzzle.rowClues,
    colClues: puzzle.colClues,
    interactive,
  }), [effectiveWidth, interactive, puzzle.colClues, puzzle.cols, puzzle.rowClues, puzzle.rows]);
  const evidenceKeySet = useMemo(() => buildCellKeySet(nextMoveEvidenceCells), [nextMoveEvidenceCells]);
  const targetValueMap = useMemo(() => buildTargetValueMap(nextMoveTargetCells), [nextMoveTargetCells]);
  const highlightedRows = useMemo(() => new Set(nextMoveHighlightRows), [nextMoveHighlightRows]);
  const highlightedCols = useMemo(() => new Set(nextMoveHighlightCols), [nextMoveHighlightCols]);
  const activeSwipeCellsRef = useRef<NonogramCellRef[]>([]);
  const activeSwipeKeysRef = useRef(new Set<string>());
  const activeSwipeValueRef = useRef<NonogramDirectState>(1);

  const resetSwipeState = useCallback(() => {
    activeSwipeCellsRef.current = [];
    activeSwipeKeysRef.current = new Set();
  }, []);

  const appendSwipeCell = useCallback((cell: NonogramCellRef) => {
    const key = buildCellKey(cell.row, cell.col);
    if (activeSwipeKeysRef.current.has(key)) {
      return;
    }

    activeSwipeKeysRef.current.add(key);
    activeSwipeCellsRef.current.push(cell);
  }, []);

  const handleTap = useCallback((x: number, y: number) => {
    if (!onCellTap) {
      return;
    }

    const cell = getCellAtPoint(layout, puzzle.rows, puzzle.cols, x, y);
    if (!cell) {
      return;
    }

    onCellTap(cell.row, cell.col);
  }, [layout, onCellTap]);

  const handleSwipeStart = useCallback((x: number, y: number) => {
    resetSwipeState();
    const cell = getCellAtPoint(layout, puzzle.rows, puzzle.cols, x, y);
    if (!cell) {
      return;
    }

    activeSwipeValueRef.current = getSwipePaintValue(board[cell.row]?.[cell.col] ?? null);
    appendSwipeCell(cell);
  }, [appendSwipeCell, board, layout, resetSwipeState]);

  const handleSwipeMove = useCallback((x: number, y: number) => {
    const cell = getCellAtPoint(layout, puzzle.rows, puzzle.cols, x, y);
    if (!cell) {
      return;
    }

    if (activeSwipeCellsRef.current.length === 0) {
      activeSwipeValueRef.current = getSwipePaintValue(board[cell.row]?.[cell.col] ?? null);
    }

    appendSwipeCell(cell);
  }, [appendSwipeCell, board, layout]);

  const handleSwipeEnd = useCallback(() => {
    if (!onCellSwipe || activeSwipeCellsRef.current.length === 0) {
      resetSwipeState();
      return;
    }

    onCellSwipe(activeSwipeCellsRef.current, activeSwipeValueRef.current);
    resetSwipeState();
  }, [onCellSwipe, resetSwipeState]);

  const interactionGesture = useMemo(() => {
    if (!interactive || (!onCellTap && !onCellSwipe)) {
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

    if (!allowSwipe || !onCellSwipe) {
      return tapGesture;
    }

    const panGesture = Gesture.Pan()
      .runOnJS(true)
      .minDistance(8)
      .maxPointers(1)
      .onBegin((event) => {
        handleSwipeStart(event.x, event.y);
      })
      .onUpdate((event) => {
        handleSwipeMove(event.x, event.y);
      })
      .onEnd(() => {
        handleSwipeEnd();
      })
      .onFinalize(() => {
        resetSwipeState();
      });

    return Gesture.Simultaneous(tapGesture, panGesture);
  }, [allowSwipe, handleSwipeEnd, handleSwipeMove, handleSwipeStart, handleTap, interactive, onCellSwipe, onCellTap, resetSwipeState]);

  const rows = puzzle.rowClues.map((clues) => padNonogramClues(clues, layout.rowClueDepth));
  const cols = puzzle.colClues.map((clues) => padNonogramClues(clues, layout.colClueDepth));
  const gridWidth = layout.gridFrameWidth - (layout.gridPadding * 2);
  const gridHeight = layout.gridFrameHeight - (layout.gridPadding * 2);

  return (
    <View
      style={styles.boardArea}
      onLayout={containerWidth === undefined ? (event: LayoutChangeEvent) => {
        setMeasuredWidth(event.nativeEvent.layout.width);
      } : undefined}
    >
      <View
        style={[
          styles.boardFrame,
          {
            width: layout.boardWidth,
            height: layout.boardHeight,
          },
        ]}
      >
        <View pointerEvents="none" style={StyleSheet.absoluteFill}>
          <View
            style={[
              styles.shellBorder,
              { borderColor: withAlpha(theme.primaryLight, isDark ? 0.38 : 0.28) },
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
              { borderColor: withAlpha(theme.primary, isDark ? 0.22 : 0.16) },
            ]}
          />
          <View
            style={[
              styles.boardSurface,
              { backgroundColor: withAlpha(theme.panelSurfaceElevated, isDark ? 0.4 : 0.52) },
            ]}
          />
        </View>

        <View pointerEvents="none" style={StyleSheet.absoluteFill}>
          {rows.map((rowClues, rowIndex) => rowClues.map((clue, clueIndex) => {
            const top = layout.rowRailY + rowIndex * (layout.cellSize + layout.cellGap)
              + Math.floor((layout.cellSize - layout.rowClueSlotSize) / 2);
            const left = layout.rowRailX + clueIndex * (layout.rowClueSlotSize + layout.rowClueValueGap);
            const isActiveRow = highlightedRows.has(rowIndex);

            return (
              <View
                key={`row-clue-${rowIndex}-${clueIndex}`}
                style={[
                  styles.clueSlot,
                  {
                    left,
                    top,
                    width: layout.rowClueSlotSize,
                    height: layout.rowClueSlotSize,
                    backgroundColor: isActiveRow
                      ? withAlpha(theme.primary, isDark ? 0.16 : 0.08)
                      : withAlpha(theme.surfaceElevated, isDark ? 0.9 : 0.82),
                    borderColor: isActiveRow
                      ? withAlpha(theme.primaryLight, isDark ? 0.72 : 0.56)
                      : withAlpha(theme.border, isDark ? 0.74 : 0.54),
                  },
                ]}
              >
                {clue !== null ? (
                  <Text
                    style={[
                      styles.clueText,
                      {
                        color: isActiveRow ? theme.primaryLight : theme.textSecondary,
                        fontSize: Math.max(10, Math.min(14, layout.rowClueSlotSize * 0.58)),
                      },
                    ]}
                  >
                    {clue}
                  </Text>
                ) : null}
              </View>
            );
          }))}

          {cols.map((colClues, colIndex) => colClues.map((clue, clueIndex) => {
            const left = layout.gridX + colIndex * (layout.cellSize + layout.cellGap)
              + Math.floor((layout.cellSize - layout.colClueSlotSize) / 2);
            const top = layout.colRailY + clueIndex * (layout.colClueSlotSize + layout.colClueValueGap);
            const isActiveCol = highlightedCols.has(colIndex);

            return (
              <View
                key={`col-clue-${colIndex}-${clueIndex}`}
                style={[
                  styles.clueSlot,
                  {
                    left,
                    top,
                    width: layout.colClueSlotSize,
                    height: layout.colClueSlotSize,
                    backgroundColor: isActiveCol
                      ? withAlpha(theme.primary, isDark ? 0.16 : 0.08)
                      : withAlpha(theme.surfaceElevated, isDark ? 0.9 : 0.82),
                    borderColor: isActiveCol
                      ? withAlpha(theme.primaryLight, isDark ? 0.72 : 0.56)
                      : withAlpha(theme.border, isDark ? 0.74 : 0.54),
                  },
                ]}
              >
                {clue !== null ? (
                  <Text
                    style={[
                      styles.clueText,
                      {
                        color: isActiveCol ? theme.primaryLight : theme.textSecondary,
                        fontSize: Math.max(10, Math.min(14, layout.colClueSlotSize * 0.58)),
                      },
                    ]}
                  >
                    {clue}
                  </Text>
                ) : null}
              </View>
            );
          }))}

          {board.map((row, rowIndex) => row.map((cell, colIndex) => {
            const key = buildCellKey(rowIndex, colIndex);
            const rect = getNonogramCellRect(layout, rowIndex, colIndex);
            const isTarget = targetValueMap.has(key);
            const targetValue = targetValueMap.get(key);
            const isEvidence = evidenceKeySet.has(key);
            const isRowHighlighted = highlightedRows.has(rowIndex);
            const isColHighlighted = highlightedCols.has(colIndex);
            const showHighlight = isEvidence || isTarget || isRowHighlighted || isColHighlighted;
            const targetAccent = targetValue === 1 ? theme.success : theme.textSecondary;
            const faceColor = cell === 1
              ? withAlpha(theme.primary, isDark ? 0.82 : 0.72)
              : cell === 0
                ? withAlpha(theme.textSecondary, isDark ? 0.22 : 0.14)
                : showHighlight
                  ? withAlpha(theme.primary, isDark ? 0.12 : 0.08)
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
                    borderColor: isTarget
                      ? withAlpha(targetAccent, isDark ? 0.9 : 0.72)
                      : isEvidence
                        ? withAlpha(theme.primary, isDark ? 0.68 : 0.48)
                        : withAlpha(theme.border, isDark ? 0.7 : 0.52),
                    backgroundColor: tokens.cellRaisedFill,
                  },
                ]}
              >
                <View
                  style={[
                    styles.cellFace,
                    {
                      backgroundColor: faceColor,
                    },
                  ]}
                />
                <View
                  style={[
                    styles.cellInset,
                    {
                      borderColor: isTarget
                        ? withAlpha(targetAccent, isDark ? 0.92 : 0.74)
                        : showHighlight
                          ? withAlpha(theme.primary, isDark ? 0.34 : 0.18)
                          : tokens.cellBevelDark,
                    },
                  ]}
                />
                {cell === 1 ? (
                  <View
                    style={[
                      styles.filledMark,
                      {
                        backgroundColor: withAlpha(theme.primaryLight, isDark ? 0.88 : 0.76),
                      },
                    ]}
                  />
                ) : null}
                {cell === 0 ? (
                  <Text
                    style={[
                      styles.emptyMark,
                      {
                        color: isTarget && targetValue === 0
                          ? targetAccent
                          : theme.textMuted,
                        fontSize: Math.max(11, rect.width * 0.52),
                      },
                    ]}
                  >
                    ×
                  </Text>
                ) : null}
                {isTarget && targetValue !== undefined ? (
                  <View
                    style={[
                      styles.targetBadge,
                      {
                        backgroundColor: withAlpha(targetAccent, isDark ? 0.92 : 0.78),
                      },
                    ]}
                  >
                    <Text style={[styles.targetBadgeText, { color: tokens.onPrimary }]}>
                      {targetValue === 1 ? '1' : '×'}
                    </Text>
                  </View>
                ) : null}
              </View>
            );
          }))}

          {Array.from({ length: puzzle.cols - 1 }, (_, index) => index + 1)
            .filter((index) => index % 5 === 0)
            .map((index) => {
              const left = layout.gridX + index * layout.cellSize + (index - 1) * layout.cellGap - Math.floor(SEPARATOR_THICKNESS / 2);
              return (
                <View
                  key={`v-separator-${index}`}
                  pointerEvents="none"
                  style={[
                    styles.separator,
                    {
                      left,
                      top: layout.gridY,
                      width: SEPARATOR_THICKNESS,
                      height: gridHeight,
                      backgroundColor: withAlpha(theme.primary, isDark ? 0.2 : 0.12),
                    },
                  ]}
                />
              );
            })}

          {Array.from({ length: puzzle.rows - 1 }, (_, index) => index + 1)
            .filter((index) => index % 5 === 0)
            .map((index) => {
              const top = layout.gridY + index * layout.cellSize + (index - 1) * layout.cellGap - Math.floor(SEPARATOR_THICKNESS / 2);
              return (
                <View
                  key={`h-separator-${index}`}
                  pointerEvents="none"
                  style={[
                    styles.separator,
                    {
                      left: layout.gridX,
                      top,
                      width: gridWidth,
                      height: SEPARATOR_THICKNESS,
                      backgroundColor: withAlpha(theme.primary, isDark ? 0.2 : 0.12),
                    },
                  ]}
                />
              );
            })}
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
    ...StyleSheet.absoluteFillObject,
    borderWidth: 1,
    borderRadius: BOARD_FRAME_RADIUS,
  },
  shellFill: {
    ...StyleSheet.absoluteFillObject,
    top: 2,
    right: 2,
    bottom: 2,
    left: 2,
    borderRadius: BOARD_INNER_RADIUS,
  },
  boardBorder: {
    ...StyleSheet.absoluteFillObject,
    borderWidth: 1,
    borderRadius: BOARD_INNER_RADIUS,
  },
  boardSurface: {
    ...StyleSheet.absoluteFillObject,
    top: 1,
    right: 1,
    bottom: 1,
    left: 1,
    borderRadius: BOARD_SURFACE_RADIUS,
  },
  clueSlot: {
    position: 'absolute',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderRadius: 6,
  },
  clueText: {
    fontWeight: '800',
    textAlign: 'center',
  },
  cellFrame: {
    position: 'absolute',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderRadius: CELL_RADIUS,
  },
  cellFace: {
    ...StyleSheet.absoluteFillObject,
    margin: CELL_FACE_INSET,
    borderRadius: CELL_RADIUS - CELL_FACE_INSET,
  },
  cellInset: {
    ...StyleSheet.absoluteFillObject,
    margin: 3,
    borderWidth: 1,
    borderRadius: CELL_RADIUS - 3,
    opacity: 0.9,
  },
  filledMark: {
    position: 'absolute',
    width: '68%',
    height: '68%',
    borderRadius: 4,
  },
  emptyMark: {
    position: 'absolute',
    fontWeight: '900',
    lineHeight: 24,
    textAlign: 'center',
  },
  targetBadge: {
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
  targetBadgeText: {
    fontSize: 9,
    fontWeight: '900',
    lineHeight: 10,
  },
  separator: {
    position: 'absolute',
  },
});
