import React, { useMemo } from 'react';
import {
  Animated,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useLanguage } from '../../../../../app/context/LanguageContext';
import { useTheme } from '../../../../../app/context/ThemeContext';
import {
  createBoundedGridLayout,
  getGridCellRect,
} from '../../../../../app/shell/boardLayout';
import type { BoardFeedbackEffect } from '../../../../../app/shell/boardFeedback';
import { createSharedBoardRenderTokens } from '../../../../../app/shell/renderTokens';
import { useBoardFeedbackAnimation } from '../../../../../app/shell/useBoardFeedbackAnimation';
import { withAlpha } from '../../../../../app/utils/color';
import { getSudokuStrings } from '../../../content/strings';
import type { SudokuHintTargetCell } from '../../../gameplay/analysis/nextMove';
import type { SudokuCellRef } from '../../../gameplay/analysis/moves';
import {
  SUDOKU_SIZE,
  sudokuDigits,
  type SudokuBoard,
  type SudokuDigit,
  type SudokuNotes,
  type SudokuUnitKey,
} from '../../../types';
import {
  buildSudokuCellKey,
} from './helpers';

const FRAME_PADDING = 8;
const GRID_PADDING = 4;
const GAP = 1;
const SHELL_INSET = 2;
const SHELL_RADIUS = 10;
const SHELL_INNER_RADIUS = 8;
const BOARD_INSET = 1;
const BOARD_RADIUS = 8;
const BOARD_SURFACE_RADIUS = 6;
const CELL_RADIUS = 5;
const CELL_FACE_INSET = 1;

interface SudokuPuzzleGridProps {
  board: SudokuBoard;
  givens: SudokuBoard;
  notes: SudokuNotes;
  finishedCells: boolean[][];
  selectedCell: { row: number; col: number } | null;
  validatedUnitKeys: readonly SudokuUnitKey[];
  penalizedUnitKeys: readonly SudokuUnitKey[];
  boardFeedbackEffects?: readonly BoardFeedbackEffect[] | null;
  interactive?: boolean;
  nextMoveEvidenceCells?: readonly SudokuCellRef[];
  nextMoveTargetCells?: readonly SudokuHintTargetCell[];
  nextMoveHighlightRows?: readonly number[];
  nextMoveHighlightCols?: readonly number[];
  nextMoveHighlightBoxes?: readonly number[];
  showPlacementTargetDigits?: boolean;
  containerWidth: number;
  containerHeight: number;
  onCellPress: (row: number, col: number) => void;
}

