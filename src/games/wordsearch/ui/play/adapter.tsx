import React, { useCallback, useMemo, useRef, useState } from 'react';
import type { LayoutChangeEvent } from 'react-native';
import { StyleSheet, Text, View } from 'react-native';
import { HintPopoverContent } from '../../../../app/components/HintPopoverContent';
import { useLanguage } from '../../../../app/context/LanguageContext';
import { useTheme } from '../../../../app/context/ThemeContext';
import ZoomableBoardSurface from '../../../../app/components/ZoomableBoardSurface';
import {
  createPuzzlePlayAdapter,
  type PuzzleHeaderAction,
  type PuzzleImmediateActionRunner,
  type PuzzlePlayAdapter,
  type PuzzlePlayAdapterInstance,
  type PuzzlePlayAdapterShellArgs,
  type PuzzleRenderState,
} from '../../../../app/shell/games/playAdapter';
import { useNextMoveHelper } from '../../../../app/shell/games/useNextMoveHelper';
import type { Theme } from '../../../../app/theme';
import { withAlpha } from '../../../../app/utils/color';
import { getWordSearchStrings } from '../../content/strings';
import { getWordSearchNextMoveHint } from '../../gameplay/analysis/nextMove';
import { buildBentPaths, runWordSearchAction, type WordSearchAction } from '../../gameplay/actions';
import {
  wordSearchPlayContract,
  type WordSearchHudState,
  type WordSearchPlaySession,
} from '../../gameplay/playContract';
import type { WordSearchActiveSession } from '../../activePuzzle';
import WordSearchPuzzleGrid from './components/WordSearchPuzzleGrid';

function formatThemeId(themeId: string): string {
  if (!themeId) {
    return '-';
  }
  return themeId[0].toUpperCase() + themeId.slice(1);
}

