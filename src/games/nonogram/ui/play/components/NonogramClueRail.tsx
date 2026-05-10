import React, { useMemo } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useTheme } from '../../../../../app/context/ThemeContext';
import type { Theme } from '../../../../../app/theme';
import { withAlpha } from '../../../../../app/utils/color';
import type { NonogramAxis } from '../../../types';

interface NonogramClueRailProps {
  clues: readonly number[][];
  axis: NonogramAxis;
  cellSize: number;
  clueSlotSize: number;
  lineGap: number;
  clueValueGap: number;
  highlightedIndexes?: readonly number[];
}

function makeStyles(theme: Theme, lineGap: number, clueValueGap: number) {
  return StyleSheet.create({
    rail: {
      gap: lineGap,
    },
    rowRail: {
      justifyContent: 'flex-start',
    },
    colRail: {
      flexDirection: 'row',
      alignItems: 'flex-end',
    },
    rowLine: {
      flexDirection: 'row',
      justifyContent: 'flex-end',
      alignItems: 'center',
      gap: clueValueGap,
    },
    colLine: {
      justifyContent: 'flex-end',
      alignItems: 'center',
      gap: clueValueGap,
    },
    clueText: {
      color: theme.textSecondary,
      fontSize: 12,
      fontWeight: '700',
    },
    clueSlot: {
      alignItems: 'center',
      justifyContent: 'center',
      borderRadius: 6,
    },
    highlightedSlot: {
      backgroundColor: withAlpha(theme.primary, 0.12),
    },
  });
}

function padClues(clues: readonly number[], targetLength: number): (number | null)[] {
  const normalized = clues.length === 1 && clues[0] === 0 ? [] : [...clues];
  const padding = Array.from({ length: Math.max(0, targetLength - normalized.length) }, () => null);
  return [...padding, ...normalized];
}

function NonogramClueRail({
  clues,
  axis,
  cellSize,
  clueSlotSize,
  lineGap,
  clueValueGap,
  highlightedIndexes = [],
}: NonogramClueRailProps) {
  const { theme } = useTheme();
  const styles = useMemo(
    () => makeStyles(theme, lineGap, clueValueGap),
    [clueValueGap, lineGap, theme],
  );
  const highlighted = useMemo(() => new Set(highlightedIndexes), [highlightedIndexes]);
  const maxDepth = Math.max(1, ...clues.map((line) => (line.length === 1 && line[0] === 0 ? 0 : line.length)));

  if (axis === 'row') {
    return (
      <View style={[styles.rail, styles.rowRail]}>
        {clues.map((line, lineIndex) => (
          <View key={`row-${lineIndex}`} style={styles.rowLine}>
            {padClues(line, maxDepth).map((value, clueIndex) => (
              <View
                key={`${lineIndex}-${clueIndex}`}
                style={[
                  styles.clueSlot,
                  {
                    width: clueSlotSize,
                    height: cellSize,
                  },
                  highlighted.has(lineIndex) ? styles.highlightedSlot : null,
                ]}
              >
                <Text style={styles.clueText}>{value ?? ''}</Text>
              </View>
            ))}
          </View>
        ))}
      </View>
    );
  }

  return (
    <View style={[styles.rail, styles.colRail]}>
      {clues.map((line, lineIndex) => (
        <View key={`col-${lineIndex}`} style={styles.colLine}>
          {padClues(line, maxDepth).map((value, clueIndex) => (
            <View
              key={`${lineIndex}-${clueIndex}`}
              style={[
                  styles.clueSlot,
                  {
                    width: cellSize,
                    height: clueSlotSize,
                  },
                highlighted.has(lineIndex) ? styles.highlightedSlot : null,
              ]}
            >
              <Text style={styles.clueText}>{value ?? ''}</Text>
            </View>
          ))}
        </View>
      ))}
    </View>
  );
}

export default React.memo(NonogramClueRail);
