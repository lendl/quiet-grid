import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  TouchableOpacity,
  StyleSheet,
  Animated,
  Easing,
  View,
  Text,
} from 'react-native';
import { useTheme } from '../../../../../app/context/ThemeContext';
import type { CellValue } from '../../../types';
import { withAlpha } from '../../../../../app/utils/color';

interface GridCellProps {
  value: CellValue;
  given: boolean;
  finished: boolean;
  cellSize: number;
  spinEventId: number | null;
  nextMoveLineHighlighted?: boolean;
  nextMoveEvidence?: boolean;
  nextMoveTarget?: boolean;
  onPress: () => void;
}

function GridCell({
  value,
  given,
  finished,
  cellSize,
  spinEventId,
  nextMoveLineHighlighted = false,
  nextMoveEvidence = false,
  nextMoveTarget = false,
  onPress,
}: GridCellProps) {
  const { theme, isDark } = useTheme();
  const locked = given || finished;
  const filled = value !== null;
  const rotation = useRef(new Animated.Value(0)).current;
  const [delayFinishedColor, setDelayFinishedColor] = useState(false);
  const safeCellSize = Math.max(cellSize, 1);
  const idleBackground = given || finished
    ? withAlpha(theme.surfaceElevated, isDark ? 0.92 : 0.68)
    : withAlpha(theme.background, isDark ? 0.94 : 1);

  const backgroundColor = nextMoveTarget
    ? withAlpha(theme.primary, isDark ? 0.28 : 0.18)
    : nextMoveEvidence
      ? withAlpha(theme.primary, isDark ? 0.16 : 0.1)
      : nextMoveLineHighlighted
        ? withAlpha(theme.primary, isDark ? 0.08 : 0.05)
        : idleBackground;
  const borderColor = nextMoveTarget
    ? withAlpha(theme.primaryLight, isDark ? 0.88 : 0.7)
    : nextMoveEvidence
      ? withAlpha(theme.primary, isDark ? 0.6 : 0.42)
      : nextMoveLineHighlighted
        ? withAlpha(theme.primary, isDark ? 0.28 : 0.2)
        : withAlpha(theme.border, isDark ? 0.48 : 0.62);
  const color = !filled
    ? theme.textMuted
    : given || (finished && !delayFinishedColor)
      ? withAlpha(theme.text, isDark ? 0.9 : 0.82)
      : withAlpha(theme.text, isDark ? 0.98 : 0.88);
  const fontWeight: '400' | '700' | '800' = !filled ? '400' : locked ? '800' : '700';
  const rotate = useMemo(() => rotation.interpolate({
    inputRange: [0, 1],
    outputRange: ['0deg', '360deg'],
  }), [rotation]);

  useEffect(() => {
    if (spinEventId === null || !filled) {
      if (!finished) {
        setDelayFinishedColor(false);
      }
      return;
    }

    setDelayFinishedColor(true);
    rotation.setValue(0);
    const animation = Animated.timing(rotation, {
      toValue: 1,
      duration: 420,
      easing: Easing.inOut(Easing.ease),
      useNativeDriver: true,
    });

    animation.start(({ finished: animationFinished }) => {
      if (animationFinished) {
        setDelayFinishedColor(false);
      }
    });

    return () => {
      animation.stop();
    };
  }, [filled, finished, rotation, spinEventId]);

  return (
    <TouchableOpacity
      disabled={locked}
      onPress={onPress}
      activeOpacity={locked ? 1 : 0.65}
      style={[styles.cell, {
        width: safeCellSize,
        height: safeCellSize,
        backgroundColor,
        borderColor,
        borderWidth: 1,
        borderRadius: Math.max(8, Math.floor(safeCellSize * 0.14)),
      }]}
    >
      {nextMoveTarget ? (
        <View
          pointerEvents="none"
          style={[styles.nextMoveBadge, {
            width: Math.max(16, Math.floor(safeCellSize * 0.34)),
            height: Math.max(16, Math.floor(safeCellSize * 0.34)),
            borderRadius: Math.max(8, Math.floor(safeCellSize * 0.17)),
            top: Math.max(2, Math.floor(safeCellSize * 0.05)),
            right: Math.max(2, Math.floor(safeCellSize * 0.05)),
            backgroundColor: theme.primary,
          }]}
        >
          <Text style={[styles.nextMoveBadgeText, {
            fontSize: Math.max(9, Math.floor(safeCellSize * 0.16)),
            color: theme.onPrimary,
          }]}
          >
            i
          </Text>
        </View>
      ) : null}
      <Animated.Text
        style={{
          color,
          fontWeight,
          fontSize: safeCellSize * 0.46,
          fontFamily: 'monospace',
          transform: [{ rotate }],
        }}
      >
        {filled ? String(value) : ''}
      </Animated.Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  cell: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  nextMoveBadge: {
    position: 'absolute',
    alignItems: 'center',
    justifyContent: 'center',
  },
  nextMoveBadgeText: {
    fontWeight: '900',
  },
});

export default React.memo(GridCell);
