import React, { useCallback, useMemo, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useTheme } from '../../app/context/ThemeContext';
import { createPuzzlePlayAdapter } from '../../app/shell/games/playAdapter';
import { getMinesweeperStrings } from './content/strings';
import {
  clearActivePuzzleState,
} from '../../app/utils/activePuzzleStateStorage';
import type { Theme } from '../../app/theme';
import { withAlpha } from '../../app/utils/color';
import type {
  PuzzleEffectHandlerArgs,
  PuzzlePlayAdapter,
  PuzzlePlayAdapterInstance,
  PuzzlePlayAdapterShellArgs,
  PuzzleRenderState,
} from '../../app/shell/games/playAdapter';
import MinesweeperBoard from './components/MinesweeperBoard';
import {
  getMinesweeperNextMoveHint,
  type MinesweeperNextMoveHint,
} from './learningCenter';
import { applyMinesweeperAction } from './actions';
import {
  minesweeperPlayContract,
  type MinesweeperAction,
  type MinesweeperActionEffect,
  type MinesweeperHudState,
  type MinesweeperPlaySession,
} from './playContract';
import type { MinesweeperActivePuzzle } from './activePuzzle';
import { countFlaggedCells } from './rules';

function useMinesweeperAdapter({
  goHome,
}: PuzzlePlayAdapterShellArgs): PuzzlePlayAdapterInstance<
  MinesweeperPlaySession,
  MinesweeperAction,
  MinesweeperActionEffect
> {
  const { theme } = useTheme();
  const minesweeperStrings = getMinesweeperStrings();
  const styles = useMemo(() => makeStyles(theme), [theme]);
  const [nextMoveHint, setNextMoveHint] = useState<MinesweeperNextMoveHint | null>(null);
  const [nextMoveVisible, setNextMoveVisible] = useState(false);

  const resetHelperState = useCallback(() => {
    setNextMoveHint(null);
    setNextMoveVisible(false);
  }, []);

  const handleMissingPuzzle = useCallback(async () => {
    resetHelperState();
    await clearActivePuzzleState();
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

    await finishLossSession('rule-based', previousSession);
  }, []);

  const getState = useCallback(({
    session,
    sessionRef,
    runImmediateAction: runShellAction,
  }: PuzzleRenderState<MinesweeperPlaySession, MinesweeperAction>) => {
    const resetNextMove = () => {
      setNextMoveHint(null);
      setNextMoveVisible(false);
    };

    const handleToggleNextMove = () => {
      if (nextMoveVisible) {
        setNextMoveVisible(false);
        return;
      }

      if (!sessionRef.current) {
        return;
      }

      setNextMoveHint(getMinesweeperNextMoveHint(sessionRef.current.board));
      setNextMoveVisible(true);
    };

    return {
      metadata: session ? [
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
      grid: session ? (
        <View style={styles.gridArea}>
          <MinesweeperBoard
            board={session.board}
            onReveal={(row, col) => {
              resetNextMove();
              void runShellAction({ type: 'reveal-cell', row, col });
            }}
            onToggleFlag={(row, col) => {
              resetNextMove();
              void runShellAction({ type: 'toggle-flag', row, col });
            }}
            nextMoveEvidenceCells={nextMoveVisible ? nextMoveHint?.evidenceCells : []}
            nextMoveSafeTargetCells={nextMoveVisible ? nextMoveHint?.targetCells : []}
          />
        </View>
      ) : (
        <View style={styles.gridArea} />
      ),
      helperState: {
        showHelperToggle: true,
        helperVisible: nextMoveVisible,
        helperToggleLabel: nextMoveVisible
          ? minesweeperStrings.play.helperToggle.hide
          : minesweeperStrings.play.helperToggle.show,
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
            {nextMoveHint.teaching ? (
              <>
                <Text style={styles.nextMoveCardLabel}>{nextMoveHint.teaching.patternTitle}</Text>
                <Text style={styles.nextMoveCardPattern}>{nextMoveHint.teaching.patternLabel}</Text>
                <Text style={styles.nextMoveCardLabel}>{nextMoveHint.teaching.explanationTitle}</Text>
                <Text style={styles.nextMoveCardBody}>{nextMoveHint.teaching.explanation}</Text>
              </>
            ) : null}
          </View>
        ) : (
          <View style={styles.emptyFooter} />
        ),
      },
    };
  }, [minesweeperStrings, nextMoveHint, nextMoveVisible, styles]);

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
  MinesweeperActivePuzzle,
  MinesweeperHudState,
  MinesweeperAction,
  MinesweeperActionEffect
>;

export const minesweeperPlayAdapter = createPuzzlePlayAdapter(minesweeperTypedPlayAdapter);

const makeStyles = (theme: Theme) => StyleSheet.create({
  gridArea: {
    flex: 1,
    justifyContent: 'center',
    paddingHorizontal: 12,
    paddingBottom: 24,
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
    borderRadius: 11,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: withAlpha(theme.primary, 0.24),
  },
  nextMoveCardBadgeText: {
    color: theme.primaryLight,
    fontSize: 13,
    fontWeight: '800',
  },
  nextMoveCardTitle: {
    flex: 1,
    color: theme.text,
    fontSize: 14,
    fontWeight: '700',
  },
  nextMoveCardBody: {
    color: theme.textSecondary,
    fontSize: 13,
    lineHeight: 18,
  },
  nextMoveCardLabel: {
    marginTop: 8,
    color: theme.textSecondary,
    fontSize: 11,
    fontWeight: '700',
    textTransform: 'uppercase',
  },
  nextMoveCardPattern: {
    marginTop: 2,
    color: theme.text,
    fontSize: 14,
    fontWeight: '700',
  },
  emptyFooter: {
    minHeight: 1,
  },
});
