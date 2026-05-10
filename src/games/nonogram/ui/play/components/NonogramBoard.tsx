import React, { useMemo, useState } from 'react';
import type { LayoutChangeEvent } from 'react-native';
import { Pressable, StyleSheet, View } from 'react-native';
import { useLanguage } from '../../../../../app/context/LanguageContext';
import { useTheme } from '../../../../../app/context/ThemeContext';
import type { Theme } from '../../../../../app/theme';
import { withAlpha } from '../../../../../app/utils/color';
import type { NonogramCellRef, NonogramCellState, NonogramPuzzle } from '../../../types';
import { cellIndex } from '../../../gameplay/rules/board';
import NonogramCell from './NonogramCell';
import NonogramClueRail from './NonogramClueRail';

interface NonogramBoardProps {
  puzzle: Pick<NonogramPuzzle, 'rows' | 'cols' | 'rowClues' | 'colClues'>;
  cells: readonly NonogramCellState[];
  onToggleCell?: (index: number) => void;
  nextMoveEvidenceCells?: readonly NonogramCellRef[];
  nextMoveTargetCells?: readonly NonogramCellRef[];
  nextMoveHighlightRows?: readonly number[];
  nextMoveHighlightCols?: readonly number[];
  interactive?: boolean;
}

const GRID_PADDING = 4;
const CELL_GAP = 2;
const BOARD_SECTION_GAP = 8;
const BOARD_CARD_PADDING = 12;
const ROW_CLUE_VALUE_GAP = 4;
const COL_CLUE_VALUE_GAP = 2;

function makeStyles(theme: Theme) {
  return StyleSheet.create({
    shell: {
      width: '100%',
      alignItems: 'center',
    },
    boardCard: {
      borderRadius: 24,
      padding: 12,
      backgroundColor: withAlpha(theme.surfaceElevated, 0.98),
      borderWidth: 1,
      borderColor: withAlpha(theme.border, 0.9),
      gap: 8,
    },
    topRow: {
      flexDirection: 'row',
      alignItems: 'flex-end',
      gap: 8,
    },
    bottomRow: {
      flexDirection: 'row',
      alignItems: 'flex-start',
      gap: 8,
    },
    rowRailSpacer: {
      minWidth: 20,
    },
    grid: {
      borderRadius: 16,
      padding: 4,
      backgroundColor: theme.gridFrame,
      overflow: 'hidden',
      gap: 2,
    },
    row: {
      flexDirection: 'row',
      gap: 2,
    },
  });
}

function toCellKey({ row, col }: NonogramCellRef): string {
  return `${row}:${col}`;
}

