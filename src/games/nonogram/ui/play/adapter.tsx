import React, { useCallback, useMemo, useRef, useState } from 'react';
import type { LayoutChangeEvent } from 'react-native';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { createPuzzlePlayAdapter } from '../../../../app/shell/games/playAdapter';
import { getAppStrings } from '../../../../app/i18n';
import { useTheme } from '../../../../app/context/ThemeContext';
import { useNextMoveHelper } from '../../../../app/shell/games/useNextMoveHelper';
import { clearActiveSessionState } from '../../../../app/utils/activeSessionStateStorage';
import { withAlpha } from '../../../../app/utils/color';
import type { Theme } from '../../../../app/theme';
import ZoomableBoardSurface from '../../../../app/components/ZoomableBoardSurface';
import type {
  PuzzleImmediateActionRunner,
  PuzzleHeaderAction,
  PuzzlePlayAdapter,
  PuzzlePlayAdapterInstance,
  PuzzlePlayAdapterShellArgs,
  PuzzleRenderState,
} from '../../../../app/shell/games/playAdapter';
import { getNonogramStrings } from '../../content/i18n';
import NonogramPuzzleGrid from './components/NonogramPuzzleGrid';
import { runNonogramAction, type NonogramAction } from '../../gameplay/actions';
import { getNonogramNextMoveHint } from '../../gameplay/analysis/nextMove';
import {
  nonogramPlayContract,
  type NonogramHudState,
  type NonogramPlaySession,
} from '../../gameplay/playContract';
import type { NonogramActiveSession } from '../../activePuzzle';

