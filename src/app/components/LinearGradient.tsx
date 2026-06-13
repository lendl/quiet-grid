import React from 'react';
import { View } from 'react-native';
import type { StyleProp, ViewStyle } from 'react-native';

type Props = {
  colors: readonly string[];
  locations?: readonly number[];
  start?: { x: number; y: number };
  end?: { x: number; y: number };
  style?: StyleProp<ViewStyle>;
  pointerEvents?: 'none' | 'box-none' | 'box-only' | 'auto';
  children?: React.ReactNode;
};

const STEPS = 16;

function parseRgba(color: string): [number, number, number, number] {
  const m = color.match(/rgba?\(\s*([\d.]+)\s*,\s*([\d.]+)\s*,\s*([\d.]+)(?:\s*,\s*([\d.]+))?\s*\)/);
  if (!m) return [0, 0, 0, 1];
  return [parseFloat(m[1]), parseFloat(m[2]), parseFloat(m[3]), m[4] !== undefined ? parseFloat(m[4]) : 1];
}

function lerp(a: number, b: number, t: number) {
  return a + (b - a) * t;
}

function sampleColor(colors: readonly string[], locs: readonly number[], t: number): string {
  let i = 0;
  while (i < locs.length - 2 && t >= locs[i + 1]) i++;
  const span = locs[i + 1] - locs[i];
  const segT = span === 0 ? 0 : (t - locs[i]) / span;
  const [r1, g1, b1, a1] = parseRgba(colors[i]);
  const [r2, g2, b2, a2] = parseRgba(colors[i + 1]);
  return `rgba(${Math.round(lerp(r1, r2, segT))},${Math.round(lerp(g1, g2, segT))},${Math.round(lerp(b1, b2, segT))},${lerp(a1, a2, segT).toFixed(3)})`;
}

export default function LinearGradient({ colors, locations, style, pointerEvents, children }: Props) {
  const locs = locations ?? colors.map((_, i) => i / (colors.length - 1));

  return (
    <View style={[style, { overflow: 'hidden' }]} pointerEvents={pointerEvents}>
      <View style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, flexDirection: 'column' }}>
        {Array.from({ length: STEPS }, (_, i) => (
          <View
            key={i}
            style={{ flex: 1, backgroundColor: sampleColor(colors, locs, i / (STEPS - 1)) }}
          />
        ))}
      </View>
      {children}
    </View>
  );
}
