import React, { useMemo, useState } from 'react';
import type { LayoutChangeEvent } from 'react-native';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import type { MinesweeperBoard as MinesweeperBoardState } from '../../../types';
import { useLanguage } from '../../../../../app/context/LanguageContext';
import { useTheme } from '../../../../../app/context/ThemeContext';
import {
  createFixedGridLayout,
  getGridCellRect,
} from '../../../../../app/shell/boardLayout';
import { createSharedBoardRenderTokens } from '../../../../../app/shell/renderTokens';
import { withAlpha } from '../../../../../app/utils/color';
import {
  getMinesweeperBoardCellSize,
  getMinesweeperNumberColor,
  MINESWEEPER_BOARD_CELL_GAP,
  MINESWEEPER_FRAME_BORDER_WIDTH,
  MINESWEEPER_FRAME_PADDING,
} from './boardStyles';

interface MinesweeperBoardProps {
  board: MinesweeperBoardState;
  onReveal: (row: number, col: number) => void;
  onToggleFlag: (row: number, col: number) => void;
  nextMoveEvidenceCells?: Array<{ row: number; col: number }>;
  nextMoveTargetCells?: Array<{ row: number; col: number }>;
  nextMoveSafeTargetCells?: Array<{ row: number; col: number }>;
  nextMoveMineTargetCells?: Array<{ row: number; col: number }>;
}

const MIN_CELL_SIZE = 32;
const MAX_CELL_SIZE = 40;
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
  onReveal,
  onToggleFlag,
  nextMoveEvidenceCells = [],
  nextMoveTargetCells = [],
  nextMoveSafeTargetCells = nextMoveTargetCells,
  nextMoveMineTargetCells = [],
}: MinesweeperBoardProps) {
  const { resolvedLanguage } = useLanguage();
  const { theme, isDark } = useTheme();
  const tokens = useMemo(() => createSharedBoardRenderTokens(theme, isDark), [theme, isDark]);
  const [containerSize, setContainerSize] = useState({ width: 0, height: 0 });
  const cellSize = useMemo(() => {
    if (containerSize.width <= 0) {
      return MIN_CELL_SIZE;
    }

    return getMinesweeperBoardCellSize(
      containerSize.width,
      board.cols,
      MIN_CELL_SIZE,
      MAX_CELL_SIZE,
      board.rows,
      containerSize.height > 0 ? containerSize.height : undefined,
    );
  }, [board.cols, board.rows, containerSize.height, containerSize.width]);
  const layout = useMemo(() => createFixedGridLayout({
    rows: board.rows,
    cols: board.cols,
    cellSize,
    gap: MINESWEEPER_BOARD_CELL_GAP,
    padding: MINESWEEPER_FRAME_PADDING,
    borderWidth: MINESWEEPER_FRAME_BORDER_WIDTH,
  }), [board.cols, board.rows, cellSize]);
  const evidenceKeys = useMemo(() => buildCellKeySet(nextMoveEvidenceCells), [nextMoveEvidenceCells]);
  const safeTargetKeys = useMemo(() => buildCellKeySet(nextMoveSafeTargetCells), [nextMoveSafeTargetCells]);
  const mineTargetKeys = useMemo(() => buildCellKeySet(nextMoveMineTargetCells), [nextMoveMineTargetCells]);

  function handleLayout(event: LayoutChangeEvent) {
    const { width, height } = event.nativeEvent.layout;
    setContainerSize({ width, height });
  }

  return (
    <View style={styles.shell} onLayout={handleLayout}>
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
          {board.cells.map((row, rowIndex) => row.map((cell, colIndex) => {
            const key = `${rowIndex}:${colIndex}`;
            const rect = getGridCellRect(layout, rowIndex, colIndex);
            const borderColor = mineTargetKeys.has(key)
              ? withAlpha(theme.difficultyExpert, isDark ? 0.84 : 0.68)
              : safeTargetKeys.has(key)
                ? withAlpha(theme.success, isDark ? 0.84 : 0.68)
                : evidenceKeys.has(key)
                  ? withAlpha(theme.primary, isDark ? 0.64 : 0.46)
                  : withAlpha(theme.gridCellBorder, isDark ? 0.84 : 0.62);
            const faceColor = mineTargetKeys.has(key)
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
                  <View style={styles.mineWrap}>
                    <View
                      style={[
                        styles.mineCore,
                        { backgroundColor: mineTargetKeys.has(key) ? theme.difficultyExpert : tokens.danger },
                      ]}
                    />
                    <View
                      style={[
                        styles.mineHorizontal,
                        { backgroundColor: mineTargetKeys.has(key) ? theme.difficultyExpert : tokens.danger },
                      ]}
                    />
                    <View
                      style={[
                        styles.mineVertical,
                        { backgroundColor: mineTargetKeys.has(key) ? theme.difficultyExpert : tokens.danger },
                      ]}
                    />
                  </View>
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
          {board.cells.map((row, rowIndex) => row.map((_, colIndex) => {
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
                accessibilityLabel={resolvedLanguage === 'nl'
                  ? `Cel ${rowIndex + 1}-${colIndex + 1}`
                  : `Cell ${rowIndex + 1}-${colIndex + 1}`}
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
  mineWrap: {
    width: '72%',
    height: '72%',
    alignItems: 'center',
    justifyContent: 'center',
  },
  mineCore: {
    position: 'absolute',
    width: '34%',
    height: '34%',
    borderRadius: 999,
  },
  mineHorizontal: {
    position: 'absolute',
    width: '72%',
    height: 2,
    borderRadius: 1,
  },
  mineVertical: {
    position: 'absolute',
    width: 2,
    height: '72%',
    borderRadius: 1,
  },
  hitCell: {
    position: 'absolute',
  },
});

export default React.memo(MinesweeperBoard);
