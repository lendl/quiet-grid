import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Animated,
  Easing,
  Pressable,
  StyleSheet,
  View,
} from 'react-native';
import { useLanguage } from '../../../../../app/context/LanguageContext';
import { useTheme } from '../../../../../app/context/ThemeContext';
import {
  createBoundedGridLayout,
  getGridCellRect,
} from '../../../../../app/shell/skia/boardLayout';
import { withAlpha } from '../../../../../app/utils/color';
import type {
  Grid,
  TakuzuNextMoveCell,
  TakuzuNextMoveTargetCell,
} from '../../../types';
import TakuzuSkiaBoard from '../skia/TakuzuSkiaBoard';

const FRAME_PADDING = 6;
const GRID_PADDING = 1;
const GAP = 1;
const SPIN_DURATION_MS = 420;

interface LineAnimationEvent {
  id: number;
  rows: number[];
  cols: number[];
}

interface PuzzleGridProps {
  board: Grid;
  isGiven: boolean[][];
  finishedCells: boolean[][];
  lineAnimationEvent: LineAnimationEvent | null;
  nextMoveEvidenceCells?: TakuzuNextMoveCell[];
  nextMoveTargetCells?: TakuzuNextMoveTargetCell[];
  nextMoveHighlightRows?: number[];
  nextMoveHighlightCols?: number[];
  size: number;
  onCellPress: (row: number, col: number) => void;
  containerWidth: number;
  containerHeight: number;
}

function buildSpinningKeys(event: LineAnimationEvent | null, size: number): Set<string> {
  const keys = new Set<string>();
  if (!event) {
    return keys;
  }

  event.rows.forEach((rowIndex) => {
    for (let colIndex = 0; colIndex < size; colIndex += 1) {
      keys.add(`${rowIndex}:${colIndex}`);
    }
  });

  event.cols.forEach((colIndex) => {
    for (let rowIndex = 0; rowIndex < size; rowIndex += 1) {
      keys.add(`${rowIndex}:${colIndex}`);
    }
  });

  return keys;
}

function TakuzuPuzzleGrid({
  board,
  isGiven,
  finishedCells,
  lineAnimationEvent,
  nextMoveEvidenceCells = [],
  nextMoveTargetCells = [],
  nextMoveHighlightRows = [],
  nextMoveHighlightCols = [],
  size,
  onCellPress,
  containerWidth,
  containerHeight,
}: PuzzleGridProps) {
  const { resolvedLanguage } = useLanguage();
  const { theme, isDark } = useTheme();
  const layout = useMemo(() => createBoundedGridLayout({
    containerWidth,
    containerHeight,
    rows: size,
    cols: size,
    gap: GAP,
    padding: FRAME_PADDING + GRID_PADDING,
    minCellSize: 1,
    maxCellSize: Number.MAX_SAFE_INTEGER,
  }), [containerHeight, containerWidth, size]);
  const rotation = useRef(new Animated.Value(0)).current;
  const spinTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [activeSpin, setActiveSpin] = useState<{
    id: number;
    keys: string[];
  } | null>(null);
  const rotate = useMemo(() => rotation.interpolate({
    inputRange: [0, 1],
    outputRange: ['0deg', '360deg'],
  }), [rotation]);

  useEffect(() => {
    if (!lineAnimationEvent) {
      return undefined;
    }

    const keys = Array.from(buildSpinningKeys(lineAnimationEvent, size));
    if (keys.length === 0) {
      return undefined;
    }

    setActiveSpin({
      id: lineAnimationEvent.id,
      keys,
    });
    if (spinTimeoutRef.current) {
      clearTimeout(spinTimeoutRef.current);
      spinTimeoutRef.current = null;
    }
    rotation.stopAnimation();
    rotation.setValue(0);
    const animation = Animated.timing(rotation, {
      toValue: 1,
      duration: SPIN_DURATION_MS,
      easing: Easing.inOut(Easing.ease),
      useNativeDriver: true,
    });

    animation.start();
    spinTimeoutRef.current = setTimeout(() => {
      setActiveSpin((current) => (current?.id === lineAnimationEvent.id ? null : current));
      rotation.stopAnimation();
      rotation.setValue(0);
      spinTimeoutRef.current = null;
    }, SPIN_DURATION_MS);

    return () => {
      animation.stop();
      if (spinTimeoutRef.current) {
        clearTimeout(spinTimeoutRef.current);
        spinTimeoutRef.current = null;
      }
    };
  }, [lineAnimationEvent, rotation, size]);

  const activeSpinKeys = useMemo(
    () => new Set(activeSpin?.keys ?? []),
    [activeSpin],
  );

  return (
    <View style={styles.boardArea}>
      <View
        style={[
          styles.boardFrame,
          {
            width: layout.frameWidth,
            height: layout.frameHeight,
          },
        ]}
      >
        <TakuzuSkiaBoard
          board={board}
          isGiven={isGiven}
          finishedCells={finishedCells}
          layout={layout}
          nextMoveEvidenceCells={nextMoveEvidenceCells}
          nextMoveTargetCells={nextMoveTargetCells}
          nextMoveHighlightRows={nextMoveHighlightRows}
          nextMoveHighlightCols={nextMoveHighlightCols}
          suppressedTextKeys={activeSpinKeys}
        />
        <View pointerEvents="box-none" style={StyleSheet.absoluteFill}>
          {board.map((row, rowIndex) => row.map((value, colIndex) => {
            const key = `${rowIndex}:${colIndex}`;
            const rect = getGridCellRect(layout, rowIndex, colIndex);
            const locked = isGiven[rowIndex][colIndex] || finishedCells[rowIndex][colIndex];
            const showSpin = activeSpinKeys.has(key) && value !== null;

            return (
              <React.Fragment key={key}>
                <Pressable
                  disabled={locked}
                  style={[
                    styles.hitCell,
                    {
                      left: rect.x,
                      top: rect.y,
                      width: rect.width,
                      height: rect.height,
                    },
                  ]}
                  accessibilityRole="button"
                  accessibilityLabel={resolvedLanguage === 'nl'
                    ? `Cel ${rowIndex + 1}-${colIndex + 1}`
                    : `Cell ${rowIndex + 1}-${colIndex + 1}`}
                  onPress={() => onCellPress(rowIndex, colIndex)}
                />
                {showSpin ? (
                  <Animated.View
                    pointerEvents="none"
                    style={[
                      styles.spinOverlay,
                      {
                        left: rect.x,
                        top: rect.y,
                        width: rect.width,
                        height: rect.height,
                        transform: [{ rotate }],
                      },
                    ]}
                  >
                    <Animated.Text
                      style={{
                        color: locked
                          ? withAlpha(theme.text, isDark ? 0.98 : 0.88)
                          : withAlpha(theme.text, isDark ? 0.98 : 0.88),
                        fontWeight: locked ? '800' : '700',
                        fontSize: layout.cellSize * 0.46,
                        fontFamily: 'monospace',
                      }}
                    >
                      {String(value)}
                    </Animated.Text>
                  </Animated.View>
                ) : null}
              </React.Fragment>
            );
          }))}
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  boardArea: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  boardFrame: {
    position: 'relative',
  },
  hitCell: {
    position: 'absolute',
  },
  spinOverlay: {
    position: 'absolute',
    alignItems: 'center',
    justifyContent: 'center',
  },
});

export default React.memo(TakuzuPuzzleGrid);
