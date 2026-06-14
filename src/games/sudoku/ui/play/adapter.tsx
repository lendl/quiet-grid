import React, { useCallback, useMemo, useRef, useState } from 'react';
import type { LayoutChangeEvent } from 'react-native';
import { StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useLanguage } from '../../../../app/context/LanguageContext';
import { useTheme } from '../../../../app/context/ThemeContext';
import ZoomableBoardSurface from '../../../../app/components/ZoomableBoardSurface';
import {
  createPuzzlePlayAdapter,
  type PuzzleHeaderAction,
  type PuzzlePlayAdapter,
  type PuzzlePlayAdapterInstance,
  type PuzzlePlayAdapterShellArgs,
  type PuzzleRenderState,
} from '../../../../app/shell/games/playAdapter';
import type { BoardFeedbackEffect } from '../../../../app/shell/boardFeedback';
import { useNextMoveHelper } from '../../../../app/shell/games/useNextMoveHelper';
import type { Theme } from '../../../../app/theme';
import { getSudokuStrings } from '../../content/strings';
import {
  applySudokuAction,
  type SudokuValidationEffect,
} from '../../gameplay/actions';
import { getSudokuNextMoveHint } from '../../gameplay/analysis/nextMove';
import {
  getSudokuBoardFillSummary,
  sudokuPlayContract,
  type SudokuHudState,
  type SudokuPlaySession,
} from '../../gameplay/playContract';
import {
  getCompletedSudokuUnitStateForKey,
  getSudokuTouchedUnitKeys,
} from '../../gameplay/rules/validation';
import type {
  SudokuActiveSession,
  SudokuBoard,
  SudokuDigit,
  SudokuUnitKey,
} from '../../types';
import SudokuInputBar from './components/SudokuInputBar';
import SudokuPuzzleGrid from './components/SudokuPuzzleGrid';
import { buildSudokuCellsForUnitGroups } from './components/helpers';

const VALIDATION_DELAY_MS = 800;

type PendingValidation = {
  board: SudokuBoard;
  unitKeys: Set<SudokuUnitKey>;
};

function areSudokuBoardsEqual(left: SudokuBoard, right: SudokuBoard): boolean {
  return left.every((row, rowIndex) => row.every((cell, colIndex) => cell === right[rowIndex]?.[colIndex]));
}

function buildBoardFeedbackEffects(
  effect: SudokuValidationEffect | undefined,
  nextIdRef: React.MutableRefObject<number>,
): BoardFeedbackEffect[] | null {
  if (!effect) {
    return null;
  }

  const nextEffects: BoardFeedbackEffect[] = [];
  const correctCells = buildSudokuCellsForUnitGroups({
    rows: effect.correctRowIndexes,
    cols: effect.correctColIndexes,
    boxes: effect.correctBoxIndexes,
  });
  const incorrectCells = buildSudokuCellsForUnitGroups({
    rows: effect.incorrectRowIndexes,
    cols: effect.incorrectColIndexes,
    boxes: effect.incorrectBoxIndexes,
  });

  if (correctCells.length > 0) {
    nextIdRef.current += 1;
    nextEffects.push({
      id: `spin-${nextIdRef.current}`,
      kind: 'spin',
      cells: correctCells,
    });
  }

  if (incorrectCells.length > 0) {
    nextIdRef.current += 1;
    nextEffects.push({
      id: `shake-${nextIdRef.current}`,
      kind: 'shake',
      cells: incorrectCells,
    });
  }

  return nextEffects.length > 0 ? nextEffects : null;
}

