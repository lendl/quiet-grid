import React, { useCallback, useMemo, useRef, useState } from 'react';
import type { LayoutChangeEvent } from 'react-native';
import { StyleSheet, Text, View } from 'react-native';
import { useLanguage } from '../../app/context/LanguageContext';
import { useTheme } from '../../app/context/ThemeContext';
import { createPuzzlePlayAdapter } from '../../app/shell/games/playAdapter';
import type { Theme } from '../../app/theme';
import { withAlpha } from '../../app/utils/color';
import type {
  PuzzlePlayAdapter,
  PuzzlePlayAdapterInstance,
  PuzzlePlayAdapterShellArgs,
  PuzzleRenderState,
} from '../../app/shell/games/playAdapter';
import BinaryPuzzleGrid from './components/BinaryPuzzleGrid';
import { getBinaryNextMoveHint } from './learningCenter';
import type { CompletedLineState } from './validation';
import { getTouchedLineStates } from './validation';
import { applyBinaryAction } from './actions';
import {
  binaryPlayContract,
  type BinaryHudState,
  type BinaryPlaySession,
} from './playContract';
import type {
  BinaryNextMoveHint,
  Grid,
  LineKey,
} from './types';
import type { BinaryActivePuzzle } from './activePuzzle';

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

function useBinaryAdapter({
  difficulty,
  setDialog,
  goBack,
  goHome,
}: PuzzlePlayAdapterShellArgs): PuzzlePlayAdapterInstance<BinaryPlaySession> {
  const { resolvedLanguage, strings } = useLanguage();
  const { theme } = useTheme();
  const styles = useMemo(() => makeStyles(theme), [theme]);
  const [lineAnimationEventState, setLineAnimationEventState] =
    useState<LineAnimationEventState | null>(null);
  const [nextMoveHint, setNextMoveHint] = useState<BinaryNextMoveHint | null>(null);
  const [nextMoveVisible, setNextMoveVisible] = useState(false);
  const [gridContainer, setGridContainer] = useState({ width: 0, height: 0 });

  const validationRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const pendingValidationRef = useRef<PendingValidation | null>(null);
  const lineAnimationTokenRef = useRef(0);

  const resetAdapterState = useCallback(() => {
    setNextMoveHint(null);
    setNextMoveVisible(false);
    setLineAnimationEventState(null);
    pendingValidationRef.current = null;
    lineAnimationTokenRef.current = 0;
    if (validationRef.current) {
      clearTimeout(validationRef.current);
      validationRef.current = null;
    }
  }, []);

  const handleNoPuzzlesAvailable = useCallback(() => {
    goBack();
  }, [goBack]);

  const onFreshMissing = useCallback(() => {
    setDialog({
      title: resolvedLanguage === 'nl' ? 'Geen puzzels beschikbaar' : 'No puzzles available',
      message: resolvedLanguage === 'nl'
        ? `Geen puzzels gevonden in de ${difficulty}-catalogus.`
        : `No puzzles found in the ${difficulty} catalog.`,
      buttons: [{ text: strings.common.back, onPress: handleNoPuzzlesAvailable }],
    });
  }, [difficulty, handleNoPuzzlesAvailable, resolvedLanguage, setDialog, strings.common.back]);

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
  }: PuzzleRenderState<BinaryPlaySession>) => {
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

        const result = applyBinaryAction(currentSession, {
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

      const result = applyBinaryAction(currentSession, {
        type: 'press-cell',
        row,
        col,
      });
      if (!result.changed) {
        return;
      }

      sessionRef.current = result.session;
      setSession(result.session);
      setNextMoveHint(null);
      setNextMoveVisible(false);

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
      if (nextMoveVisible) {
        setNextMoveVisible(false);
        return;
      }
      if (!sessionRef.current) {
        return;
      }
      const suggestedNextMove = getBinaryNextMoveHint(sessionRef.current.board);
      setNextMoveHint(suggestedNextMove);
      setNextMoveVisible(true);
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

    return {
      loading: loading || !session,
      exitToHome,
      helperState: {
        showHelperToggle: true,
        helperVisible: nextMoveVisible,
        helperToggleLabel: nextMoveVisible
          ? (resolvedLanguage === 'nl' ? 'Verberg volgende zet' : 'Hide next move')
          : (resolvedLanguage === 'nl' ? 'Toon volgende zet' : 'Show next move'),
        onToggleHelper: handleToggleNextMove,
        footer: nextMoveVisible && nextMoveHint ? (
          <View style={styles.nextMoveCard}>
            <View style={styles.nextMoveCardHeader}>
              <View style={styles.nextMoveCardBadge}>
                <Text style={styles.nextMoveCardBadgeText}>i</Text>
              </View>
              <Text style={styles.nextMoveCardTitle}>{nextMoveHint.title}</Text>
            </View>
            <Text style={styles.nextMoveCardBody}>{nextMoveHint.body}</Text>
          </View>
        ) : (
          <View style={styles.footerSpacer} />
        ),
      },
      grid: (
        <View style={styles.gridArea} onLayout={handleGridLayout}>
          {session && gridContainer.width > 0 ? (
            <BinaryPuzzleGrid
              board={session.board}
              isGiven={session.isGiven}
              finishedCells={session.finishedCells}
              lineAnimationEvent={lineAnimationEvent}
              nextMoveEvidenceCells={nextMoveVisible ? nextMoveHint?.evidenceCells : []}
              nextMoveTargetCells={nextMoveVisible ? nextMoveHint?.targetCells : []}
              nextMoveHighlightRows={nextMoveVisible ? nextMoveHint?.highlightRows : []}
              nextMoveHighlightCols={nextMoveVisible ? nextMoveHint?.highlightCols : []}
              size={session.puzzle.size}
              onCellPress={handleCellPress}
              containerWidth={gridContainer.width}
              containerHeight={gridContainer.height}
            />
          ) : null}
        </View>
      ),
    };
  }, [
    nextMoveHint,
    nextMoveVisible,
    goHome,
    gridContainer.height,
    gridContainer.width,
    handleGridLayout,
    lineAnimationEventState,
    styles,
    resolvedLanguage,
  ]);

  return {
    onFreshMissing,
    onBeforeLoad: resetAdapterState,
    onCleanup: resetAdapterState,
    getState,
  };
}

const binaryTypedPlayAdapter = {
  contract: binaryPlayContract,
  useAdapter: useBinaryAdapter,
} satisfies PuzzlePlayAdapter<
  BinaryPlaySession,
  BinaryActivePuzzle,
  BinaryHudState
>;

export const binaryPlayAdapter = createPuzzlePlayAdapter(binaryTypedPlayAdapter);

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
