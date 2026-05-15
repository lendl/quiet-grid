import React, { useMemo, useState } from 'react';
import type { LayoutChangeEvent } from 'react-native';
import { Pressable, StyleSheet, View } from 'react-native';
import type { MinesweeperBoard as MinesweeperBoardState } from '../../../types';
import { useLanguage } from '../../../../../app/context/LanguageContext';
import {
  createFixedGridLayout,
  getGridCellRect,
} from '../../../../../app/shell/skia/boardLayout';
import {
  getMinesweeperBoardCellSize,
  MINESWEEPER_BOARD_CELL_GAP,
  MINESWEEPER_FRAME_BORDER_WIDTH,
  MINESWEEPER_FRAME_PADDING,
} from './boardStyles';
import MinesweeperSkiaBoard from '../skia/MinesweeperSkiaBoard';

interface MinesweeperBoardProps {
  board: MinesweeperBoardState;
  onReveal: (row: number, col: number) => void;
  onToggleFlag: (row: number, col: number) => void;
  nextMoveEvidenceCells?: Array<{ row: number; col: number }>;
  nextMoveTargetCells?: Array<{ row: number; col: number }>;
  nextMoveSafeTargetCells?: Array<{ row: number; col: number }>;
  nextMoveMineTargetCells?: Array<{ row: number; col: number }>;
}

const MIN_CELL_SIZE = 32;
const MAX_CELL_SIZE = 40;

function MinesweeperBoard({
  board,
  onReveal,
  onToggleFlag,
  nextMoveEvidenceCells = [],
  nextMoveTargetCells = [],
  nextMoveSafeTargetCells = nextMoveTargetCells,
  nextMoveMineTargetCells = [],
}: MinesweeperBoardProps) {
  const { resolvedLanguage } = useLanguage();
  const [containerSize, setContainerSize] = useState({ width: 0, height: 0 });
  const cellSize = useMemo(() => {
    if (containerSize.width <= 0) {
      return MIN_CELL_SIZE;
    }

    return getMinesweeperBoardCellSize(
      containerSize.width,
      board.cols,
      MIN_CELL_SIZE,
      MAX_CELL_SIZE,
      board.rows,
      containerSize.height > 0 ? containerSize.height : undefined,
    );
  }, [board.cols, board.rows, containerSize.height, containerSize.width]);
  const layout = useMemo(() => createFixedGridLayout({
    rows: board.rows,
    cols: board.cols,
    cellSize,
    gap: MINESWEEPER_BOARD_CELL_GAP,
    padding: MINESWEEPER_FRAME_PADDING,
    borderWidth: MINESWEEPER_FRAME_BORDER_WIDTH,
  }), [board.cols, board.rows, cellSize]);

  function handleLayout(event: LayoutChangeEvent) {
    const { width, height } = event.nativeEvent.layout;
    setContainerSize({ width, height });
  }

  return (
    <View style={styles.shell} onLayout={handleLayout}>
      <View
        style={[
          styles.boardFrame,
          {
            width: layout.frameWidth,
            height: layout.frameHeight,
          },
        ]}
      >
        <MinesweeperSkiaBoard
          board={board}
          layout={layout}
          nextMoveEvidenceCells={nextMoveEvidenceCells}
          nextMoveSafeTargetCells={nextMoveSafeTargetCells}
          nextMoveMineTargetCells={nextMoveMineTargetCells}
        />
        <View pointerEvents="box-none" style={StyleSheet.absoluteFill}>
          {board.cells.map((row, rowIndex) => row.map((_, colIndex) => {
            const rect = getGridCellRect(layout, rowIndex, colIndex);

            return (
              <Pressable
                key={`${rowIndex}-${colIndex}`}
                style={[
                  styles.hitCell,
                  {
                    left: rect.x,
                    top: rect.y,
                    width: rect.width,
                    height: rect.height,
                  },
                ]}
                onPress={() => onReveal(rowIndex, colIndex)}
                onLongPress={() => onToggleFlag(rowIndex, colIndex)}
                delayLongPress={180}
                accessibilityRole="button"
                accessibilityLabel={resolvedLanguage === 'nl'
                  ? `Cel ${rowIndex + 1}-${colIndex + 1}`
                  : `Cell ${rowIndex + 1}-${colIndex + 1}`}
              />
            );
          }))}
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  shell: {
    width: '100%',
    alignItems: 'center',
  },
  boardFrame: {
    position: 'relative',
  },
  hitCell: {
    position: 'absolute',
  },
});

export default React.memo(MinesweeperBoard);
