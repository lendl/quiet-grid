import React, { useCallback, useMemo, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useLanguage } from '../../../../app/context/LanguageContext';
import { useTheme } from '../../../../app/context/ThemeContext';
import type { Theme } from '../../../../app/theme';
import { withAlpha } from '../../../../app/utils/color';
import { clearActivePuzzleState } from '../../../../app/utils/activePuzzleStateStorage';
import {
  createPuzzlePlayAdapter,
  type PuzzleHeaderAction,
  type PuzzlePlayAdapter,
  type PuzzlePlayAdapterInstance,
  type PuzzlePlayAdapterShellArgs,
  type PuzzleRenderState,
} from '../../../../app/shell/games/playAdapter';
import type {
  NonogramActivePuzzle,
  NonogramHudState,
  NonogramPlaySession,
} from '../../gameplay/activePuzzle';
import { applyNonogramAction, type NonogramAction } from '../../gameplay/actions';
import { getNonogramNextMoveHint } from '../../gameplay/analysis';
import { nonogramPlayContract } from '../../gameplay/playContract';
import { getNonogramStrings } from '../../content/strings';
import NonogramBoard from './components/NonogramBoard';

function makeStyles(theme: Theme) {
  return StyleSheet.create({
    gridArea: {
      flex: 1,
      justifyContent: 'center',
      paddingHorizontal: 8,
      paddingBottom: 24,
    },
    hintCard: {
      flex: 1,
      borderRadius: 16,
      paddingHorizontal: 12,
      paddingTop: 10,
      paddingBottom: 10,
      backgroundColor: withAlpha(theme.surfaceElevated, 0.96),
      borderWidth: 1,
      borderColor: withAlpha(theme.primaryLight, 0.34),
      gap: 6,
    },
    hintTitle: {
      color: theme.text,
      fontSize: 14,
      fontWeight: '700',
    },
    hintBody: {
      color: theme.textSecondary,
      fontSize: 13,
      lineHeight: 18,
    },
    emptyFooter: {
      minHeight: 1,
    },
  });
}

function useNonogramAdapter({
  difficulty,
  setDialog,
  goBack,
  goHome,
}: PuzzlePlayAdapterShellArgs): PuzzlePlayAdapterInstance<
  NonogramPlaySession,
  NonogramAction
> {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const nonogramStrings = getNonogramStrings();
  const styles = useMemo(() => makeStyles(theme), [theme]);
  const [nextMoveVisible, setNextMoveVisible] = useState(false);

  const resetHelperState = useCallback(() => {
    setNextMoveVisible(false);
  }, []);

  const handleMissingPuzzle = useCallback(async () => {
    resetHelperState();
    await clearActivePuzzleState();
    goHome();
  }, [goHome, resetHelperState]);

  const handleFreshMissing = useCallback(() => {
    setDialog({
      title: nonogramStrings.play.noPuzzlesDialog.title,
      message: nonogramStrings.play.noPuzzlesDialog.message,
      buttons: [{ text: strings.common.back, onPress: goBack }],
    });
  }, [goBack, nonogramStrings, setDialog, strings.common.back]);

  const runImmediateAction = useMemo(() => ({
    run(session: NonogramPlaySession, action: NonogramAction) {
      return applyNonogramAction(session, action);
    },
  }), []);

  const getState = useCallback(({
    session,
    sessionRef,
    runImmediateAction: runShellAction,
  }: PuzzleRenderState<NonogramPlaySession, NonogramAction>) => {
    const hint = session && nextMoveVisible ? getNonogramNextMoveHint(session) : null;

    const toggleNextMove = () => {
      if (!sessionRef.current) {
        return;
      }
      setNextMoveVisible((current) => !current);
    };

    const nextMoveHeaderAction: PuzzleHeaderAction = {
      key: 'next-move',
      accessibilityLabel: nextMoveVisible
        ? nonogramStrings.play.helperToggle.hide
        : nonogramStrings.play.helperToggle.show,
      iconName: nextMoveVisible ? 'bulb' : 'bulb-outline',
      active: nextMoveVisible,
      onPress: toggleNextMove,
    };

    return {
      headerActions: [nextMoveHeaderAction],
      headerMeta: session ? [
        {
          key: 'size',
          label: nonogramStrings.play.metadataLabels.size,
          value: `${session.puzzle.rows}x${session.puzzle.cols}`,
        },
        {
          key: 'difficulty',
          label: nonogramStrings.play.metadataLabels.difficulty,
          value: nonogramStrings.difficultyLabels[session.puzzle.difficulty],
        },
      ] : [],
      main: session ? (
        <View style={styles.gridArea}>
          <NonogramBoard
            puzzle={session.puzzle}
            cells={session.cells}
            onToggleCell={(index) => {
              void runShellAction({ type: 'toggle-cell', index });
            }}
            nextMoveEvidenceCells={hint?.evidenceCells ?? []}
            nextMoveTargetCells={hint?.targetCells ?? []}
            nextMoveHighlightRows={hint?.highlightRows ?? []}
            nextMoveHighlightCols={hint?.highlightCols ?? []}
          />
        </View>
      ) : (
        <View style={styles.gridArea} />
      ),
      footer: nextMoveVisible && hint ? (
        <View style={styles.hintCard}>
          <Text style={styles.hintTitle}>{hint.title}</Text>
          <Text style={styles.hintBody}>{hint.body}</Text>
        </View>
      ) : (
        <View style={styles.emptyFooter} />
      ),
    };
  }, [nextMoveVisible, nonogramStrings, styles]);

  return {
    onMissing: handleMissingPuzzle,
    onFreshMissing: handleFreshMissing,
    onBeforeLoad: resetHelperState,
    onCleanup: resetHelperState,
    runImmediateAction,
    getState,
  };
}

const nonogramTypedPlayAdapter = {
  contract: nonogramPlayContract,
  useAdapter: useNonogramAdapter,
} satisfies PuzzlePlayAdapter<
  NonogramPlaySession,
  NonogramActivePuzzle,
  NonogramHudState,
  NonogramAction
>;

export const nonogramPlayAdapter = createPuzzlePlayAdapter(nonogramTypedPlayAdapter);