function useNonogramAdapter({
  difficulty,
  setDialog,
  goHome,
  goBack,
}: PuzzlePlayAdapterShellArgs): PuzzlePlayAdapterInstance<NonogramPlaySession, NonogramAction, never> {
  const { theme } = useTheme();
  const appStrings = getAppStrings();
  const strings = getNonogramStrings();
  const styles = useMemo(() => makeStyles(theme), [theme]);
  const nextMove = useNextMoveHelper((session: NonogramPlaySession) => (
    getNonogramNextMoveHint(session.puzzle, session.board)
  ));
  const [isBoardZoomed, setIsBoardZoomed] = useState(false);
  const [inputMode, setInputMode] = useState<'fill' | 'cross'>('fill');
  const resetBoardZoomRef = useRef<(() => void) | null>(null);
  const [gridContainer, setGridContainer] = useState({ width: 0, height: 0 });

  const resetAdapterState = useCallback(() => {
    nextMove.reset();
    setIsBoardZoomed(false);
    setInputMode('fill');
    resetBoardZoomRef.current = null;
  }, [nextMove.reset]);

  const handleMissingPuzzle = useCallback(async () => {
    resetAdapterState();
    await clearActiveSessionState();
    goHome();
  }, [goHome, resetAdapterState]);

  const onFreshMissing = useCallback(() => {
    const difficultyLabel = strings.difficultyLabels[difficulty];
    setDialog({
      title: strings.play.noPuzzlesDialog.title,
      message: strings.play.noPuzzlesDialog.message(difficultyLabel),
      buttons: [{ text: appStrings.common.back, onPress: goBack }],
    });
  }, [appStrings, difficulty, goBack, setDialog, strings]);

  const handleGridLayout = useCallback((event: LayoutChangeEvent) => {
    const { width, height } = event.nativeEvent.layout;
    setGridContainer({
      width: Math.max(0, width),
      height: Math.max(0, height),
    });
  }, []);

  const runImmediateAction = useMemo(() => ({
    run(session: NonogramPlaySession, action: NonogramAction) {
      const result = runNonogramAction(session, action);
      return {
        ...result,
        effects: [] as const,
      };
    },
  } satisfies PuzzleImmediateActionRunner<NonogramPlaySession, NonogramAction, never>), []);

  const getState = useCallback(({
    session,
    sessionRef,
    runImmediateAction,
  }: PuzzleRenderState<NonogramPlaySession, NonogramAction>) => {
    const nextMoveHeaderAction: PuzzleHeaderAction = {
      key: 'next-move',
      accessibilityLabel: nextMove.visible
        ? strings.play.helperToggle.hide
        : strings.play.helperToggle.show,
      iconName: nextMove.visible ? 'bulb' : 'bulb-outline',
      active: nextMove.visible,
      onPress: () => {
        nextMove.toggle(sessionRef.current);
      },
    };
    const resetZoomHeaderAction: PuzzleHeaderAction = {
      key: 'reset-zoom',
      accessibilityLabel: 'Reset zoom',
      iconName: 'refresh-outline',
      onPress: () => {
        resetBoardZoomRef.current?.();
      },
    };

    return {
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
      ] : [],
      main: session ? (
        <View style={styles.boardArea} onLayout={handleGridLayout}>
          {gridContainer.width > 0 && gridContainer.height > 0 ? (
            <ZoomableBoardSurface
              panEnabled={isBoardZoomed}
              onZoomStateChange={setIsBoardZoomed}
              onRegisterReset={(reset) => {
                resetBoardZoomRef.current = reset;
              }}
            >
              <NonogramPuzzleGrid
                puzzle={session.puzzle}
                board={session.board}
                containerWidth={gridContainer.width}
                containerHeight={gridContainer.height}
                interactive
                allowSwipe={!isBoardZoomed}
                inputMode={inputMode}
                onCellTap={(row, col) => {
                  nextMove.reset();
                  void runImmediateAction({ kind: 'tap', row, col, mode: inputMode });
                }}
                onCellSwipe={(cells, value) => {
                  nextMove.reset();
                  void runImmediateAction({ kind: 'swipe', cells, value });
                }}
                nextMoveEvidenceCells={nextMove.hint?.evidenceCells ?? []}
                nextMoveTargetCells={nextMove.hint?.targetCells ?? []}
                nextMoveHighlightRows={nextMove.hint?.lineOrientation === 'row' ? [nextMove.hint.lineIndex] : []}
                nextMoveHighlightCols={nextMove.hint?.lineOrientation === 'col' ? [nextMove.hint.lineIndex] : []}
              />
            </ZoomableBoardSurface>
          ) : null}
        </View>
      ) : (
        <View style={styles.boardArea} />
      ),
      footer: (
        <View style={styles.footer}>
          {nextMove.visible && nextMove.hint ? (
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
          )}
          <View style={styles.modeToggleRow}>
            <TouchableOpacity
              style={[styles.modeBtn, inputMode === 'cross' && styles.modeBtnActive]}
              onPress={() => setInputMode('cross')}
              activeOpacity={0.75}
            >
              <Text style={[
                styles.modeBtnCrossText,
                { color: inputMode === 'cross' ? theme.onPrimary : theme.textSecondary },
              ]}>
                ×
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.modeBtn, inputMode === 'fill' && styles.modeBtnActive]}
              onPress={() => setInputMode('fill')}
              activeOpacity={0.75}
            >
              <View style={[
                styles.modeBtnSquare,
                { backgroundColor: inputMode === 'fill' ? theme.onPrimary : theme.textSecondary },
              ]} />
            </TouchableOpacity>
          </View>
        </View>
      ),
    };
  }, [
    gridContainer.height,
    gridContainer.width,
    handleGridLayout,
    inputMode,
    isBoardZoomed,
    nextMove,
    strings,
    theme,
  ]);

  return {
    onMissing: handleMissingPuzzle,
    onFreshMissing,
    onBeforeLoad: resetAdapterState,
    onCleanup: resetAdapterState,
    runImmediateAction,
    getState,
  };
}

const nonogramTypedPlayAdapter = {
  contract: nonogramPlayContract,
  useAdapter: useNonogramAdapter,
} satisfies PuzzlePlayAdapter<
  NonogramPlaySession,
  NonogramActiveSession,
  NonogramHudState,
  NonogramAction,
  never
>;

export const nonogramPlayAdapter = createPuzzlePlayAdapter(nonogramTypedPlayAdapter);

const makeStyles = (theme: Theme) => StyleSheet.create({
  boardArea: {
    flex: 1,
    justifyContent: 'center',
    paddingHorizontal: 8,
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
  footer: {
    flex: 1,
    gap: 8,
  },
  footerSpacer: {
    flex: 1,
  },
  modeToggleRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 10,
  },
  modeBtn: {
    width: 52,
    height: 52,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: withAlpha(theme.surfaceElevated, 0.9),
    borderWidth: 1,
    borderColor: withAlpha(theme.border, 0.5),
  },
  modeBtnActive: {
    backgroundColor: theme.primary,
    borderColor: theme.primary,
  },
  modeBtnCrossText: {
    fontSize: 26,
    fontWeight: '900',
    lineHeight: 30,
    textAlign: 'center',
  },
  modeBtnSquare: {
    width: 20,
    height: 20,
    borderRadius: 4,
  },
});
