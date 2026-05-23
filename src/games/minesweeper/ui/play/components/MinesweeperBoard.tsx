import React, { useMemo, useState } from 'react';
import type { LayoutChangeEvent } from 'react-native';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import type { MinesweeperBoard as MinesweeperBoardState } from '../../../types';
import { useLanguage } from '../../../../../app/context/LanguageContext';
import { useTheme } from '../../../../../app/context/ThemeContext';
import {
  createBoundedGridLayout,
  createFixedGridLayout,
  getGridCellRect,
} from '../../../../../app/shell/boardLayout';
import { createSharedBoardRenderTokens } from '../../../../../app/shell/renderTokens';
import { withAlpha } from '../../../../../app/utils/color';
import { getMinesweeperLearningCenterContent } from '../../../content/i18n';
import {
  getMinesweeperNumberColor,
  MINESWEEPER_BOARD_CELL_GAP,
  MINESWEEPER_FRAME_BORDER_WIDTH,
  MINESWEEPER_FRAME_PADDING,
} from './boardStyles';
import MinesweeperMineGlyph from '../../shared/mineGlyph';

interface MinesweeperBoardProps {
  board: MinesweeperBoardState;
  /** Measured viewport width from the play adapter for fit-first sizing. */
  containerWidth?: number;
  /** Measured viewport height from the play adapter for fit-first sizing. */
  containerHeight?: number;
  onReveal: (row: number, col: number) => void;
  onToggleFlag: (row: number, col: number) => void;
  nextMoveEvidenceCells?: Array<{ row: number; col: number }>;
  nextMoveTargetCells?: Array<{ row: number; col: number }>;
  nextMoveSafeTargetCells?: Array<{ row: number; col: number }>;
  nextMoveMineTargetCells?: Array<{ row: number; col: number }>;
  focusedCells?: Array<{ row: number; col: number }>;
}

const SHELL_INSET = 2;
const SHELL_RADIUS = 8;
const SHELL_INNER_RADIUS = 6;
const BOARD_INSET = 1;
const BOARD_RADIUS = 6;
const BOARD_SURFACE_RADIUS = 4;
const CELL_RADIUS = 4;
const CELL_FACE_INSET = 1;

function buildCellKeySet(cells: ReadonlyArray<{ row: number; col: number }>): Set<string> {
  return new Set(cells.map(({ row, col }) => `${row}:${col}`));
}