function useWordSearchAdapter({
  difficulty,
  viewportGestureEnabled,
  goBack,
  setDialog,
}: PuzzlePlayAdapterShellArgs): PuzzlePlayAdapterInstance<WordSearchPlaySession, WordSearchAction, never> {
  const { strings: appStrings, resolvedLanguage } = useLanguage();
  const { theme } = useTheme();
  const strings = useMemo(() => getWordSearchStrings(), [resolvedLanguage]);
  const styles = useMemo(() => makeStyles(theme), [theme]);
  const nextMove = useNextMoveHelper((session: WordSearchPlaySession) => getWordSearchNextMoveHint(session));
  const [isBoardZoomed, setIsBoardZoomed] = useState(false);
  const [gridContainer, setGridContainer] = useState({ width: 0, height: 0 });
  const resetBoardZoomRef = useRef<(() => void) | null>(null);

  const resetAdapterState = useCallback(() => {
    nextMove.reset();
    setIsBoardZoomed(false);
    setGridContainer({ width: 0, height: 0 });
    resetBoardZoomRef.current = null;
  }, [nextMove.reset]);

  const onFreshMissing = useCallback(() => {
    const difficultyLabel = strings.difficultyLabels[difficulty];
    setDialog({
      title: strings.play.noPuzzlesDialog.title,
      message: strings.play.noPuzzlesDialog.message(difficultyLabel),
      buttons: [{ text: appStrings.common.back, onPress: goBack }],
    });
  }, [appStrings.common.back, difficulty, goBack, setDialog, strings]);

  const handleGridLayout = useCallback((event: LayoutChangeEvent) => {
    const { width, height } = event.nativeEvent.layout;
    setGridContainer({
      width: Math.max(0, width),
      height: Math.max(0, height),
    });
  }, []);

  const runImmediateAction = useMemo(() => ({
    run(session: WordSearchPlaySession, action: WordSearchAction) {
      const result = runWordSearchAction(session, action);
      return {
        ...result,
        effects: [] as const,
      };
    },
  } satisfies PuzzleImmediateActionRunner<WordSearchPlaySession, WordSearchAction, never>), []);

  const getState = useCallback(({
    session,
    running,
    sessionRef,
    runImmediateAction: runShellAction,
  }: PuzzleRenderState<WordSearchPlaySession, WordSearchAction>) => {
    const applyAction = async (action: WordSearchAction) => {
      if (!running) {
        return;
      }
      nextMove.reset();
      await runShellAction(action);
    };

    const handleCellTap = async (row: number, col: number) => {
      const currentSession = sessionRef.current;
      if (!currentSession || !running) {
        return;
      }

      if (currentSession.hiddenWordMode) {
        await applyAction({ type: 'input-hidden-word-cell', cell: { row, col } });
        return;
      }

      // Tapping any cell already in the active selection clears it.
      if (currentSession.tempSelection) {
        const isInSelection = currentSession.tempSelection.path.some(
          (cell) => cell.row === row && cell.col === col,
        );
        if (isInSelection) {
          await applyAction({ type: 'clear-selection' });
          return;
        }
      }

      if (!currentSession.tempSelection) {
        await applyAction({ type: 'begin-selection', cell: { row, col } });
        return;
      }

      const updatePreview = runWordSearchAction(currentSession, {
        type: 'update-selection',
        cell: { row, col },
      });

      // Try L-shapes alongside the straight line — needed when the second leg is
      // exactly one cell (e.g. COW where C→O is down and O→W is right): the
      // diagonal C→W is geometrically valid but is NOT the intended bent path.
      const bentPaths = buildBentPaths(currentSession.tempSelection.start, { row, col });

      if (updatePreview.changed) {
        // Straight path aligned — but check L-shapes first when a bent word
        // matches, so a diagonal never wins over the correct L-shape.
        for (const bentPath of bentPaths) {
          const bentPreview = runWordSearchAction(
            { ...currentSession, tempSelection: { start: bentPath[0]!, end: bentPath[bentPath.length - 1]!, path: bentPath } },
            { type: 'commit-selection' },
          );
          if (bentPreview.changed && bentPreview.session.foundWordIds.length > currentSession.foundWordIds.length) {
            await applyAction({ type: 'set-selection', path: bentPath });
            await applyAction({ type: 'commit-selection' });
            return;
          }
        }

        // No bent word matched — commit straight and leave visible.
        await applyAction({ type: 'update-selection', cell: { row, col } });
        await applyAction({ type: 'commit-selection' });
        return;
      }

      // Straight line doesn't align — try L-shaped bent paths.
      for (const bentPath of bentPaths) {
        const preview = runWordSearchAction(
          { ...currentSession, tempSelection: { start: bentPath[0]!, end: bentPath[bentPath.length - 1]!, path: bentPath } },
          { type: 'commit-selection' },
        );
        if (preview.changed && preview.session.foundWordIds.length > currentSession.foundWordIds.length) {
          await applyAction({ type: 'set-selection', path: bentPath });
          await applyAction({ type: 'commit-selection' });
          return;
        }
      }

      // No match on any path — start a new selection here.
      await applyAction({ type: 'begin-selection', cell: { row, col } });
    };

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
      popoverContent: !session?.hiddenWordMode && nextMove.hint ? (
        <HintPopoverContent title={nextMove.hint.title} body={nextMove.hint.body} />
      ) : undefined,
    };
    const hiddenWordHeaderAction: PuzzleHeaderAction = {
      key: 'hidden-word',
      accessibilityLabel: session?.hiddenWordMode
        ? strings.play.hiddenWord.exitMode
        : strings.play.hiddenWord.enterMode,
      iconName: session?.hiddenWordMode ? 'key' : 'key-outline',
      active: Boolean(session?.hiddenWordMode),
      onPress: () => {
        void applyAction({ type: 'toggle-hidden-word-mode' });
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
    const sortedWords = session
      ? [...session.puzzle.words].sort((a, b) => a.word.localeCompare(b.word))
      : [];
    return {
      headerActions: isBoardZoomed
        ? [resetZoomHeaderAction, hiddenWordHeaderAction, nextMoveHeaderAction]
        : [hiddenWordHeaderAction, nextMoveHeaderAction],
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
          key: 'theme',
          label: strings.play.metadataLabels.theme,
          value: formatThemeId(session.puzzle.themeId),
        },
        {
          key: 'found',
          label: strings.play.metadataLabels.found,
          value: `${session.foundWordIds.length}/${session.puzzle.words.length}`,
        },
      ] : [],
      main: session ? (
        <View style={styles.mainArea}>
          <View style={styles.wordListRow}>
            {sortedWords.map((word) => {
              const found = session.foundWordIds.includes(word.id);
              return (
                <View
                  key={word.id}
                  style={[
                    styles.wordChip,
                    found ? styles.wordChipFound : null,
                  ]}
                >
                  <Text
                    style={[
                      styles.wordChipText,
                      found ? styles.wordChipTextFound : null,
                    ]}
                  >
                    {word.word}
                  </Text>
                </View>
              );
            })}
          </View>

          <View style={styles.gridArea} onLayout={handleGridLayout}>
            {gridContainer.width > 0 && gridContainer.height > 0 ? (() => {
              const grid = (
                <WordSearchPuzzleGrid
                  puzzle={session.puzzle}
                  foundWordIds={session.foundWordIds}
                  tempSelection={session.tempSelection}
                  containerWidth={gridContainer.width}
                  containerHeight={gridContainer.height}
                  interactive={running}
                  allowDrag={false}
                  nextMoveEvidenceCells={nextMove.hint?.evidenceCells ?? []}
                  nextMoveTargetCells={nextMove.hint?.targetCells ?? []}
                  onCellTap={(row, col) => { void handleCellTap(row, col); }}
                />
              );
              return viewportGestureEnabled ? (
                <ZoomableBoardSurface
                  onZoomStateChange={setIsBoardZoomed}
                  onRegisterReset={(reset) => {
                    resetBoardZoomRef.current = reset;
                  }}
                >
                  {grid}
                </ZoomableBoardSurface>
              ) : grid;
            })() : null}
          </View>
        </View>
      ) : (
        <View style={styles.mainArea} />
      ),
      footer: session ? (
        <View style={styles.footerStack}>
          {session.hiddenWordSolved ? (
            <View style={styles.hiddenWordSolvedCard}>
              <Text style={styles.hiddenWordSolvedTitle}>
                {strings.play.hiddenWord.solvedTitle}
              </Text>
              <Text style={styles.hiddenWordSolvedWord}>
                {strings.play.hiddenWord.revealed(
                  formatThemeId(session.puzzle.hiddenWord.clue),
                  session.puzzle.hiddenWord.word,
                )}
              </Text>
            </View>
          ) : (
            <View style={styles.hiddenWordCard}>
              <Text style={styles.hiddenWordLabel}>
                {formatThemeId(session.puzzle.hiddenWord.clue)}
              </Text>
              <Text style={styles.hiddenWordLetters}>
                {session.puzzle.hiddenWord.word
                  .split('')
                  .map((_, i) => session.puzzle.grid[session.hiddenWordProgress[i]?.row ?? -1]?.[session.hiddenWordProgress[i]?.col ?? -1] ?? '_')
                  .join(' ')}
              </Text>
              <Text style={styles.hiddenWordBody}>
                {session.hiddenWordMode
                  ? strings.play.hiddenWord.instructions
                  : strings.play.hiddenWord.locked}
              </Text>
              {session.hiddenWordMode ? (
                <Text style={styles.hiddenWordHint}>{strings.play.hiddenWord.resetOnMistake}</Text>
              ) : null}
            </View>
          )}
        </View>
      ) : (
        <View style={styles.footerSpacer} />
      ),
    };
  }, [
    gridContainer.height,
    gridContainer.width,
    handleGridLayout,
    isBoardZoomed,
    nextMove,
    strings,
    styles,
  ]);

  return {
    solvedCompletionDelayMs: 2000,
    onFreshMissing,
    onBeforeLoad: resetAdapterState,
    onCleanup: resetAdapterState,
    runImmediateAction,
    getState,
  };
}

