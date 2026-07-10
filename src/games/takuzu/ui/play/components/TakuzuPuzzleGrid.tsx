import React, { useMemo } from 'react';
import {
  Animated,
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
import type { BoardFeedbackEffect } from '../../../../../app/shell/boardFeedback';
import { createSharedBoardRenderTokens } from '../../../../../app/shell/renderTokens';
import { useBoardFeedbackAnimation } from '../../../../../app/shell/useBoardFeedbackAnimation';
import { withAlpha } from '../../../../../app/utils/color';
import { getTakuzuStrings } from '../../../content/i18n';
import type {
  Grid,
  TakuzuNextMoveCell,
  TakuzuNextMoveTargetCell,
} from '../../../types';

const FRAME_PADDING = 6;
const GRID_PADDING = 1;
const GAP = 1;
const SHELL_INSET = 2;
const SHELL_RADIUS = 8;
const SHELL_INNER_RADIUS = 6;
const BOARD_INSET = 1;
const BOARD_RADIUS = 6;
const BOARD_SURFACE_RADIUS = 4;
const CELL_RADIUS = 4;
const CELL_FACE_INSET = 1;

interface PuzzleGridProps {
  board: Grid;
  isGiven: boolean[][];
  finishedCells: boolean[][];
  boardFeedbackEffects?: readonly BoardFeedbackEffect[] | null;
  nextMoveEvidenceCells?: TakuzuNextMoveCell[];
  nextMoveTargetCells?: TakuzuNextMoveTargetCell[];
  nextMoveHighlightRows?: number[];
  nextMoveHighlightCols?: number[];
  size: number;
  onCellPress: (row: number, col: number) => void;
  containerWidth: number;
  containerHeight: number;
}

function buildCellKeySet(cells: ReadonlyArray<{ row: number; col: number }>): Set<string> {
  return new Set(cells.map(({ row, col }) => `${row}:${col}`));
}

function TakuzuPuzzleGrid({
  board,
  isGiven,
  finishedCells,
  boardFeedbackEffects,
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
  const takuzuStrings = useMemo(() => getTakuzuStrings(), [resolvedLanguage]);
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
  const {
    activeSpinKeys,
    activeShakeKeys,
    rotate,
    shakeTranslateX,
  } = useBoardFeedbackAnimation({
    effects: boardFeedbackEffects,
    cellSize: layout.cellSize,
  });
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
            const given = isGiven[rowIndex][colIndex];
            const locked = given || finishedCells[rowIndex][colIndex];
            const showTarget = targetKeys.has(key);
            const showEvidence = evidenceKeys.has(key);
            const showHighlight = highlightedRows.has(rowIndex) || highlightedCols.has(colIndex);
            const showShake = activeShakeKeys.has(key) && value !== null;

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
                        : withAlpha(theme.border, isDark ? 0.95 : 0.85),
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
                {value !== null && !activeSpinKeys.has(key) && !showShake ? (
                  <Text
                    style={[
                      styles.cellValue,
                      {
                        color: given
                          ? withAlpha(theme.text, isDark ? 0.92 : 0.84)
                          : withAlpha(theme.primary, isDark ? 0.98 : 0.9),
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
            const given = isGiven[rowIndex][colIndex];
            const locked = given || finishedCells[rowIndex][colIndex];
            const showSpin = activeSpinKeys.has(key) && value !== null;
            const showShake = activeShakeKeys.has(key) && value !== null;

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
                  accessibilityLabel={`${takuzuStrings.play.cellLabel} ${rowIndex + 1}-${colIndex + 1}`}
                  onPress={() => onCellPress(rowIndex, colIndex)}
                />
                {showSpin || showShake ? (
                  <Animated.View
                    pointerEvents="none"
                    style={[
                      styles.valueOverlay,
                      {
                        left: rect.x,
                        top: rect.y,
                        width: rect.width,
                        height: rect.height,
                        transform: showShake ? [{ translateX: shakeTranslateX }] : [{ rotate }],
                      },
                    ]}
                  >
                    <Animated.Text
                      style={{
                        color: showShake
                          ? withAlpha(theme.difficultyExpert, isDark ? 0.98 : 0.9)
                          : given
                            ? withAlpha(theme.text, isDark ? 0.98 : 0.88)
                            : withAlpha(theme.primaryLight, isDark ? 0.98 : 0.82),
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
    ...StyleSheet.absoluteFill,
    borderWidth: 1,
    borderRadius: SHELL_RADIUS,
  },
  fieldKitShellFill: {
    ...StyleSheet.absoluteFill,
    top: SHELL_INSET,
    right: SHELL_INSET,
    bottom: SHELL_INSET,
    left: SHELL_INSET,
    borderRadius: SHELL_INNER_RADIUS,
  },
  fieldKitBoardBorder: {
    ...StyleSheet.absoluteFill,
    borderWidth: 1,
    borderRadius: BOARD_RADIUS,
  },
  fieldKitBoardSurface: {
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
  valueOverlay: {
    position: 'absolute',
    alignItems: 'center',
    justifyContent: 'center',
  },
});

export default React.memo(TakuzuPuzzleGrid);
