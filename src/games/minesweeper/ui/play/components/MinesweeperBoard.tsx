import React, { useMemo, useState } from 'react';
import type { LayoutChangeEvent } from 'react-native';
import {
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import type { MinesweeperBoard as MinesweeperBoardState } from '../../../types';
import { useLanguage } from '../../../../../app/context/LanguageContext';
import { useTheme } from '../../../../../app/context/ThemeContext';
import type { Theme } from '../../../../../app/theme';
import { withAlpha } from '../../../../../app/utils/color';
import {
  buildSharedMinesweeperBoardStyles,
  getMinesweeperBoardCellSize,
  getMinesweeperNumberColor,
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

const makeStyles = (theme: Theme, cellSize: number) => {
  const shared = buildSharedMinesweeperBoardStyles(theme, cellSize);

  return StyleSheet.create({
    ...shared,
    mineCell: {
      backgroundColor: withAlpha(theme.difficultyExpert, 0.22),
    },
  });
};

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
  const [cellSize, setCellSize] = useState(MIN_CELL_SIZE);

  const styles = useMemo(() => makeStyles(theme, cellSize), [theme, cellSize]);
  const nextMoveEvidenceKeys = useMemo(() => new Set(
    nextMoveEvidenceCells.map(({ row, col }) => `${row}:${col}`),
  ), [nextMoveEvidenceCells]);
  const nextMoveSafeTargetKeys = useMemo(() => new Set(
    nextMoveSafeTargetCells.map(({ row, col }) => `${row}:${col}`),
  ), [nextMoveSafeTargetCells]);
  const nextMoveMineTargetKeys = useMemo(() => new Set(
    nextMoveMineTargetCells.map(({ row, col }) => `${row}:${col}`),
  ), [nextMoveMineTargetCells]);

  function handleLayout(event: LayoutChangeEvent) {
    const { width, height } = event.nativeEvent.layout;
    setCellSize(getMinesweeperBoardCellSize(width, board.cols, MIN_CELL_SIZE, MAX_CELL_SIZE, board.rows, height));
  }

  function getCellContent(row: number, col: number): string {
    const cell = board.cells[row][col];
    if (cell.state === 'flagged') return '🚩';
    if (cell.state === 'hidden') return '';
    if (cell.isMine) return '💣';
    if (cell.adjacentMines === 0) return '';
    return String(cell.adjacentMines);
  }

  function getCellStyle(row: number, col: number) {
    const cell = board.cells[row][col];
    const key = `${row}:${col}`;
    const nextMoveSafeTarget = nextMoveSafeTargetKeys.has(key);
    const nextMoveMineTarget = nextMoveMineTargetKeys.has(key);
    const nextMoveEvidence = nextMoveEvidenceKeys.has(key);
    const highlightStyle = nextMoveMineTarget
      ? {
          backgroundColor: withAlpha(theme.difficultyExpert, isDark ? 0.28 : 0.16),
          borderColor: withAlpha(theme.difficultyExpert, isDark ? 0.84 : 0.68),
        }
      : nextMoveSafeTarget
        ? {
            backgroundColor: withAlpha(theme.success, isDark ? 0.3 : 0.16),
            borderColor: withAlpha(theme.success, isDark ? 0.86 : 0.72),
          }
        : nextMoveEvidence
          ? {
              backgroundColor: withAlpha(theme.primary, isDark ? 0.14 : 0.08),
              borderColor: withAlpha(theme.primary, isDark ? 0.62 : 0.44),
            }
          : null;

    if (cell.state === 'flagged') {
      return [styles.flaggedCell, highlightStyle];
    }
    if (cell.state === 'revealed' && cell.isMine) {
      return [styles.mineCell, highlightStyle];
    }
    if (cell.state === 'revealed') {
      return [styles.revealedCell, highlightStyle];
    }

    return [styles.hiddenCell, highlightStyle];
  }

  function getCellTextStyle(row: number, col: number) {
    const cell = board.cells[row][col];
    const key = `${row}:${col}`;
    if (nextMoveMineTargetKeys.has(key)) return { color: theme.difficultyExpert };
    if (nextMoveSafeTargetKeys.has(key)) return { color: theme.success };
    if (cell.state === 'flagged') return { color: theme.primaryLight };
    if (cell.state === 'revealed' && cell.isMine) return { color: theme.difficultyExpert };
    if (cell.state === 'revealed' && cell.adjacentMines > 0) {
      return { color: getMinesweeperNumberColor(theme, cell.adjacentMines) };
    }
    return styles.emptyLabel;
  }

  return (
    <View onLayout={handleLayout} style={{ width: '100%' }}>
      <View style={styles.frame}>
        {board.cells.map((row, rowIndex) => (
          <View
            key={`row-${rowIndex}`}
            style={[styles.row, rowIndex === board.rows - 1 ? styles.lastRow : null]}
          >
            {row.map((_, colIndex) => (
              <Pressable
                key={`${rowIndex}-${colIndex}`}
                style={[styles.cell, ...getCellStyle(rowIndex, colIndex)]}
                onPress={() => onReveal(rowIndex, colIndex)}
                onLongPress={() => onToggleFlag(rowIndex, colIndex)}
                delayLongPress={180}
                accessibilityRole="button"
                accessibilityLabel={resolvedLanguage === 'nl'
                  ? `Cel ${rowIndex + 1}-${colIndex + 1}`
                  : `Cell ${rowIndex + 1}-${colIndex + 1}`}
              >
                <Text style={[styles.label, getCellTextStyle(rowIndex, colIndex)]}>
                  {getCellContent(rowIndex, colIndex)}
                </Text>
              </Pressable>
            ))}
          </View>
        ))}
      </View>
    </View>
  );
}

export default React.memo(MinesweeperBoard);
