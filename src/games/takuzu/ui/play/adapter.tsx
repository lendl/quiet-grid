import React, { useCallback, useMemo, useRef, useState } from 'react';
import type { LayoutChangeEvent } from 'react-native';
import { StyleSheet, Text, View } from 'react-native';
import { useLanguage } from '../../../../app/context/LanguageContext';
import { useTheme } from '../../../../app/context/ThemeContext';
import { createPuzzlePlayAdapter } from '../../../../app/shell/games/playAdapter';
import { useNextMoveHelper } from '../../../../app/shell/games/useNextMoveHelper';
import type { Theme } from '../../../../app/theme';
import { withAlpha } from '../../../../app/utils/color';
import ZoomableBoardSurface from '../../../../app/components/ZoomableBoardSurface';
import type {
  PuzzleHeaderAction,
  PuzzlePlayAdapter,
  PuzzlePlayAdapterInstance,
  PuzzlePlayAdapterShellArgs,
  PuzzleRenderState,
} from '../../../../app/shell/games/playAdapter';
import { getTakuzuStrings } from '../../content/strings';
import TakuzuPuzzleGrid from './components/TakuzuPuzzleGrid';
import { getTakuzuNextMoveHint } from '../../learningCenter';
import type { CompletedLineState } from '../../validation';
import { getTouchedLineStates } from '../../validation';
import { applyTakuzuAction } from '../../actions';
import {
  takuzuPlayContract,
  type TakuzuHudState,
  type TakuzuPlaySession,
} from '../../playContract';
import type {
  TakuzuNextMoveHint,
  Grid,
  LineKey,
} from '../../types';
import type { TakuzuActivePuzzle } from '../../activePuzzle';

const VALIDATION_DELAY_MS = 800;

type PendingValidation = {
  board: Grid;
  lineStates: Map<LineKey, CompletedLineState>;
};

type LineAnimationEventState = {
  token: number;
  rowIndexes: number[];
  colIndexes: number[];
};

