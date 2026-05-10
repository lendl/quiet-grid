import React, { useMemo } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useTheme } from '../../../../../app/context/ThemeContext';
import type { Theme } from '../../../../../app/theme';
import { withAlpha } from '../../../../../app/utils/color';
import type { NonogramCellState } from '../../../types';

interface NonogramCellProps {
  state: NonogramCellState;
  size: number;
  lineHighlighted: boolean;
  evidence: boolean;
  target: boolean;
}

function makeStyles(theme: Theme) {
  return StyleSheet.create({
    cell: {
      alignItems: 'center',
      justifyContent: 'center',
      borderRadius: 8,
      borderWidth: 1,
    },
    filledCell: {
      backgroundColor: theme.primary,
      borderColor: withAlpha(theme.primaryLight, 0.7),
    },
    emptyCell: {
      backgroundColor: theme.gridCellBackground,
      borderColor: theme.gridCellBorder,
    },
    markedCell: {
      backgroundColor: withAlpha(theme.text, 0.04),
      borderColor: withAlpha(theme.textSecondary, 0.35),
    },
    lineHighlight: {
      backgroundColor: withAlpha(theme.primary, 0.08),
    },
    evidenceCell: {
      borderColor: withAlpha(theme.primaryLight, 0.7),
      borderWidth: 2,
    },
    targetCell: {
      borderColor: withAlpha(theme.success, 0.85),
      borderWidth: 2,
      backgroundColor: withAlpha(theme.success, 0.12),
    },
    filledLabel: {
      color: theme.onPrimary,
      fontSize: 16,
      fontWeight: '800',
    },
    markedLabel: {
      color: theme.textSecondary,
      fontSize: 16,
      fontWeight: '800',
    },
  });
}

function NonogramCell({
  state,
  size,
  lineHighlighted,
  evidence,
  target,
}: NonogramCellProps) {
  const { theme } = useTheme();
  const styles = useMemo(() => makeStyles(theme), [theme]);

  const baseStyle = state === 'filled'
    ? styles.filledCell
    : state === 'marked'
      ? styles.markedCell
      : styles.emptyCell;

  return (
    <View
      style={[
        styles.cell,
        {
          width: size,
          height: size,
        },
        baseStyle,
        lineHighlighted ? styles.lineHighlight : null,
        evidence ? styles.evidenceCell : null,
        target ? styles.targetCell : null,
      ]}
    >
      {state === 'filled' ? <Text style={styles.filledLabel}>■</Text> : null}
      {state === 'marked' ? <Text style={styles.markedLabel}>✕</Text> : null}
    </View>
  );
}

export default React.memo(NonogramCell);