function NonogramBoard({
  puzzle,
  cells,
  onToggleCell,
  nextMoveEvidenceCells = [],
  nextMoveTargetCells = [],
  nextMoveHighlightRows = [],
  nextMoveHighlightCols = [],
  interactive = true,
}: NonogramBoardProps) {
  const { resolvedLanguage } = useLanguage();
  const { theme } = useTheme();
  const styles = useMemo(() => makeStyles(theme), [theme]);
  const [layoutWidth, setLayoutWidth] = useState(0);

  const evidenceKeys = useMemo(() => new Set(nextMoveEvidenceCells.map(toCellKey)), [nextMoveEvidenceCells]);
  const targetKeys = useMemo(() => new Set(nextMoveTargetCells.map(toCellKey)), [nextMoveTargetCells]);
  const highlightedRows = useMemo(() => new Set(nextMoveHighlightRows), [nextMoveHighlightRows]);
  const highlightedCols = useMemo(() => new Set(nextMoveHighlightCols), [nextMoveHighlightCols]);

  const rowClueDepth = Math.max(1, ...puzzle.rowClues.map((line) => (line.length === 1 && line[0] === 0 ? 0 : line.length)));
  const colClueDepth = Math.max(1, ...puzzle.colClues.map((line) => (line.length === 1 && line[0] === 0 ? 0 : line.length)));
  const outerWidth = Math.min(layoutWidth || 384, interactive ? 456 : 420);
  const minCellSize = interactive ? 30 : 24;
  const maxCellSize = interactive ? 44 : 38;
  const baseGridWidth = outerWidth
    - (BOARD_CARD_PADDING * 2)
    - BOARD_SECTION_GAP
    - GRID_PADDING * 2
    - Math.max(0, puzzle.cols - 1) * CELL_GAP;
  const estimatedCellSize = Math.max(
    minCellSize,
    Math.min(maxCellSize, Math.floor(baseGridWidth / (puzzle.cols + rowClueDepth * 0.72))),
  );
  const rowClueSlotSize = Math.max(20, Math.floor(estimatedCellSize * 0.72));
  const colClueSlotSize = Math.max(20, Math.floor(estimatedCellSize * 0.72));
  const rowRailWidth = rowClueDepth * rowClueSlotSize + Math.max(0, rowClueDepth - 1) * ROW_CLUE_VALUE_GAP;
  const gridInnerWidth = outerWidth
    - (BOARD_CARD_PADDING * 2)
    - rowRailWidth
    - BOARD_SECTION_GAP
    - GRID_PADDING * 2
    - Math.max(0, puzzle.cols - 1) * CELL_GAP;
  const cellSize = Math.max(minCellSize, Math.min(maxCellSize, Math.floor(gridInnerWidth / puzzle.cols)));

  const handleOuterLayout = (event: LayoutChangeEvent) => {
    setLayoutWidth(event.nativeEvent.layout.width);
  };

  return (
    <View style={styles.shell} onLayout={handleOuterLayout}>
      <View style={[styles.boardCard, { width: outerWidth }]}>
        <View style={styles.topRow}>
          <View style={[styles.rowRailSpacer, { width: rowRailWidth }]} />
          <NonogramClueRail
            clues={puzzle.colClues}
            axis="col"
            cellSize={cellSize}
            clueSlotSize={colClueSlotSize}
            lineGap={CELL_GAP}
            clueValueGap={COL_CLUE_VALUE_GAP}
            highlightedIndexes={[...highlightedCols]}
          />
        </View>
        <View style={styles.bottomRow}>
          <View style={{ width: rowRailWidth }}>
            <NonogramClueRail
              clues={puzzle.rowClues}
              axis="row"
              cellSize={cellSize}
              clueSlotSize={rowClueSlotSize}
              lineGap={CELL_GAP}
              clueValueGap={ROW_CLUE_VALUE_GAP}
              highlightedIndexes={[...highlightedRows]}
            />
          </View>
          <View>
            <View style={styles.grid}>
              {Array.from({ length: puzzle.rows }, (_, rowIndex) => (
                <View key={`row-${rowIndex}`} style={styles.row}>
                  {Array.from({ length: puzzle.cols }, (_, colIndex) => {
                    const index = cellIndex(rowIndex, colIndex, puzzle.cols);
                    const key = `${rowIndex}:${colIndex}`;
                    return (
                      <Pressable
                        key={key}
                        accessibilityRole={interactive ? 'button' : undefined}
                        accessibilityLabel={interactive
                          ? (resolvedLanguage === 'nl'
                            ? `Cel ${rowIndex + 1}-${colIndex + 1}`
                            : `Cell ${rowIndex + 1}-${colIndex + 1}`)
                          : undefined}
                        onPress={interactive ? () => onToggleCell?.(index) : undefined}
                      >
                        <NonogramCell
                          state={cells[index] ?? 'empty'}
                          size={cellSize}
                          lineHighlighted={highlightedRows.has(rowIndex) || highlightedCols.has(colIndex)}
                          evidence={evidenceKeys.has(key)}
                          target={targetKeys.has(key)}
                        />
                      </Pressable>
                    );
                  })}
                </View>
              ))}
            </View>
          </View>
        </View>
      </View>
    </View>
  );
}

export default React.memo(NonogramBoard);
