import React from 'react';
import { Text, type StyleProp, type TextStyle } from 'react-native';

export const MINESWEEPER_MINE_GLYPH = '💣';

interface MinesweeperMineGlyphProps {
  style?: StyleProp<TextStyle>;
}

export default function MinesweeperMineGlyph({ style }: MinesweeperMineGlyphProps) {
  return <Text style={style}>{MINESWEEPER_MINE_GLYPH}</Text>;
}