function SudokuPuzzleGrid({
  board,
  givens,
  notes,
  finishedCells,
  selectedCell,
  validatedUnitKeys,
  penalizedUnitKeys,
  boardFeedbackEffects,
  interactive = true,
  nextMoveEvidenceCells = [],
  nextMoveTargetCells = [],
  nextMoveHighlightRows = [],
  nextMoveHighlightCols = [],
  nextMoveHighlightBoxes = [],
  showPlacementTargetDigits = true,
  containerWidth,
  containerHeight,
  onCellPress,
}: SudokuPuzzleGridProps) {
  const { resolvedLanguage } = useLanguage();
  const { theme, isDark } = useTheme();
  const strings = useMemo(() => getSudokuStrings(), [resolvedLanguage]);
  const tokens = useMemo(() => createSharedBoardRenderTokens(theme, isDark), [theme, isDark]);
  const layout = useMemo(() => createBoundedGridLayout({
    containerWidth,
    containerHeight,
    rows: SUDOKU_SIZE,
    cols: SUDOKU_SIZE,
    gap: GAP,
    padding: FRAME_PADDING + GRID_PADDING,
    minCellSize: 18,
    maxCellSize: 56,
  }), [containerHeight, containerWidth]);
  const {
    activeSpinKeys,
    activeShakeKeys,
    rotate,
    shakeTranslateX,
  } = useBoardFeedbackAnimation({
    effects: boardFeedbackEffects,
    cellSize: layout.cellSize,
  });
  const evidenceKeys = useMemo(
    () => new Set(nextMoveEvidenceCells.map((cell) => buildSudokuCellKey(cell.row, cell.col))),
    [nextMoveEvidenceCells],
  );
  const placementTargets = useMemo(() => {
    const targets = new Map<string, SudokuDigit>();
    nextMoveTargetCells.forEach((cell) => {
      if (cell.action === 'place') {
        targets.set(buildSudokuCellKey(cell.row, cell.col), cell.digit);
      }
    });
    return targets;
  }, [nextMoveTargetCells]);
  const eliminationTargets = useMemo(() => {
    const targets = new Map<string, Set<SudokuDigit>>();
    nextMoveTargetCells.forEach((cell) => {
      if (cell.action !== 'eliminate') {
        return;
      }

      const key = buildSudokuCellKey(cell.row, cell.col);
      const digits = targets.get(key) ?? new Set<SudokuDigit>();
      digits.add(cell.digit);
      targets.set(key, digits);
    });
    return targets;
  }, [nextMoveTargetCells]);
  const highlightedRows = useMemo(() => new Set(nextMoveHighlightRows), [nextMoveHighlightRows]);
  const highlightedCols = useMemo(() => new Set(nextMoveHighlightCols), [nextMoveHighlightCols]);
  const highlightedBoxes = useMemo(() => new Set(nextMoveHighlightBoxes), [nextMoveHighlightBoxes]);
  const validatedUnitSets = useMemo(() => {
    const rows = new Set<number>();
    const cols = new Set<number>();
    const boxes = new Set<number>();
    validatedUnitKeys.forEach((key) => {
      const index = Number(key.slice(1));
      if (key[0] === 'r') rows.add(index);
      else if (key[0] === 'c') cols.add(index);
      else boxes.add(index);
    });
    return { rows, cols, boxes };
  }, [validatedUnitKeys]);
  const penalizedUnitSets = useMemo(() => {
    const rows = new Set<number>();
    const cols = new Set<number>();
    const boxes = new Set<number>();
    penalizedUnitKeys.forEach((key) => {
      const index = Number(key.slice(1));
      if (key[0] === 'r') rows.add(index);
      else if (key[0] === 'c') cols.add(index);
      else boxes.add(index);
    });
    return { rows, cols, boxes };
  }, [penalizedUnitKeys]);
  const separatorThickness = Math.max(4, Math.round(layout.cellSize * 0.1));
  const gridWidth = SUDOKU_SIZE * layout.cellSize + Math.max(0, SUDOKU_SIZE - 1) * GAP;
  const gridHeight = SUDOKU_SIZE * layout.cellSize + Math.max(0, SUDOKU_SIZE - 1) * GAP;
  const gridOriginX = layout.borderWidth + layout.padding;
  const gridOriginY = layout.borderWidth + layout.padding;
  const noteFontSize = Math.max(8, layout.cellSize * 0.18);
  const selectedCellKey = selectedCell
    ? buildSudokuCellKey(selectedCell.row, selectedCell.col)
    : null;

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
        <View pointerEvents="none" style={StyleSheet.absoluteFill}>
          <View
            style={[
              styles.shellBorder,
              { borderColor: withAlpha(theme.primaryLight, isDark ? 0.44 : 0.3) },
            ]}
          />
          <View
            style={[
              styles.shellFill,
              { backgroundColor: tokens.panelFill },
            ]}
          />
          <View
            style={[
              styles.boardBorder,
              { borderColor: withAlpha(theme.primary, isDark ? 0.28 : 0.18) },
            ]}
          />
          <View
            style={[
              styles.boardSurface,
              { backgroundColor: withAlpha(theme.panelSurfaceElevated, isDark ? 0.38 : 0.54) },
            ]}
          />

          {board.map((row, rowIndex) => row.map((value, colIndex) => {
            const key = buildSudokuCellKey(rowIndex, colIndex);
            const rect = getGridCellRect(layout, rowIndex, colIndex);
            const given = givens[rowIndex][colIndex] !== null;
            const finished = finishedCells[rowIndex][colIndex];
            const locked = given || finished;
            const cellNotes = notes[rowIndex][colIndex];
            const showShake = activeShakeKeys.has(key) && value !== null;
            const placementTargetDigit = placementTargets.get(key) ?? null;
            const eliminationDigits = eliminationTargets.get(key) ?? null;
            const cellBoxIndex = Math.floor(rowIndex / 3) * 3 + Math.floor(colIndex / 3);
            const isEvidenceCell = evidenceKeys.has(key);
            const isSelectedCell = selectedCellKey === key;
            const isHouseHighlighted = highlightedRows.has(rowIndex)
              || highlightedCols.has(colIndex)
              || highlightedBoxes.has(cellBoxIndex);
            const isInValidatedUnit = validatedUnitSets.rows.has(rowIndex)
              || validatedUnitSets.cols.has(colIndex)
              || validatedUnitSets.boxes.has(cellBoxIndex);
            const isInPenalizedUnit = penalizedUnitSets.rows.has(rowIndex)
              || penalizedUnitSets.cols.has(colIndex)
              || penalizedUnitSets.boxes.has(cellBoxIndex);
            const isPlacementTarget = placementTargetDigit !== null;
            const isEliminationTarget = Boolean(eliminationDigits && eliminationDigits.size > 0);
            const borderColor = isPlacementTarget
              ? withAlpha(theme.success, isDark ? 0.88 : 0.62)
              : isSelectedCell
                ? withAlpha(theme.primaryLight, isDark ? 0.96 : 0.72)
              : isEliminationTarget
                ? withAlpha(theme.difficultyHard, isDark ? 0.86 : 0.56)
                : isEvidenceCell
                  ? withAlpha(theme.primaryLight, isDark ? 0.96 : 0.7)
                  : isHouseHighlighted
                    ? withAlpha(theme.primaryLight, isDark ? 0.44 : 0.24)
                    : isInPenalizedUnit
                      ? withAlpha(theme.difficultyExpert, isDark ? 0.52 : 0.32)
                      : withAlpha(theme.border, isDark ? 0.84 : 0.66);
            const faceColor = isPlacementTarget
              ? withAlpha(theme.success, isDark ? 0.26 : 0.14)
              : isSelectedCell
                ? withAlpha(theme.primary, isDark ? 0.2 : 0.12)
              : isEliminationTarget
                ? withAlpha(theme.difficultyHard, isDark ? 0.18 : 0.1)
                : isEvidenceCell
                  ? withAlpha(theme.primary, isDark ? 0.28 : 0.18)
                  : isHouseHighlighted
                    ? withAlpha(theme.primary, isDark ? 0.08 : 0.04)
                    : isInPenalizedUnit
                      ? withAlpha(theme.difficultyExpert, isDark ? 0.16 : 0.09)
                      : isInValidatedUnit
                        ? withAlpha(theme.success, isDark ? 0.18 : 0.1)
                        : locked
                          ? tokens.cellSunkenFill
                          : tokens.cellRaisedFill;

            return (
              <View
                key={key}
                style={[
                  styles.cellFrame,
                  {
                    left: rect.x,
                    top: rect.y,
                    width: rect.width,
                    height: rect.height,
                    borderColor,
                    backgroundColor: locked ? tokens.cellSunkenFill : tokens.cellRaisedFill,
                  },
                ]}
              >
                <View
                  style={[
                    styles.cellFace,
                    {
                      backgroundColor: faceColor,
                    },
                  ]}
                />

                {value !== null && !activeSpinKeys.has(key) && !showShake ? (
                  <Text
                    style={[
                      styles.cellValue,
                      {
                        color: withAlpha(theme.text, locked ? (isDark ? 0.92 : 0.84) : (isDark ? 0.98 : 0.9)),
                        fontWeight: locked ? '800' : '700',
                        fontSize: layout.cellSize * 0.58,
                      },
                    ]}
                  >
                    {String(value)}
                  </Text>
                ) : null}

                {value === null && placementTargetDigit !== null && showPlacementTargetDigits ? (
                  <Text
                    style={[
                      styles.cellValue,
                      styles.ghostTargetValue,
                      {
                        color: withAlpha(theme.success, isDark ? 0.94 : 0.84),
                        fontSize: layout.cellSize * 0.58,
                      },
                    ]}
                  >
                    {String(placementTargetDigit)}
                  </Text>
                ) : null}

                {value === null && cellNotes.some(Boolean) ? (
                  <View style={styles.notesGrid}>
                    {sudokuDigits.map((digit, digitIndex) => (
                      <View key={digit} style={styles.noteSlot}>
                        {cellNotes[digitIndex] ? (
                          <Text
                            style={[
                              styles.noteValue,
                              {
                                color: eliminationDigits?.has(digit)
                                  ? theme.difficultyHard
                                  : theme.textMuted,
                                fontSize: noteFontSize,
                              },
                            ]}
                          >
                            {digit}
                          </Text>
                        ) : null}
                      </View>
                    ))}
                  </View>
                ) : null}
              </View>
            );
          }))}

          {[3, 6].map((index) => {
            const offset = index * layout.cellSize + Math.max(0, index - 1) * GAP;
            const separatorColor = withAlpha(theme.primaryLight, isDark ? 0.44 : 0.26);

            return (
              <React.Fragment key={index}>
                <View
                  style={[
                    styles.separator,
                    {
                      left: gridOriginX + offset - separatorThickness / 2,
                      top: gridOriginY,
                      width: separatorThickness,
                      height: gridHeight,
                      backgroundColor: separatorColor,
                    },
                  ]}
                />
                <View
                  style={[
                    styles.separator,
                    {
                      left: gridOriginX,
                      top: gridOriginY + offset - separatorThickness / 2,
                      width: gridWidth,
                      height: separatorThickness,
                      backgroundColor: separatorColor,
                    },
                  ]}
                />
              </React.Fragment>
            );
          })}
        </View>

        <View pointerEvents="box-none" style={StyleSheet.absoluteFill}>
          {board.map((row, rowIndex) => row.map((value, colIndex) => {
            const key = buildSudokuCellKey(rowIndex, colIndex);
            const rect = getGridCellRect(layout, rowIndex, colIndex);
            const given = givens[rowIndex][colIndex] !== null;
            const finished = finishedCells[rowIndex][colIndex];
            const locked = given || finished;
            const showSpin = activeSpinKeys.has(key) && value !== null;
            const showShake = activeShakeKeys.has(key) && value !== null;

            return (
              <React.Fragment key={key}>
                <Pressable
                  disabled={locked || !interactive}
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
                  accessibilityLabel={`${strings.play.cellLabel} ${rowIndex + 1}-${colIndex + 1}`}
                  onPress={() => onCellPress(rowIndex, colIndex)}
                />
                {showSpin || showShake ? (
                  <Animated.View
                    pointerEvents="none"
                    style={[
                      styles.valueOverlay,
                      {
                        left: rect.x,
                        top: rect.y,
                        width: rect.width,
                        height: rect.height,
                        transform: showShake ? [{ translateX: shakeTranslateX }] : [{ rotate }],
                      },
                    ]}
                  >
                    <Animated.Text
                      style={{
                        color: locked
                          ? withAlpha(theme.text, isDark ? 0.92 : 0.84)
                          : showShake
                            ? withAlpha(theme.difficultyExpert, isDark ? 0.98 : 0.88)
                            : withAlpha(theme.primaryLight, isDark ? 0.98 : 0.82),
                        fontWeight: locked ? '800' : '700',
                        fontSize: layout.cellSize * 0.58,
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
  shellBorder: {
    ...StyleSheet.absoluteFill,
    borderWidth: 1,
    borderRadius: SHELL_RADIUS,
  },
  shellFill: {
    ...StyleSheet.absoluteFill,
    top: SHELL_INSET,
    right: SHELL_INSET,
    bottom: SHELL_INSET,
    left: SHELL_INSET,
    borderRadius: SHELL_INNER_RADIUS,
  },
  boardBorder: {
    ...StyleSheet.absoluteFill,
    borderWidth: 1,
    borderRadius: BOARD_RADIUS,
  },
  boardSurface: {
    ...StyleSheet.absoluteFill,
    top: BOARD_INSET,
    right: BOARD_INSET,
    bottom: BOARD_INSET,
    left: BOARD_INSET,
    borderRadius: BOARD_SURFACE_RADIUS,
  },
  cellFrame: {
    position: 'absolute',
    borderWidth: 1,
    borderRadius: CELL_RADIUS,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cellFace: {
    ...StyleSheet.absoluteFill,
    margin: CELL_FACE_INSET,
    borderRadius: CELL_RADIUS - CELL_FACE_INSET,
  },
  cellValue: {
    fontFamily: 'monospace',
    textAlign: 'center',
  },
  ghostTargetValue: {
    position: 'absolute',
    fontWeight: '700',
  },
  notesGrid: {
    ...StyleSheet.absoluteFill,
    flexDirection: 'row',
    flexWrap: 'wrap',
    paddingHorizontal: 1,
    paddingVertical: 1,
  },
  noteSlot: {
    width: '33.333333%',
    height: '33.333333%',
    alignItems: 'center',
    justifyContent: 'center',
  },
  noteValue: {
    fontFamily: 'monospace',
    fontWeight: '600',
    textAlign: 'center',
  },
  separator: {
    position: 'absolute',
    borderRadius: 999,
  },
  hitCell: {
    position: 'absolute',
  },
  valueOverlay: {
    position: 'absolute',
    alignItems: 'center',
    justifyContent: 'center',
  },
});

export default React.memo(SudokuPuzzleGrid);
