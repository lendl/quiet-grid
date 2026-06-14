import React, { useCallback, useMemo, useRef, useState } from 'react';
import type { LayoutChangeEvent } from 'react-native';
import { StyleSheet, Text, View } from 'react-native';
import { Switch } from 'react-native-paper';
import { createPuzzlePlayAdapter } from '../../../../app/shell/games/playAdapter';
import { getAppStrings } from '../../../../app/i18n';
import { useTheme } from '../../../../app/context/ThemeContext';
import { useNextMoveHelper } from '../../../../app/shell/games/useNextMoveHelper';
import { clearActiveSessionState } from '../../../../app/utils/activeSessionStateStorage';
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
  const styles = useMemo(() => makeStyles(), []);
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
      width: Math.max(0, width - 16),
      height: Math.max(0, height - 10),
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
      tooltipTitle: nextMove.hint?.title,
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
        <View style={styles.switchRow}>
          <Text style={[styles.sideLabel, { color: inputMode === 'cross' ? theme.text : theme.textSecondary }]}>
            ×
          </Text>
          <Switch
            value={inputMode === 'fill'}
            onValueChange={(fill) => setInputMode(fill ? 'fill' : 'cross')}
            accessibilityLabel="Input mode: fill or empty"
          />
          <View style={[styles.fillSquare, { backgroundColor: inputMode === 'fill' ? theme.primary : theme.textSecondary }]} />
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

const makeStyles = () => StyleSheet.create({
  boardArea: {
    flex: 1,
    justifyContent: 'center',
    paddingHorizontal: 8,
    paddingTop: 4,
    paddingBottom: 6,
  },
  switchRow: {
    height: 52,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 14,
  },
  sideLabel: {
    fontSize: 26,
    fontWeight: '900',
    lineHeight: 30,
    width: 28,
    textAlign: 'center',
  },
  fillSquare: {
    width: 20,
    height: 20,
    borderRadius: 4,
  },
});