function useTakuzuAdapter({
  difficulty,
  setDialog,
  goBack,
  goHome,
}: PuzzlePlayAdapterShellArgs): PuzzlePlayAdapterInstance<TakuzuPlaySession> {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const takuzuStrings = getTakuzuStrings();
  const styles = useMemo(() => makeStyles(theme), [theme]);
  const [lineAnimationEventState, setLineAnimationEventState] =
    useState<LineAnimationEventState | null>(null);
  const nextMove = useNextMoveHelper((session: TakuzuPlaySession) => (
    getTakuzuNextMoveHint(session.board)
  ));
  const [gridContainer, setGridContainer] = useState({ width: 0, height: 0 });
  const [isBoardZoomed, setIsBoardZoomed] = useState(false);
  const resetBoardZoomRef = useRef<(() => void) | null>(null);

  const validationRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const pendingValidationRef = useRef<PendingValidation | null>(null);
  const lineAnimationTokenRef = useRef(0);

  const resetAdapterState = useCallback(() => {
    nextMove.reset();
    setLineAnimationEventState(null);
    setIsBoardZoomed(false);
    resetBoardZoomRef.current = null;
    pendingValidationRef.current = null;
    lineAnimationTokenRef.current = 0;
    if (validationRef.current) {
      clearTimeout(validationRef.current);
      validationRef.current = null;
    }
  }, [nextMove.reset]);

  const handleNoPuzzlesAvailable = useCallback(() => {
    goBack();
  }, [goBack]);

  const onFreshMissing = useCallback(() => {
    const difficultyLabel = takuzuStrings.difficultyLabels[difficulty];
    setDialog({
      title: takuzuStrings.play.noPuzzlesDialog.title,
      message: takuzuStrings.play.noPuzzlesDialog.message(difficultyLabel),
      buttons: [{ text: strings.common.back, onPress: handleNoPuzzlesAvailable }],
    });
  }, [difficulty, handleNoPuzzlesAvailable, setDialog, strings.common.back, takuzuStrings]);

  const handleGridLayout = useCallback((e: LayoutChangeEvent) => {
    const { width, height } = e.nativeEvent.layout;
    setGridContainer({ width, height });
  }, []);

  const getState = useCallback(({
    session,
    setSession,
    loading,
    running,
    sessionRef,
    finishSolvedSession,
    completeExitToHome,
  }: PuzzleRenderState<TakuzuPlaySession>) => {
    const scheduleValidation = (
      newBoard: Grid,
      lineStateEntries: [LineKey, CompletedLineState][],
    ) => {
      if (validationRef.current) {
        clearTimeout(validationRef.current);
      }

      const pendingLineStates = pendingValidationRef.current
        ? new Map(pendingValidationRef.current.lineStates)
        : new Map<LineKey, CompletedLineState>();

      lineStateEntries.forEach(([lineKey, lineState]) => {
        if (!pendingLineStates.has(lineKey)) {
          pendingLineStates.set(lineKey, lineState);
        }
      });

      pendingValidationRef.current = {
        board: newBoard,
        lineStates: pendingLineStates,
      };

      validationRef.current = setTimeout(() => {
        validationRef.current = null;
        const pendingValidation = pendingValidationRef.current;
        const currentSession = sessionRef.current;
        pendingValidationRef.current = null;
        if (!currentSession || !pendingValidation) {
          return;
        }

        const result = applyTakuzuAction(currentSession, {
          type: 'finalize-validation',
          board: pendingValidation.board,
          lineKeys: Array.from(pendingValidation.lineStates.keys()),
        });
        const validationEffect = result.effects.find((effect) => effect.type === 'validated-lines');

        if (
          validationEffect
          && (validationEffect.correctRowIndexes.length > 0
            || validationEffect.correctColIndexes.length > 0)
        ) {
          lineAnimationTokenRef.current += 1;
          setLineAnimationEventState({
            token: lineAnimationTokenRef.current,
            rowIndexes: validationEffect.correctRowIndexes,
            colIndexes: validationEffect.correctColIndexes,
          });
        }

        sessionRef.current = result.session;
        setSession(result.session);

        void finishSolvedSession(result.session);
      }, VALIDATION_DELAY_MS);
    };

    const handleCellPress = (row: number, col: number) => {
      const currentSession = sessionRef.current;
      if (!currentSession || !running) {
        return;
      }

      const result = applyTakuzuAction(currentSession, {
        type: 'press-cell',
        row,
        col,
      });
      if (!result.changed) {
        return;
      }

      sessionRef.current = result.session;
      setSession(result.session);
      nextMove.reset();

      const { rowState, colState } = getTouchedLineStates(
        currentSession.board,
        currentSession.solution,
        row,
        col,
      );
      scheduleValidation(result.session.board, [
        [`r${row}`, rowState],
        [`c${col}`, colState],
      ]);
    };

    const handleToggleNextMove = () => {
      nextMove.toggle(sessionRef.current);
    };
    const handleResetZoom = () => {
      resetBoardZoomRef.current?.();
    };

    const exitToHome = async () => {
      const currentSession = sessionRef.current;
      const pendingBoard = pendingValidationRef.current?.board ?? null;

      if (validationRef.current) {
        clearTimeout(validationRef.current);
        validationRef.current = null;
      }
      pendingValidationRef.current = null;

      if (pendingBoard && currentSession) {
        const sessionWithPendingBoard = {
          ...currentSession,
          board: pendingBoard,
        };
        const wonOnExit = await finishSolvedSession(sessionWithPendingBoard, false);
        if (wonOnExit) {
          goHome();
          return;
        }

        await completeExitToHome(sessionWithPendingBoard);
        goHome();
        return;
      }

      await completeExitToHome(currentSession);
      goHome();
    };

    const lineAnimationEvent = lineAnimationEventState ? {
      id: lineAnimationEventState.token,
      rows: lineAnimationEventState.rowIndexes,
      cols: lineAnimationEventState.colIndexes,
    } : null;
    const metadata = session ? [
      {
        key: 'size',
        label: takuzuStrings.play.metadataLabels.size,
        value: `${session.puzzle.size}x${session.puzzle.size}`,
      },
      {
        key: 'difficulty',
        label: takuzuStrings.play.metadataLabels.difficulty,
        value: takuzuStrings.difficultyLabels[session.puzzle.difficulty],
      },
    ] : [];
    const nextMoveHeaderAction: PuzzleHeaderAction = {
      key: 'next-move',
      accessibilityLabel: nextMove.visible
        ? takuzuStrings.play.helperToggle.hide
        : takuzuStrings.play.helperToggle.show,
      iconName: nextMove.visible ? 'bulb' : 'bulb-outline',
      active: nextMove.visible,
      onPress: handleToggleNextMove,
    };
    const resetZoomHeaderAction: PuzzleHeaderAction = {
      key: 'reset-zoom',
      accessibilityLabel: 'Reset zoom',
      iconName: 'refresh-outline',
      onPress: handleResetZoom,
    };

    return {
      loading: loading || !session,
      exitToHome,
      headerActions: isBoardZoomed
        ? [resetZoomHeaderAction, nextMoveHeaderAction]
        : [nextMoveHeaderAction],
      headerMeta: metadata,
      footer: nextMove.visible && nextMove.hint ? (
        <View style={styles.nextMoveCard}>
          <View style={styles.nextMoveCardHeader}>
            <View style={styles.nextMoveCardBadge}>
              <Text style={styles.nextMoveCardBadgeText}>i</Text>
            </View>
            <Text style={styles.nextMoveCardTitle}>{nextMove.hint.title}</Text>
          </View>
          <Text style={styles.nextMoveCardBody}>{nextMove.hint.body}</Text>
        </View>
      ) : (
        <View style={styles.footerSpacer} />
      ),
      main: (
        <View style={styles.gridArea} onLayout={handleGridLayout}>
          {session && gridContainer.width > 0 ? (
            <ZoomableBoardSurface
              onZoomStateChange={setIsBoardZoomed}
              onRegisterReset={(reset) => {
                resetBoardZoomRef.current = reset;
              }}
            >
              <TakuzuPuzzleGrid
                board={session.board}
                isGiven={session.isGiven}
                finishedCells={session.finishedCells}
                lineAnimationEvent={lineAnimationEvent}
                nextMoveEvidenceCells={nextMove.hint?.evidenceCells ?? []}
                nextMoveTargetCells={nextMove.hint?.targetCells ?? []}
                nextMoveHighlightRows={nextMove.hint?.highlightRows ?? []}
                nextMoveHighlightCols={nextMove.hint?.highlightCols ?? []}
                size={session.puzzle.size}
                onCellPress={handleCellPress}
                containerWidth={gridContainer.width}
                containerHeight={gridContainer.height}
              />
            </ZoomableBoardSurface>
          ) : null}
        </View>
      ),
    };
  }, [
    isBoardZoomed,
    nextMove,
    goHome,
    gridContainer.height,
    gridContainer.width,
    handleGridLayout,
    lineAnimationEventState,
    styles,
    takuzuStrings,
  ]);

  return {
    onFreshMissing,
    onBeforeLoad: resetAdapterState,
    onCleanup: resetAdapterState,
    getState,
  };
}

