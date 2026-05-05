import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useTheme } from '../context/ThemeContext';
import type { Theme } from '../theme';

interface PuzzleStatusBarProps {
  leftLabel: string;
  rightLabel?: string;
}

function PuzzleStatusBar({ leftLabel, rightLabel }: PuzzleStatusBarProps) {
  const { theme } = useTheme();
  const s = makeStyles(theme);

  return (
    <View style={s.row}>
      <Text style={s.leftLabel}>{leftLabel}</Text>
      {rightLabel ? <Text style={s.rightLabel}>{rightLabel}</Text> : null}
    </View>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingBottom: 10,
  },
  leftLabel: {
    fontSize: 16,
    fontWeight: '700',
    color: theme.text,
  },
  rightLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: theme.textSecondary,
  },
});

export default React.memo(PuzzleStatusBar);
