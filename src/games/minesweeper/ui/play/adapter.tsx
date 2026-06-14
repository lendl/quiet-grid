import React, { useCallback, useMemo, useRef, useState } from 'react';
import type { LayoutChangeEvent } from 'react-native';
import { StyleSheet, View } from 'react-native';
import { createPuzzlePlayAdapter } from '../../../../app/shell/games/playAdapter';
import { useNextMoveHelper } from '../../../../app/shell/games/useNextMoveHelper';
import { getMinesweeperStrings } from '../../content/strings';
import {
  clearActiveSessionState,
} from '../../../../app/utils/activeSessionStateStorage';
import ZoomableBoardSurface from '../../../../app/components/ZoomableBoardSurface';
import type {
  PuzzleEffectHandlerArgs,
  PuzzleHeaderAction,
  PuzzlePlayAdapter,
  PuzzlePlayAdapterInstance,
  PuzzlePlayAdapterShellArgs,
  PuzzleRenderState,
} from '../../../../app/shell/games/playAdapter';
import MinesweeperBoard from './components/MinesweeperBoard';
import { applyMinesweeperAction } from '../../gameplay/actions';
import { getMinesweeperNextMoveHint } from '../../gameplay/analysis';
import type { MinesweeperActiveSession } from '../../gameplay/activePuzzle';
import { countFlaggedCells } from '../../gameplay/rules';
import {
  minesweeperPlayContract,
  type MinesweeperAction,
  type MinesweeperActionEffect,
  type MinesweeperHudState,
  type MinesweeperPlaySession,
} from '../../gameplay/playContract';

const GRID_HORIZONTAL_PADDING = 12;
const GRID_BOTTOM_PADDING = 24;

function useMinesweeperAdapter({
  goHome,
}: PuzzlePlayAdapterShellArgs): PuzzlePlayAdapterInstance<
  MinesweeperPlaySession,
  MinesweeperAction,
  MinesweeperActionEffect
