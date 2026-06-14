import React, { useMemo } from 'react';
import Ionicons from '@react-native-vector-icons/ionicons';
import { StyleSheet, Text, View } from 'react-native';
import { TouchableRipple } from 'react-native-paper';
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
  const { theme } = useTheme();
  const strings = useMemo(() => getSudokuStrings(), [resolvedLanguage]);
  const selectedValue = selectedCell ? board[selectedCell.row][selectedCell.col] : null;
  const selectedNotes = selectedCell ? notes[selectedCell.row][selectedCell.col] : null;
  const selectedGiven = selectedCell ? givens[selectedCell.row][selectedCell.col] !== null : false;
  const hasEditableSelection = Boolean(selectedCell && !selectedGiven);
  const digitsDisabled = !hasEditableSelection || (noteMode && selectedValue !== null);

  return (
    <View accessibilityRole="toolbar" style={styles.row}>
      <TouchableRipple
        accessibilityRole="button"
        accessibilityLabel={noteMode
          ? strings.play.controls.noteModeEnabled
          : strings.play.controls.noteModeDisabled}
        accessibilityState={{ selected: noteMode, disabled: !hasEditableSelection }}
        disabled={!hasEditableSelection}
        onPress={onToggleNoteMode}
        rippleColor={noteMode ? withAlpha(theme.onPrimary, 0.2) : undefined}
        style={[
          styles.btn,
          {
            backgroundColor: noteMode
              ? theme.primary
              : withAlpha(theme.surfaceElevated, 0.9),
            borderColor: noteMode
              ? theme.primary
              : withAlpha(theme.border, 0.5),
            opacity: hasEditableSelection ? 1 : 0.45,
          },
        ]}
      >
        <Ionicons
          name={noteMode ? 'create' : 'create-outline'}
          size={20}
          color={noteMode ? theme.onPrimary : theme.textSecondary}
        />
      </TouchableRipple>
      {sudokuDigits.map((digit) => {
        const active = noteMode
          ? Boolean(selectedNotes?.[digit - 1])
          : selectedValue === digit;
        const label = noteMode
          ? strings.play.controls.noteDigitLabel(digit)
          : strings.play.controls.digitButtonLabel(digit);

        return (
          <TouchableRipple
            key={digit}
            accessibilityRole="button"
            accessibilityLabel={label}
            accessibilityState={{ selected: active, disabled: digitsDisabled }}
            disabled={digitsDisabled}
            onPress={() => onPressDigit(digit)}
            rippleColor={active ? withAlpha(theme.onPrimary, 0.2) : undefined}
            style={[
              styles.btn,
              {
                backgroundColor: active
                  ? theme.primary
                  : withAlpha(theme.surfaceElevated, 0.9),
                borderColor: active
                  ? theme.primary
                  : withAlpha(theme.border, 0.5),
                opacity: digitsDisabled ? 0.45 : 1,
              },
            ]}
          >
            <Text style={[styles.digitText, { color: active ? theme.onPrimary : theme.text }]}>
              {digit}
            </Text>
          </TouchableRipple>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    gap: 4,
  },
  btn: {
    flex: 1,
    height: 52,
    borderRadius: 12,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  digitText: {
    fontSize: 20,
    fontWeight: '800',
  },
});
