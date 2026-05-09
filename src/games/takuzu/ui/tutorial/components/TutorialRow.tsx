import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import type { Grid } from '../../../types';
import { useTheme } from '../../../../../app/context/ThemeContext';
import type { Theme } from '../../../../../app/theme';
import { withAlpha } from '../../../../../app/utils/color';

interface TutorialRowProps {
  grid: Grid;
  focusCell: { row: number; col: number } | null;
  answerState: 'idle' | 'wrong' | 'correct';
}

export default function TutorialRow({ grid, focusCell, answerState }: TutorialRowProps) {
  const { theme } = useTheme();
  const s = makeStyles(theme);

  return (
    <View style={s.grid}>
      {grid.map((cells, row) => (
        <View key={row} style={s.row}>
          {cells.map((value, col) => {
            const isFocus = focusCell?.row === row && focusCell?.col === col;
            const isFilled = value !== null;
            const isWrongFocus = isFocus && answerState === 'wrong';
            const isCorrectFocus = isFocus && answerState === 'correct';

            return (
              <View
                key={col}
                style={[
                  s.cell,
                  isFilled ? s.filledCell : s.emptyCell,
                  isFocus ? s.focusCell : null,
                  isWrongFocus ? s.wrongFocusCell : null,
                  isCorrectFocus ? s.correctFocusCell : null,
                ]}
              >
                <Text
                  style={[
                    s.cellText,
                    isFilled ? s.filledText : s.emptyText,
                    isCorrectFocus ? s.correctFocusText : null,
                  ]}
                >
                  {value !== null ? String(value) : ''}
                </Text>
              </View>
            );
          })}
        </View>
      ))}
    </View>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  grid: {
    alignSelf: 'center',
    gap: 8,
    padding: 10,
    borderRadius: 18,
    backgroundColor: theme.surface,
    borderWidth: 1,
    borderColor: theme.border,
  },
  row: {
    flexDirection: 'row',
    gap: 8,
  },
  cell: {
    width: 42,
    height: 42,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
  },
  emptyCell: {
    backgroundColor: theme.surfaceElevated,
    borderColor: theme.border,
  },
  filledCell: {
    backgroundColor: theme.filledBackground,
    borderColor: theme.border,
  },
  focusCell: {
    borderWidth: 2,
    borderColor: theme.primary,
    backgroundColor: withAlpha(theme.primary, 0.1),
  },
  wrongFocusCell: {
    borderColor: theme.difficultyMedium,
    backgroundColor: withAlpha(theme.primary, 0.1),
  },
  correctFocusCell: {
    borderColor: theme.success,
    backgroundColor: withAlpha(theme.primary, 0.1),
  },
  cellText: {
    fontSize: 20,
    fontWeight: '800',
  },
  filledText: {
    color: theme.filled,
  },
  emptyText: {
    color: theme.textMuted,
  },
  correctFocusText: {
    color: theme.text,
  },
});