const wordSearchTypedPlayAdapter = {
  contract: wordSearchPlayContract,
  useAdapter: useWordSearchAdapter,
} satisfies PuzzlePlayAdapter<
  WordSearchPlaySession,
  WordSearchActiveSession,
  WordSearchHudState,
  WordSearchAction,
  never
>;

export const wordSearchPlayAdapter = createPuzzlePlayAdapter(wordSearchTypedPlayAdapter);

const makeStyles = (theme: Theme) => StyleSheet.create({
  mainArea: {
    flex: 1,
    gap: 8,
    paddingHorizontal: 8,
    paddingTop: 4,
    paddingBottom: 6,
  },
  wordListRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    justifyContent: 'flex-start',
    gap: 6,
  },
  wordChip: {
    borderRadius: 999,
    borderWidth: 1,
    borderColor: withAlpha(theme.border, 0.6),
    backgroundColor: withAlpha(theme.panelSurface, 0.8),
    paddingHorizontal: 10,
    paddingVertical: 6,
  },
  wordChipFound: {
    borderColor: withAlpha(theme.success, 0.5),
    backgroundColor: withAlpha(theme.success, 0.14),
  },
  wordChipText: {
    color: theme.textSecondary,
    fontSize: 12,
    lineHeight: 16,
    fontWeight: '700',
  },
  wordChipTextFound: {
    color: theme.success,
  },
  gridArea: {
    flex: 1,
    justifyContent: 'center',
  },
  footerStack: {
    gap: 10,
  },
  hiddenWordSolvedCard: {
    borderRadius: 14,
    borderWidth: 1,
    borderColor: withAlpha(theme.success, 0.4),
    backgroundColor: withAlpha(theme.success, 0.1),
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  hiddenWordSolvedTitle: {
    color: theme.success,
    fontSize: 13,
    lineHeight: 19,
    fontWeight: '800',
  },
  hiddenWordSolvedWord: {
    marginTop: 4,
    color: theme.text,
    fontSize: 16,
    lineHeight: 22,
    fontWeight: '800',
    letterSpacing: 1,
  },
  hiddenWordCard: {
    borderRadius: 14,
    borderWidth: 1,
    borderColor: withAlpha(theme.primary, 0.2),
    backgroundColor: withAlpha(theme.surfaceElevated, 0.92),
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  hiddenWordLabel: {
    color: theme.text,
    fontSize: 13,
    lineHeight: 19,
    fontWeight: '700',
  },
  hiddenWordLetters: {
    marginTop: 6,
    color: theme.text,
    fontSize: 18,
    lineHeight: 24,
    fontWeight: '800',
    fontFamily: 'monospace',
    letterSpacing: 2,
  },
  hiddenWordBody: {
    marginTop: 6,
    color: theme.text,
    fontSize: 13,
    lineHeight: 18,
  },
  hiddenWordHint: {
    marginTop: 4,
    color: theme.textSecondary,
    fontSize: 12,
    lineHeight: 17,
  },
  footerSpacer: {
    minHeight: 1,
  },
});
