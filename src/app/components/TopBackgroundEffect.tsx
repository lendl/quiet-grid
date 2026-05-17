import React, { useMemo } from 'react';
import { StyleSheet } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { useTheme } from '../context/ThemeContext';
import { withAlpha } from '../utils/color';

type Props = {
  topOffset?: number;
};

export default function TopBackgroundEffect({ topOffset = 0 }: Props) {
  const { theme, isDark } = useTheme();
  const colors = useMemo<readonly [string, string, string]>(() => ([
    withAlpha(theme.primary, isDark ? 0.22 : 0.12),
    withAlpha(theme.primary, isDark ? 0.04 : 0.03),
    withAlpha(theme.primary, 0),
  ]), [isDark, theme.primary]);

  return (
    <LinearGradient
      pointerEvents="none"
      style={[styles.effect, { top: topOffset }]}
      colors={colors}
      locations={[0, 0.58, 1]}
      start={{ x: 0.5, y: 0 }}
      end={{ x: 0.5, y: 1 }}
    />
  );
}

const styles = StyleSheet.create({
  effect: {
    position: 'absolute',
    left: 0,
    right: 0,
    height: 240,
  },
});
