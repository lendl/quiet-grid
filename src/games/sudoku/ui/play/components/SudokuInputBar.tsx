import React, { useMemo } from 'react';
import Ionicons from '@react-native-vector-icons/ionicons';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useLanguage } from '../../../../../app/context/LanguageContext';
import { useTheme } from '../../../../../app/context/ThemeContext';
import { withAlpha } from '../../../../../app/utils/color';
import { getSudokuStrings } from '../../../content/strings';
import { sudokuDigits, type SudokuBoard, type SudokuDigit, type SudokuNotes } from '../../../types';

interface SudokuInputBarProps {
  selectedCell: { row: number; col: number } | null;
  board: SudokuBoard;
  givens: SudokuBoard;
  notes: SudokuNotes;
  noteMode: boolean;
  onToggleNoteMode: () => void;
  onPressDigit: (digit: SudokuDigit) => void;
}

export default function SudokuInputBar({
  selectedCell,
  board,
  givens,
  notes,
  noteMode,
  onToggleNoteMode,
  onPressDigit,
}: SudokuInputBarProps) {
  const { resolvedLanguage } = useLanguage();
  const { theme, isDark } = useTheme();
  const strings = useMemo(() => getSudokuStrings(), [resolvedLanguage]);
  const selectedValue = selectedCell ? board[selectedCell.row][selectedCell.col] : null;
  const selectedNotes = selectedCell ? notes[selectedCell.row][selectedCell.col] : null;
  const selectedGiven = selectedCell ? givens[selectedCell.row][selectedCell.col] !== null : false;
  const hasEditableSelection = Boolean(selectedCell && !selectedGiven);
  const digitsDisabled = !hasEditableSelection || (noteMode && selectedValue !== null);
  const selectionText = selectedCell
    ? strings.play.controls.selectedCellLabel(strings.learning.labels.cell(selectedCell.row + 1, selectedCell.col + 1))
    : strings.play.controls.selectedCellPrompt;

  return (
    <View
      accessibilityRole="toolbar"
      style={[
        styles.card,
        {
          backgroundColor: withAlpha(theme.surfaceElevated, 0.96),
          borderColor: withAlpha(theme.primaryLight, 0.26),
        },
      ]}
    >
      <View style={styles.selectionHeader}>
        <Text style={[styles.selectionText, { color: theme.textSecondary }]}>
          {selectionText}
        </Text>
      </View>

      <View style={styles.controlsRow}>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={noteMode
            ? strings.play.controls.noteModeEnabled
            : strings.play.controls.noteModeDisabled}
          accessibilityState={{ selected: noteMode, disabled: !hasEditableSelection }}
          disabled={!hasEditableSelection}
          onPress={onToggleNoteMode}
          style={[
            styles.modeButton,
            {
              backgroundColor: noteMode
                ? theme.primary
                : withAlpha(theme.surface, isDark ? 0.52 : 0.72),
              borderColor: noteMode
                ? withAlpha(theme.primaryLight, 0.86)
                : withAlpha(theme.border, isDark ? 0.8 : 0.62),
              opacity: hasEditableSelection ? 1 : 0.45,
            },
          ]}
        >
          <Ionicons
            name={noteMode ? 'pencil' : 'pencil-outline'}
            size={18}
            color={noteMode ? theme.onPrimary : theme.text}
          />
        </Pressable>
        {sudokuDigits.map((digit) => {
          const active = noteMode
            ? Boolean(selectedNotes?.[digit - 1])
            : selectedValue === digit;
          const label = noteMode
            ? strings.play.controls.noteDigitLabel(digit)
            : strings.play.controls.digitButtonLabel(digit);

          return (
            <Pressable
              key={digit}
              accessibilityRole="button"
              accessibilityLabel={label}
              accessibilityState={{ selected: active, disabled: digitsDisabled }}
              disabled={digitsDisabled}
              onPress={() => onPressDigit(digit)}
              style={[
                styles.digitButton,
                {
                  backgroundColor: active
                    ? theme.primary
                    : withAlpha(theme.surface, isDark ? 0.52 : 0.72),
                  borderColor: active
                    ? withAlpha(theme.primaryLight, 0.86)
                    : withAlpha(theme.border, isDark ? 0.8 : 0.62),
                  opacity: digitsDisabled ? 0.45 : 1,
                },
              ]}
            >
              <Text
                style={[
                  styles.digitButtonText,
                  {
                    color: active ? theme.onPrimary : theme.text,
                  },
                ]}
              >
                {digit}
              </Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    borderRadius: 16,
    borderWidth: 1,
    paddingHorizontal: 12,
    paddingTop: 10,
    paddingBottom: 12,
    gap: 8,
  },
  selectionHeader: {
    paddingHorizontal: 2,
  },
  selectionText: {
    fontSize: 13,
    fontWeight: '700',
  },
  controlsRow: {
    flexDirection: 'row',
    gap: 6,
  },
  modeButton: {
    width: 40,
    minHeight: 36,
    borderWidth: 1,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  digitButton: {
    flex: 1,
    minHeight: 36,
    borderWidth: 1,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  digitButtonText: {
    fontSize: 14,
    fontWeight: '800',
  },
});