function useSudokuAdapter({
  difficulty,
  goHome,
  setDialog,
}: PuzzlePlayAdapterShellArgs): PuzzlePlayAdapterInstance<SudokuPlaySession> {
  const { strings: appStrings, resolvedLanguage } = useLanguage();
  const { theme } = useTheme();
  const { bottom: safeAreaBottom } = useSafeAreaInsets();
  const strings = useMemo(() => getSudokuStrings(), [resolvedLanguage]);
  const styles = useMemo(() => makeStyles(theme), [theme]);
  const [gridContainer, setGridContainer] = useState({ width: 0, height: 0 });
  const [selectedCell, setSelectedCell] = useState<{ row: number; col: number } | null>(null);
  const [noteMode, setNoteMode] = useState(false);
  const [isBoardZoomed, setIsBoardZoomed] = useState(false);
  const [boardFeedbackEffects, setBoardFeedbackEffects] =
    useState<BoardFeedbackEffect[] | null>(null);
  const resetBoardZoomRef = useRef<(() => void) | null>(null);
  const validationRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const pendingValidationRef = useRef<PendingValidation | null>(null);
  const boardFeedbackEffectTokenRef = useRef(0);
  const nextMove = useNextMoveHelper((session: SudokuPlaySession) => getSudokuNextMoveHint(session));

  const resetAdapterState = useCallback(() => {
    nextMove.reset();
    setSelectedCell(null);
    setNoteMode(false);
    setGridContainer({ width: 0, height: 0 });
    setIsBoardZoomed(false);
    setBoardFeedbackEffects(null);
    resetBoardZoomRef.current = null;
    pendingValidationRef.current = null;
    boardFeedbackEffectTokenRef.current = 0;

    if (validationRef.current) {
      clearTimeout(validationRef.current);
      validationRef.current = null;
    }
  }, [nextMove.reset]);

  const onMissing = useCallback(() => {
    goHome();
  }, [goHome]);

  const onFreshMissing = useCallback(() => {
    const difficultyLabel = strings.difficultyLabels[difficulty];
    setDialog({
      title: strings.play.noPuzzlesDialog.title,
      message: strings.play.noPuzzlesDialog.message(difficultyLabel),
      buttons: [{ text: appStrings.common.goHome, onPress: goHome }],
    });
  }, [appStrings.common.goHome, difficulty, goHome, setDialog, strings]);

  const handleGridLayout = useCallback((event: LayoutChangeEvent) => {
    const { width, height } = event.nativeEvent.layout;
    setGridContainer({
      width: Math.max(0, width - 16),
      height: Math.max(0, height - 10),
    });
  }, []);

  const getState = useCallback(({
    session,
    setSession,
    loading,
    running,
    sessionRef,
    finishSolvedSession,
    completeExitToHome,
  }: PuzzleRenderState<SudokuPlaySession>) => {
    const commitSession = (nextSession: SudokuPlaySession) => {
      sessionRef.current = nextSession;
      setSession(nextSession);
    };
    const visibleSelectedCell = selectedCell && !session?.finishedCells[selectedCell.row][selectedCell.col]
      ? selectedCell
      : null;

    const getTouchedCompletedUnitKeys = (
      previousBoard: SudokuBoard,
      nextBoard: SudokuBoard,
      row: number,
      col: number,
      solution: SudokuPlaySession['puzzle']['solution'],
    ) => getSudokuTouchedUnitKeys(row, col).filter((unitKey) => {
      const previousState = getCompletedSudokuUnitStateForKey(
        previousBoard,
        solution,
        unitKey,
      );
      const nextState = getCompletedSudokuUnitStateForKey(
        nextBoard,
        solution,
        unitKey,
      );

      return previousState !== 'incomplete' || nextState !== 'incomplete';
    });

    const scheduleValidation = (board: SudokuBoard, unitKeys: readonly SudokuUnitKey[]) => {
      if (unitKeys.length === 0) {
        return;
      }

      if (validationRef.current) {
        clearTimeout(validationRef.current);
      }

      const pendingUnitKeys = pendingValidationRef.current
        ? new Set(pendingValidationRef.current.unitKeys)
        : new Set<SudokuUnitKey>();
      unitKeys.forEach((unitKey) => {
        pendingUnitKeys.add(unitKey);
      });

      pendingValidationRef.current = {
        board,
        unitKeys: pendingUnitKeys,
      };

      validationRef.current = setTimeout(() => {
        validationRef.current = null;
        const pendingValidation = pendingValidationRef.current;
        const currentSession = sessionRef.current;
        pendingValidationRef.current = null;

        if (!pendingValidation || !currentSession) {
          return;
        }

        const result = applySudokuAction(currentSession, {
          type: 'finalize-validation',
          board: pendingValidation.board,
          unitKeys: Array.from(pendingValidation.unitKeys),
        });
        const validationEffect = result.effects.find(
          (effect): effect is SudokuValidationEffect => effect.type === 'validated-units',
        );

        setBoardFeedbackEffects(buildBoardFeedbackEffects(
          validationEffect,
          boardFeedbackEffectTokenRef,
        ));
        commitSession(result.session);
        void finishSolvedSession(result.session);
      }, VALIDATION_DELAY_MS);
    };

    const handleCellPress = (row: number, col: number) => {
      const currentSession = sessionRef.current;
      if (!currentSession || !running) {
        return;
      }

      if (
        currentSession.puzzle.givens[row]?.[col] !== null
        || currentSession.finishedCells[row][col]
      ) {
        setSelectedCell(null);
        return;
      }

      setSelectedCell({ row, col });
    };

    const handlePressDigit = (digit: SudokuDigit) => {
      const currentSession = sessionRef.current;
      if (!currentSession || !running || !selectedCell) {
        return;
      }

      const selectedValue = currentSession.board[selectedCell.row][selectedCell.col];
      const result = noteMode
        ? applySudokuAction(currentSession, {
            type: 'toggle-cell-note',
            row: selectedCell.row,
            col: selectedCell.col,
            digit,
          })
        : applySudokuAction(currentSession, selectedValue === digit
          ? {
              type: 'clear-cell',
              row: selectedCell.row,
              col: selectedCell.col,
            }
          : {
              type: 'set-cell-digit',
              row: selectedCell.row,
              col: selectedCell.col,
              digit,
            });
      if (!result.changed) {
        return;
      }

      commitSession(result.session);
      const boardChanged = !areSudokuBoardsEqual(currentSession.board, result.session.board);
      if (boardChanged) {
        nextMove.reset();
      }

      if (noteMode) {
        return;
      }

      const touchedUnitKeys = getTouchedCompletedUnitKeys(
        currentSession.board,
        result.session.board,
        selectedCell.row,
        selectedCell.col,
        currentSession.puzzle.solution,
      );
      scheduleValidation(result.session.board, touchedUnitKeys);
    };

    const handleToggleNoteMode = () => {
      if (!visibleSelectedCell) {
        return;
      }

      setNoteMode((current) => !current);
    };

    const handleResetZoom = () => {
      resetBoardZoomRef.current?.();
    };

    const handleToggleNextMove = () => {
      const hint = nextMove.toggle(sessionRef.current);
      const placementTarget = hint?.kind === 'progress'
        ? hint.targetCells.find((cell) => cell.action === 'place')
        : null;

      if (placementTarget) {
        setSelectedCell({ row: placementTarget.row, col: placementTarget.col });
      }
    };

    const exitToHome = async () => {
      const currentSession = sessionRef.current;
      const pendingValidation = pendingValidationRef.current;

      if (validationRef.current) {
        clearTimeout(validationRef.current);
        validationRef.current = null;
      }
      pendingValidationRef.current = null;

      if (pendingValidation && currentSession) {
        const result = applySudokuAction(currentSession, {
          type: 'finalize-validation',
          board: pendingValidation.board,
          unitKeys: Array.from(pendingValidation.unitKeys),
        });
        const wonOnExit = await finishSolvedSession(result.session, false);

        if (wonOnExit) {
          goHome();
          return;
        }

        await completeExitToHome(result.session);
        goHome();
        return;
      }

      await completeExitToHome(currentSession);
      goHome();
    };

    const resetZoomHeaderAction: PuzzleHeaderAction = {
      key: 'reset-zoom',
      accessibilityLabel: strings.play.resetZoom,
      iconName: 'refresh-outline',
      onPress: handleResetZoom,
    };
    const nextMoveHeaderAction: PuzzleHeaderAction = {
      key: 'next-move',
      accessibilityLabel: nextMove.visible
        ? strings.play.helperToggle.hide
        : strings.play.helperToggle.show,
      iconName: nextMove.visible ? 'bulb' : 'bulb-outline',
      active: nextMove.visible,
      onPress: handleToggleNextMove,
      tooltipTitle: nextMove.hint?.title,
    };

    return {
      loading: loading || !session,
      exitToHome,
      headerActions: isBoardZoomed
        ? [resetZoomHeaderAction, nextMoveHeaderAction]
        : [nextMoveHeaderAction],
      headerMeta: session ? [
        {
          key: 'size',
          label: strings.play.metadataLabels.size,
          value: `${session.puzzle.rows}x${session.puzzle.cols}`,
        },
        {
          key: 'difficulty',
          label: strings.play.metadataLabels.difficulty,
          value: strings.difficultyLabels[session.puzzle.difficulty],
        },
        {
          key: 'filled',
          label: strings.play.metadataLabels.filled,
          value: getSudokuBoardFillSummary(session),
        },
      ] : [],
      main: session ? (() => {
        // Space the input bar midway between grid bottom and screen bottom.
        // gridContainer.height = boardArea.height - 10 (handleGridLayout subtraction).
        // boardContentHeight = boardArea.height - paddingTop(4) = gridContainer.height + 6.
        // footerInset = what footerInset View would be if footer is null.
        // Solving for equal gap: spacer = (boardContentHeight - inputH - gridW - 2*footerInset) / 3
        const footerInset = Math.max(10, safeAreaBottom + 4);
        const inputH = 52;
        const spacer = Math.max(0, (gridContainer.height + 6 - inputH - gridContainer.width - 2 * footerInset) / 3);

        return (
          <View style={styles.boardArea} onLayout={handleGridLayout}>
            <View style={styles.gridContainer}>
              {gridContainer.width > 0 && gridContainer.height > 0 ? (
                <ZoomableBoardSurface
                  panEnabled={isBoardZoomed}
                  onZoomStateChange={setIsBoardZoomed}
                  onRegisterReset={(reset) => {
                    resetBoardZoomRef.current = reset;
                  }}
                >
                  <SudokuPuzzleGrid
                    board={session.board}
                    givens={session.puzzle.givens}
                    notes={session.notes}
                    finishedCells={session.finishedCells}
                    selectedCell={visibleSelectedCell}
                    validatedUnitKeys={session.validatedUnitKeys}
                    penalizedUnitKeys={session.penalizedUnitKeys}
                    boardFeedbackEffects={boardFeedbackEffects}
                    nextMoveEvidenceCells={nextMove.hint?.evidenceCells ?? []}
                    nextMoveTargetCells={nextMove.hint?.targetCells ?? []}
                    nextMoveHighlightRows={nextMove.hint?.highlightRows ?? []}
                    nextMoveHighlightCols={nextMove.hint?.highlightCols ?? []}
                    nextMoveHighlightBoxes={nextMove.hint?.highlightBoxes ?? []}
                    containerWidth={gridContainer.width}
                    containerHeight={gridContainer.height}
                    onCellPress={handleCellPress}
                  />
                </ZoomableBoardSurface>
              ) : null}
            </View>
            <SudokuInputBar
              selectedCell={visibleSelectedCell}
              board={session.board}
              givens={session.puzzle.givens}
              notes={session.notes}
              noteMode={noteMode}
              onToggleNoteMode={handleToggleNoteMode}
              onPressDigit={handlePressDigit}
            />
            <View style={{ height: spacer }} />
          </View>
        );
      })() : (
        <View style={styles.boardArea} />
      ),
      footer: null,
    };
  }, [
    boardFeedbackEffects,
    goHome,
    gridContainer.height,
    gridContainer.width,
    handleGridLayout,
    isBoardZoomed,
    noteMode,
    nextMove,
    safeAreaBottom,
    selectedCell,
    strings,
    styles,
  ]);

  return {
    onMissing,
    onFreshMissing,
    onBeforeLoad: resetAdapterState,
    onCleanup: resetAdapterState,
    getState,
  };
}

const sudokuTypedPlayAdapter = {
  contract: sudokuPlayContract,
  useAdapter: useSudokuAdapter,
} satisfies PuzzlePlayAdapter<
  SudokuPlaySession,
  SudokuActiveSession,
  SudokuHudState
>;

export const sudokuPlayAdapter = createPuzzlePlayAdapter(sudokuTypedPlayAdapter);

const makeStyles = (theme: Theme) => StyleSheet.create({
  boardArea: {
    flex: 1,
    paddingHorizontal: 8,
    paddingTop: 4,
  },
  gridContainer: {
    flex: 1,
    justifyContent: 'center',
  },
  footerStack: {
    gap: 12,
  },
  footerSpacer: {
    flex: 1,
  },
});
