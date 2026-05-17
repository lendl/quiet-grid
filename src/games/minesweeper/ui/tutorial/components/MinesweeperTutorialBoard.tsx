import React, { useMemo, useState } from 'react';
import type { LayoutChangeEvent } from 'react-native';
import {
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import type { MinesweeperBoard } from '../../../types';
import { useLanguage } from '../../../../../app/context/LanguageContext';
import { useTheme } from '../../../../../app/context/ThemeContext';
import type { Theme } from '../../../../../app/theme';
import {
  buildSharedMinesweeperBoardStyles,
  getMinesweeperBoardCellSize,
  getMinesweeperNumberColor,
} from '../../play/components/boardStyles';
import { MINESWEEPER_MINE_GLYPH } from '../../shared/mineGlyph';

interface FocusCell {
  row: number;
  col: number;
}

interface MinesweeperTutorialBoardProps {
  board: MinesweeperBoard;
  focusCell: FocusCell | null;
  answerState: 'idle' | 'wrong' | 'correct';
  onPressFocus: () => void;
}

const MIN_CELL_SIZE = 40;
const MAX_CELL_SIZE = 52;

const makeStyles = (theme: Theme, cellSize: number) => {
  const shared = buildSharedMinesweeperBoardStyles(theme, cellSize);

  return StyleSheet.create({
    ...shared,
    focusCell: {
      borderWidth: 2,
      borderColor: theme.primary,
    },
    wrongFocusCell: {
      borderColor: theme.difficultyMedium,
    },
    correctFocusCell: {
      borderColor: theme.success,
    },
  });
};

function getCellLabel(board: MinesweeperBoard, row: number, col: number): string {
  const cell = board.cells[row][col];
  if (cell.state === 'flagged') return '🚩';
  if (cell.state === 'hidden') return '';
  if (cell.isMine) return MINESWEEPER_MINE_GLYPH;
  if (cell.adjacentMines === 0) return '';
  return String(cell.adjacentMines);
}

export default function MinesweeperTutorialBoard({
  board,
  focusCell,
  answerState,
  onPressFocus,
}: MinesweeperTutorialBoardProps) {
  const { resolvedLanguage } = useLanguage();
  const { theme } = useTheme();
  const [cellSize, setCellSize] = useState(MIN_CELL_SIZE);
  const styles = useMemo(() => makeStyles(theme, cellSize), [theme, cellSize]);

  function handleLayout(event: LayoutChangeEvent) {
    const { width, height } = event.nativeEvent.layout;
    setCellSize(getMinesweeperBoardCellSize(width, board.cols, MIN_CELL_SIZE, MAX_CELL_SIZE, board.rows, height));
  }

  return (
    <View onLayout={handleLayout}>
      <View style={styles.frame}>
        {board.cells.map((row, rowIndex) => (
          <View
            key={`row-${rowIndex}`}
            style={[styles.row, rowIndex === board.rows - 1 ? styles.lastRow : null]}
          >
            {row.map((cell, colIndex) => {
              const isFocus = focusCell?.row === rowIndex && focusCell?.col === colIndex;
              const isWrongFocus = isFocus && answerState === 'wrong';
              const isCorrectFocus = isFocus && answerState === 'correct';
              const isHidden = cell.state === 'hidden';
              const isFlagged = cell.state === 'flagged';
              const textStyle = cell.state === 'revealed' && cell.adjacentMines > 0
                ? { color: getMinesweeperNumberColor(theme, cell.adjacentMines) }
                : isFlagged
                  ? { color: theme.primaryLight }
                  : styles.emptyLabel;

              return (
                <Pressable
                  key={`${rowIndex}-${colIndex}`}
                  accessibilityRole={isFocus ? 'button' : undefined}
                   accessibilityLabel={isFocus
                     ? (resolvedLanguage === 'nl' ? 'Gemarkeerd tutorialvak' : 'Highlighted tutorial tile')
                     : undefined}
                  onPress={isFocus ? onPressFocus : undefined}
                  style={[
                    styles.cell,
                    isHidden ? styles.hiddenCell : styles.revealedCell,
                    isFlagged ? styles.flaggedCell : null,
                    isFocus ? styles.focusCell : null,
                    isWrongFocus ? styles.wrongFocusCell : null,
                    isCorrectFocus ? styles.correctFocusCell : null,
                  ]}
                >
                  <Text style={[styles.label, textStyle]}>
                    {getCellLabel(board, rowIndex, colIndex)}
                  </Text>
                </Pressable>
              );
            })}
          </View>
        ))}
      </View>
    </View>
  );
}