> {
  const minesweeperStrings = getMinesweeperStrings();
  const styles = useMemo(() => makeStyles(), []);
  const nextMove = useNextMoveHelper((session: MinesweeperPlaySession) => (
    getMinesweeperNextMoveHint(session.board)
  ));
  const [isBoardZoomed, setIsBoardZoomed] = useState(false);
  const resetBoardZoomRef = useRef<(() => void) | null>(null);
  const [gridContainer, setGridContainer] = useState({ width: 0, height: 0 });

  const resetHelperState = useCallback(() => {
    nextMove.reset();
    setIsBoardZoomed(false);
    resetBoardZoomRef.current = null;
  }, [nextMove.reset]);

  const handleMissingPuzzle = useCallback(async () => {
    resetHelperState();
    await clearActiveSessionState();
    goHome();
  }, [goHome, resetHelperState]);

  const runImmediateAction = useMemo(() => ({
    run(session: MinesweeperPlaySession, action: MinesweeperAction) {
      return applyMinesweeperAction(session, action);
    },
  }), []);

  const handleEffects = useCallback(async ({
    effects,
    previousSession,
    finalizedRef,
    finishLossSession,
  }: PuzzleEffectHandlerArgs<MinesweeperPlaySession, MinesweeperActionEffect>) => {
    if (!effects.some((effect) => effect.type === 'lost') || finalizedRef.current) {
      return;
    }

    await finishLossSession('rule-failure', previousSession);
  }, []);

  const handleGridLayout = useCallback((event: LayoutChangeEvent) => {
    const { width, height } = event.nativeEvent.layout;
    setGridContainer({
      width: Math.max(0, width - GRID_HORIZONTAL_PADDING * 2),
      height: Math.max(0, height - GRID_BOTTOM_PADDING),
    });
  }, []);

  const getState = useCallback(({
    session,
    sessionRef,
    runImmediateAction: runShellAction,
  }: PuzzleRenderState<MinesweeperPlaySession, MinesweeperAction>) => {
    const resetNextMove = () => {
      nextMove.reset();
    };

    const handleToggleNextMove = () => {
      nextMove.toggle(sessionRef.current);
    };
    const handleResetZoom = () => {
      resetBoardZoomRef.current?.();
    };
    const nextMoveHeaderAction: PuzzleHeaderAction = {
      key: 'next-move',
      accessibilityLabel: nextMove.visible
        ? minesweeperStrings.play.helperToggle.hide
        : minesweeperStrings.play.helperToggle.show,
      iconName: nextMove.visible ? 'bulb' : 'bulb-outline',
      active: nextMove.visible,
      onPress: handleToggleNextMove,
      tooltipTitle: nextMove.hint?.title,
    };
    const resetZoomHeaderAction: PuzzleHeaderAction = {
      key: 'reset-zoom',
      accessibilityLabel: 'Reset zoom',
      iconName: 'refresh-outline',
      onPress: handleResetZoom,
    };

    return {
      headerActions: isBoardZoomed
        ? [resetZoomHeaderAction, nextMoveHeaderAction]
        : [nextMoveHeaderAction],
      headerMeta: session ? [
        {
          key: 'size',
          label: minesweeperStrings.play.metadataLabels.size,
          value: `${session.board.rows}x${session.board.cols}`,
        },
        {
          key: 'difficulty',
          label: minesweeperStrings.play.metadataLabels.difficulty,
          value: minesweeperStrings.difficultyLabels[session.puzzle.difficulty],
        },
        {
          key: 'mines-left',
          label: minesweeperStrings.play.metadataLabels.minesLeft,
          value: String(Math.max(0, session.board.mines - countFlaggedCells(session.board))),
        },
      ] : [],
      main: session ? (
        <View style={styles.gridArea} onLayout={handleGridLayout}>
          {gridContainer.width > 0 && gridContainer.height > 0 ? (
            <ZoomableBoardSurface
              onZoomStateChange={setIsBoardZoomed}
              onRegisterReset={(reset) => {
                resetBoardZoomRef.current = reset;
              }}
            >
              <MinesweeperBoard
                board={session.board}
                containerWidth={gridContainer.width}
                containerHeight={gridContainer.height}
                onReveal={(row, col) => {
                  resetNextMove();
                  void runShellAction({ type: 'reveal-cell', row, col });
                }}
                onToggleFlag={(row, col) => {
                  resetNextMove();
                  void runShellAction({ type: 'toggle-flag', row, col });
                }}
                nextMoveEvidenceCells={nextMove.hint?.evidenceCells ?? []}
                nextMoveSafeTargetCells={nextMove.hint?.targetCells ?? []}
              />
            </ZoomableBoardSurface>
          ) : null}
        </View>
      ) : (
        <View style={styles.gridArea} />
      ),
      footer: null,
    };
  }, [gridContainer.height, gridContainer.width, handleGridLayout, isBoardZoomed, minesweeperStrings, nextMove, styles]);

  return {
    onMissing: handleMissingPuzzle,
    onFreshMissing: handleMissingPuzzle,
    onBeforeLoad: resetHelperState,
    onCleanup: resetHelperState,
    runImmediateAction,
    handleEffects,
    getState,
  };
}

const minesweeperTypedPlayAdapter = {
  contract: minesweeperPlayContract,
  useAdapter: useMinesweeperAdapter,
} satisfies PuzzlePlayAdapter<
  MinesweeperPlaySession,
  MinesweeperActiveSession,
  MinesweeperHudState,
  MinesweeperAction,
  MinesweeperActionEffect
>;

export const minesweeperPlayAdapter = createPuzzlePlayAdapter(minesweeperTypedPlayAdapter);

const makeStyles = () => StyleSheet.create({
  gridArea: {
    flex: 1,
    justifyContent: 'center',
    paddingHorizontal: GRID_HORIZONTAL_PADDING,
    paddingBottom: GRID_BOTTOM_PADDING,
  },
});
