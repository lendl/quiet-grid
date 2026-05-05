import React from 'react';
import { StyleSheet, View } from 'react-native';
import { useTheme } from '../context/ThemeContext';
import type { Theme } from '../theme';

export default function GridHomeIcon() {
  const { theme } = useTheme();
  const s = makeStyles(theme);

  return (
    <View style={s.frame}>
      <View style={s.roofWrap}>
        <View style={s.roofLeft} />
        <View style={s.roofRight} />
      </View>

      <View style={s.grid}>
        <View style={[s.tile, s.tilePrimary]} />
        <View style={[s.tile, s.tileAccent]} />
        <View style={[s.tile, s.tileText]} />
        <View style={[s.tile, s.tileAccent]} />
      </View>
    </View>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  frame: {
    width: 24,
    height: 24,
    alignItems: 'center',
  },
  roofWrap: {
    position: 'absolute',
    top: 1,
    width: 20,
    height: 10,
  },
  roofLeft: {
    position: 'absolute',
    left: 1,
    top: 4,
    width: 10,
    height: 2,
    borderRadius: 999,
    backgroundColor: theme.text,
    transform: [{ rotate: '-35deg' }],
  },
  roofRight: {
    position: 'absolute',
    right: 1,
    top: 4,
    width: 10,
    height: 2,
    borderRadius: 999,
    backgroundColor: theme.text,
    transform: [{ rotate: '35deg' }],
  },
  grid: {
    position: 'absolute',
    top: 11,
    width: 17,
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 3,
    justifyContent: 'center',
  },
  tile: {
    width: 7,
    height: 7,
    borderRadius: 2,
  },
  tilePrimary: {
    backgroundColor: theme.primary,
  },
  tileAccent: {
    backgroundColor: theme.primaryLight,
    opacity: 0.55,
  },
  tileText: {
    backgroundColor: theme.text,
  },
});