const takuzuTypedPlayAdapter = {
  contract: takuzuPlayContract,
  useAdapter: useTakuzuAdapter,
} satisfies PuzzlePlayAdapter<
  TakuzuPlaySession,
  TakuzuActivePuzzle,
  TakuzuHudState
>;

export const takuzuPlayAdapter = createPuzzlePlayAdapter(takuzuTypedPlayAdapter);

const makeStyles = (theme: Theme) => StyleSheet.create({
  gridArea: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 6,
    paddingTop: 4,
    paddingBottom: 6,
  },
  nextMoveCard: {
    flex: 1,
    borderRadius: 16,
    paddingHorizontal: 12,
    paddingTop: 10,
    paddingBottom: 10,
    backgroundColor: withAlpha(theme.surfaceElevated, 0.96),
    borderWidth: 1,
    borderColor: withAlpha(theme.primaryLight, 0.34),
  },
  nextMoveCardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 4,
  },
  nextMoveCardBadge: {
    width: 22,
    height: 22,
    borderRadius: 999,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: theme.primary,
  },
  nextMoveCardBadgeText: {
    color: theme.onPrimary,
    fontSize: 12,
    fontWeight: '800',
  },
  nextMoveCardTitle: {
    flex: 1,
    color: theme.text,
    fontSize: 13,
    fontWeight: '800',
  },
  nextMoveCardBody: {
    color: theme.textSecondary,
    fontSize: 12,
    lineHeight: 17,
  },
  footerSpacer: {
    flex: 1,
  },
});
