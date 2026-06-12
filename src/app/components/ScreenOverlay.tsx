import React from 'react';
import { StyleSheet, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { useTheme } from '../context/ThemeContext';
import { withAlpha } from '../utils/color';

export default function ScreenOverlay() {
  const { theme, isDark } = useTheme();
  const startAlpha = isDark ? 0.22 : 0.12;
  const midAlpha = isDark ? 0.04 : 0.03;

  return (
    <View pointerEvents="none" style={styles.fill}>
      <LinearGradient
        style={[
          styles.veil,
        ]}
        colors={[
          withAlpha(theme.primary, startAlpha),
          withAlpha(theme.primary, midAlpha),
          withAlpha(theme.primary, 0),
        ]}
        locations={[0, 0.58, 1]}
        start={{ x: 0.5, y: 0 }}
        end={{ x: 0.5, y: 1 }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  fill: {
    ...StyleSheet.absoluteFill,
    zIndex: 2,
  },
  veil: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: 240,
  },
});
