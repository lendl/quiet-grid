import React from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useTheme } from '../../../../../app/context/ThemeContext';
import type { ChimpTestCell } from '../../../types';

interface ChimpTestGridProps {
  cells: ChimpTestCell[];
  revealAll: boolean;
  wrongTapCell: number | null;
  gridSize: number;
  nextExpected: number;
  onCellTap: (row: number, col: number) => void;
  containerWidth: number;
  containerHeight: number;
}

const GAP = 6;
const WRONG_TAP_BORDER = '#ef4444';

export default function ChimpTestGrid({
  cells,
  revealAll,
  wrongTapCell,
  gridSize,
  nextExpected,
  onCellTap,
  containerWidth,
  containerHeight,
}: ChimpTestGridProps) {
  const { theme } = useTheme();

  const cellSize = Math.max(
    8,
    Math.floor(Math.min(
      (containerWidth - GAP * (gridSize - 1)) / gridSize,
      (containerHeight - GAP * (gridSize - 1)) / gridSize,
    )),
  );

  const gridSideLength = cellSize * gridSize + GAP * (gridSize - 1);
  const borderRadius = Math.max(6, Math.round(cellSize * 0.16));
  const fontSize = Math.max(10, Math.round(cellSize * 0.44));

  return (
    <View style={{ width: gridSideLength, height: gridSideLength }}>
      {cells.map((cell) => {
        const top = cell.row * (cellSize + GAP);
        const left = cell.col * (cellSize + GAP);

        // Correctly tapped cells disappear from the grid
        if (cell.hidden && !revealAll && cell.number < nextExpected) {
          return null;
        }

        // Memory phase: cell is hidden but still needs to be tapped — show as numberless square
        const isMemoryPhase = cell.hidden && !revealAll;
        // Wrong-tap reveal: show this specific cell with red border
        const isWrongCell = revealAll && cell.number === wrongTapCell;

        return (
          <TouchableOpacity
            key={cell.number}
            activeOpacity={0.65}
            onPress={() => onCellTap(cell.row, cell.col)}
            style={[
              styles.cellBase,
              styles.cellCard,
              {
                width: cellSize,
                height: cellSize,
                top,
                left,
                borderRadius,
                backgroundColor: theme.surface,
                borderColor: isWrongCell ? WRONG_TAP_BORDER : theme.border,
                borderWidth: isWrongCell ? 2 : 1.5,
              },
            ]}
          >
            {!isMemoryPhase && (
              <Text
                style={[styles.cellNumber, { fontSize, color: theme.text }]}
                allowFontScaling={false}
              >
                {cell.number}
              </Text>
            )}
          </TouchableOpacity>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  cellBase: {
    position: 'absolute',
  },
  cellCard: {
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.18,
    shadowRadius: 4,
    elevation: 3,
  },
  cellNumber: {
    fontWeight: '700',
  },
});