function MinesweeperBoard({
  board,
  containerWidth,
  containerHeight,
  onReveal,
  onToggleFlag,
  nextMoveEvidenceCells = [],
  nextMoveTargetCells = [],
  nextMoveSafeTargetCells = nextMoveTargetCells,
  nextMoveMineTargetCells = [],
  focusedCells = [],
}: MinesweeperBoardProps) {
  const { resolvedLanguage } = useLanguage();
  const { theme, isDark } = useTheme();
  const tokens = useMemo(() => createSharedBoardRenderTokens(theme, isDark), [theme, isDark]);
  const lc = useMemo(() => getMinesweeperLearningCenterContent(), [resolvedLanguage]);
  // Self-measure only when the caller does not provide viewport dimensions (e.g. analysis boards).
  const [selfMeasured, setSelfMeasured] = useState({ width: 0, height: 0 });
  const effectiveWidth = containerWidth ?? selfMeasured.width;
  const effectiveHeight = containerHeight ?? selfMeasured.height;
  const layout = useMemo(() => {
    if (effectiveWidth > 0 && effectiveHeight > 0) {
      return createBoundedGridLayout({
        containerWidth: effectiveWidth,
        containerHeight: effectiveHeight,
        rows: board.rows,
        cols: board.cols,
        gap: MINESWEEPER_BOARD_CELL_GAP,
        padding: MINESWEEPER_FRAME_PADDING,
        borderWidth: MINESWEEPER_FRAME_BORDER_WIDTH,
        minCellSize: 1,
        maxCellSize: Number.MAX_SAFE_INTEGER,
      });
    }

    return createFixedGridLayout({
      rows: board.rows,
      cols: board.cols,
      cellSize: 1,
      gap: MINESWEEPER_BOARD_CELL_GAP,
      padding: MINESWEEPER_FRAME_PADDING,
      borderWidth: MINESWEEPER_FRAME_BORDER_WIDTH,
    });
  }, [board.cols, board.rows, effectiveHeight, effectiveWidth]);
  const evidenceKeys = useMemo(() => buildCellKeySet(nextMoveEvidenceCells), [nextMoveEvidenceCells]);
  const safeTargetKeys = useMemo(() => buildCellKeySet(nextMoveSafeTargetCells), [nextMoveSafeTargetCells]);
  const mineTargetKeys = useMemo(() => buildCellKeySet(nextMoveMineTargetCells), [nextMoveMineTargetCells]);
  const focusedKeys = useMemo(() => buildCellKeySet(focusedCells), [focusedCells]);

  return (
    <View
      style={styles.shell}
      onLayout={containerWidth === undefined || containerHeight === undefined ? (event: LayoutChangeEvent) => {
        const { width, height } = event.nativeEvent.layout;
        setSelfMeasured({ width, height });
      } : undefined}
    >
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
          {board.cells.map((row, rowIndex) => row
            .map((cell, colIndex) => {
              const key = `${rowIndex}:${colIndex}`;
              const rect = getGridCellRect(layout, rowIndex, colIndex);
              const isFocused = focusedKeys.has(key);
              const borderColor = isFocused
                ? withAlpha(theme.primaryLight, 0.92)
                : mineTargetKeys.has(key)
                ? withAlpha(theme.difficultyExpert, isDark ? 0.84 : 0.68)
                : safeTargetKeys.has(key)
                  ? withAlpha(theme.success, isDark ? 0.84 : 0.68)
                  : evidenceKeys.has(key)
                    ? withAlpha(theme.primary, isDark ? 0.64 : 0.46)
                    : withAlpha(theme.gridCellBorder, isDark ? 0.84 : 0.62);
              const faceColor = isFocused
                ? withAlpha(theme.primary, isDark ? 0.22 : 0.14)
                : mineTargetKeys.has(key)
                ? withAlpha(theme.difficultyExpert, isDark ? 0.26 : 0.16)
                : safeTargetKeys.has(key)
                  ? withAlpha(theme.success, isDark ? 0.26 : 0.16)
                  : evidenceKeys.has(key)
                    ? withAlpha(theme.primary, isDark ? 0.14 : 0.08)
                    : cell.state === 'flagged'
                      ? withAlpha(theme.primaryLight, 0.2)
                      : cell.state === 'revealed' && cell.isMine
                        ? withAlpha(theme.difficultyExpert, 0.22)
                        : cell.state === 'revealed'
                          ? tokens.cellSunkenFill
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
                      borderWidth: isFocused ? 2 : 1,
                      opacity: focusedKeys.size === 0 || isFocused || evidenceKeys.has(key) ? 1 : 0.82,
                      backgroundColor: cell.state === 'revealed' ? tokens.cellSunkenFill : tokens.cellRaisedFill,
                    },
                  ]}
                >
                  <View style={[styles.cellFace, { backgroundColor: faceColor }]} />
                  {cell.state === 'flagged' ? (
                    <View style={styles.flagWrap}>
                      <View style={[styles.flagPole, { backgroundColor: tokens.text }]} />
                      <View
                        style={[
                          styles.flagPennant,
                          {
                            borderLeftColor: mineTargetKeys.has(key)
                              ? theme.difficultyExpert
                              : safeTargetKeys.has(key)
                                ? theme.success
                                : theme.primaryLight,
                          },
                        ]}
                      />
                    </View>
                  ) : null}
                  {cell.state === 'revealed' && cell.isMine ? (
                    <MinesweeperMineGlyph
                      style={[
                        styles.mineGlyph,
                        {
                          color: mineTargetKeys.has(key) ? theme.difficultyExpert : tokens.danger,
                          fontSize: layout.cellSize * 0.52,
                        },
                      ]}
                    />
                  ) : null}
                  {cell.state === 'revealed' && !cell.isMine && cell.adjacentMines > 0 ? (
                    <Text
                      style={[
                        styles.clueValue,
                        {
                          color: mineTargetKeys.has(key)
                            ? theme.difficultyExpert
                            : safeTargetKeys.has(key)
                              ? theme.success
                              : getMinesweeperNumberColor(theme, cell.adjacentMines),
                          fontSize: layout.cellSize * 0.52,
                        },
                      ]}
                    >
                      {String(cell.adjacentMines)}
                    </Text>
                  ) : null}
                </View>
              );
            }))}
        </View>
        <View pointerEvents="box-none" style={StyleSheet.absoluteFill}>
          {board.cells.map((row, rowIndex) => row
            .map((_, colIndex) => {
              const rect = getGridCellRect(layout, rowIndex, colIndex);

              return (
                <Pressable
                  key={`${rowIndex}-${colIndex}`}
                  style={[
                    styles.hitCell,
                    {
                      left: rect.x,
                      top: rect.y,
                      width: rect.width,
                      height: rect.height,
                    },
                  ]}
                  onPress={() => onReveal(rowIndex, colIndex)}
                  onLongPress={() => onToggleFlag(rowIndex, colIndex)}
                  delayLongPress={180}
                  accessibilityRole="button"
                  accessibilityLabel={lc.formatCellLabel({ row: rowIndex, col: colIndex })}
                />
              );
            }))}
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  shell: {
    width: '100%',
    alignItems: 'center',
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
  clueValue: {
    fontFamily: 'monospace',
    fontWeight: '800',
    textAlign: 'center',
  },
  mineGlyph: {
    textAlign: 'center',
  },
  flagWrap: {
    width: '70%',
    height: '70%',
    alignItems: 'center',
    justifyContent: 'center',
  },
  flagPole: {
    position: 'absolute',
    left: '32%',
    width: 2,
    height: '62%',
    borderRadius: 1,
  },
  flagPennant: {
    position: 'absolute',
    top: '16%',
    left: '36%',
    width: 0,
    height: 0,
    borderTopWidth: 5,
    borderBottomWidth: 5,
    borderLeftWidth: 10,
    borderTopColor: 'transparent',
    borderBottomColor: 'transparent',
  },
  hitCell: {
    position: 'absolute',
  },
});

export default React.memo(MinesweeperBoard);
