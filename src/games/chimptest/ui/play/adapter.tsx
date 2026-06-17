import React, { useCallback, useMemo, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { createPuzzlePlayAdapter } from '../../../../app/shell/games/playAdapter';
import { clearActiveSessionState } from '../../../../app/utils/activeSessionStateStorage';
import type {
  PuzzleEffectHandlerArgs,
  PuzzlePlayAdapter,
  PuzzlePlayAdapterInstance,
  PuzzlePlayAdapterShellArgs,
  PuzzleRenderState,
} from '../../../../app/shell/games/playAdapter';
import { getChimpTestStrings } from '../../content/i18n';
import ChimpTestGrid from './components/ChimpTestGrid';
import { runChimpTestAction, type ChimpTestAction, type ChimpTestEffect } from '../../gameplay/actions';
import {
  chimpTestPlayContract,
  type ChimpTestHudState,
  type ChimpTestPlaySession,
} from '../../gameplay/playContract';
import type { ChimpTestActiveSession } from '../../activePuzzle';

const WRONG_TAP_REVEAL_MS = 700;

function useChimpTestAdapter({
  goHome,
  goBack,
}: PuzzlePlayAdapterShellArgs): PuzzlePlayAdapterInstance<
  ChimpTestPlaySession,
  ChimpTestAction,
  ChimpTestEffect
> {
  const strings = getChimpTestStrings();
  const [gridContainer, setGridContainer] = useState({ width: 0, height: 0 });

  const resetAdapterState = useCallback(() => {
    setGridContainer({ width: 0, height: 0 });
  }, []);

  const handleMissingPuzzle = useCallback(async () => {
    resetAdapterState();
    await clearActiveSessionState();
    goHome();
  }, [goHome, resetAdapterState]);

  const handleFreshMissing = useCallback(() => {
    handleMissingPuzzle();
  }, [handleMissingPuzzle]);

  const runImmediateAction = useMemo(() => ({
    run(session: ChimpTestPlaySession, action: ChimpTestAction) {
      return runChimpTestAction(session, action);
    },
  }), []);

  const handleEffects = useCallback(async ({
    effects,
    previousSession,
    finalizedRef,
    finishLossSession,
  }: PuzzleEffectHandlerArgs<ChimpTestPlaySession, ChimpTestEffect>) => {
    if (!effects.some((e) => e.type === 'wrong-tap') || finalizedRef.current) {
      return;
    }

    await new Promise<void>((resolve) => setTimeout(resolve, WRONG_TAP_REVEAL_MS));

    if (!finalizedRef.current) {
      await finishLossSession('rule-failure', previousSession);
    }
  }, []);

  const getState = useCallback(({
    session,
    sessionRef,
    setSession,
    elapsedSeconds,
    finalizedRef,
    runImmediateAction: runShellAction,
  }: PuzzleRenderState<ChimpTestPlaySession, ChimpTestAction>) => {
    const currentCount = session?.currentCount ?? 0;
    const maxCount = session?.puzzle.maxCount ?? 0;

    const handleCellTap = (row: number, col: number) => {
      const currentSession = sessionRef.current;
      if (!currentSession || currentSession.status !== 'playing' || currentSession.revealAll || finalizedRef.current) {
        return;
      }

      const result = runChimpTestAction(currentSession, { kind: 'tap', row, col, elapsedSeconds });
      if (!result.changed) return;

      if (result.effects.some((e) => e.type === 'wrong-tap')) {
        void runShellAction({ kind: 'tap', row, col, elapsedSeconds });
        return;
      }

      sessionRef.current = result.session;
      setSession(result.session);
    };

    return {
      headerMeta: session ? [
        {
          key: 'round',
          label: strings.play.metadataLabels.round,
          value: `${currentCount} / ${maxCount}`,
        },
        {
          key: 'difficulty',
          label: strings.play.metadataLabels.difficulty,
          value: strings.difficultyLabels[session.puzzle.difficulty],
        },
      ] : [],
      main: session ? (
        <View
          style={styles.boardArea}
          onLayout={(e) => {
            const { width, height } = e.nativeEvent.layout;
            setGridContainer({ width, height });
          }}
        >
          {gridContainer.width > 0 && gridContainer.height > 0 ? (
            <ChimpTestGrid
              cells={session.cells}
              revealAll={session.revealAll}
              wrongTapCell={session.wrongTapCell}
              gridSize={session.puzzle.gridSize}
              nextExpected={session.nextExpected}
              containerWidth={gridContainer.width}
              containerHeight={gridContainer.height}
              onCellTap={handleCellTap}
            />
          ) : null}
        </View>
      ) : (
        <View style={styles.boardArea} />
      ),
      footer: null,
    };
  }, [gridContainer, strings]);

  return {
    onMissing: handleMissingPuzzle,
    onFreshMissing: handleFreshMissing,
    onBeforeLoad: resetAdapterState,
    onCleanup: resetAdapterState,
    runImmediateAction,
    handleEffects,
    getState,
  };
}

const chimpTestTypedPlayAdapter = {
  contract: chimpTestPlayContract,
  useAdapter: useChimpTestAdapter,
} satisfies PuzzlePlayAdapter<
  ChimpTestPlaySession,
  ChimpTestActiveSession,
  ChimpTestHudState,
  ChimpTestAction,
  ChimpTestEffect
>;

export const chimpTestPlayAdapter = createPuzzlePlayAdapter(chimpTestTypedPlayAdapter);

const styles = StyleSheet.create({
  boardArea: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
});
