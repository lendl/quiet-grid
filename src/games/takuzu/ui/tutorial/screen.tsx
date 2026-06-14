import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { TouchableRipple } from 'react-native-paper';
import type { StackScreenProps } from '@react-navigation/stack';
import PuzzleTutorialScaffold from '../../../../app/components/PuzzleTutorialScaffold';
import { useLanguage } from '../../../../app/context/LanguageContext';
import { useTheme } from '../../../../app/context/ThemeContext';
import type { RootStackParamList } from '../../../../app/navigation/types';
import type { Theme } from '../../../../app/theme';
import { markPuzzleTutorialSeen } from '../../../../app/utils/settingsStorage';
import { withAlpha } from '../../../../app/utils/color';
import TutorialRow from './components/TutorialRow';
import { getTakuzuTutorialLessons } from '../../content';
import { getTakuzuStrings } from '../../content/strings';
import { cloneGrid } from '../../core/puzzleData';
import type { Grid } from '../../types';

type Props = StackScreenProps<RootStackParamList, 'Tutorial'>;

const ADVANCE_DELAY_MS = 1800;
type TutorialAnswerState = 'idle' | 'wrong' | 'correct';

export default function TutorialScreen({ navigation, route }: Props) {
  const { strings, resolvedLanguage } = useLanguage();
  const { theme } = useTheme();
  const takuzuStrings = getTakuzuStrings();
  const s = makeStyles(theme);
  const lessons = useMemo(() => getTakuzuTutorialLessons(), [resolvedLanguage]);
  const [lessonIndex, setLessonIndex] = useState(0);
  const [grid, setGrid] = useState<Grid>(cloneGrid(lessons[0].grid));
  const [moveIndex, setMoveIndex] = useState(0);
  const [selectedAnswer, setSelectedAnswer] = useState<0 | 1 | null>(null);
  const [answerState, setAnswerState] = useState<TutorialAnswerState>('idle');
  const advanceTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const clearAdvanceTimeout = useCallback(() => {
    if (advanceTimeoutRef.current) {
      clearTimeout(advanceTimeoutRef.current);
      advanceTimeoutRef.current = null;
    }
  }, []);

  const lesson = lessons[lessonIndex];
  const currentMove = lesson.moves[moveIndex] ?? null;

  const resetLessonState = useCallback((nextLesson: typeof lesson) => {
    setGrid(cloneGrid(nextLesson.grid));
    setMoveIndex(0);
    setSelectedAnswer(null);
    setAnswerState('idle');
  }, []);

  useEffect(() => {
    resetLessonState(lesson);
  }, [lesson, resetLessonState]);

  useEffect(() => {
    return () => {
      clearAdvanceTimeout();
    };
  }, [clearAdvanceTimeout]);

  const isLastLesson = lessonIndex === lessons.length - 1;

  const exitTutorial = useCallback(async () => {
    clearAdvanceTimeout();
    await markPuzzleTutorialSeen(route.params.gameId);
    navigation.replace('Game', { gameId: route.params.gameId });
  }, [clearAdvanceTimeout, navigation, route.params.gameId]);

  const advanceLesson = useCallback(async () => {
    if (isLastLesson) {
      await exitTutorial();
      return;
    }

    const nextLesson = lessons[lessonIndex + 1];
    if (!nextLesson) {
      await exitTutorial();
      return;
    }

    resetLessonState(nextLesson);
    setLessonIndex((current) => current + 1);
  }, [exitTutorial, isLastLesson, lessonIndex, lessons, resetLessonState]);

  const goToPreviousLesson = useCallback(() => {
    if (lessonIndex <= 0) {
      return;
    }

    const previousLesson = lessons[lessonIndex - 1];
    if (!previousLesson) {
      return;
    }

    clearAdvanceTimeout();
    resetLessonState(previousLesson);
    setLessonIndex((current) => current - 1);
  }, [clearAdvanceTimeout, lessonIndex, lessons, resetLessonState]);

  // Board stays stable during feedback — no preview of selected answer in the grid.
  const displayGrid = grid;

  // Commits the correct answer to the board and advances the step or lesson,
  // called after the feedback delay so the board only mutates when the step changes.
  const commitCorrectAnswer = useCallback(async () => {
    if (!currentMove) {
      return;
    }

    const isLastMove = moveIndex === lesson.moves.length - 1;
    if (isLastMove) {
      await advanceLesson();
      return;
    }

    const nextGrid = cloneGrid(grid);
    const row = nextGrid[currentMove.row];
    if (!row || currentMove.col < 0 || currentMove.col >= row.length) {
      return;
    }

    nextGrid[currentMove.row][currentMove.col] = currentMove.value;
    setGrid(nextGrid);
    setMoveIndex((current) => current + 1);
    setSelectedAnswer(null);
    setAnswerState('idle');
  }, [advanceLesson, currentMove, grid, lesson.moves.length, moveIndex]);

  const handleAnswerPress = useCallback((value: 0 | 1) => {
    if (!currentMove || answerState === 'correct') {
      return;
    }

    setSelectedAnswer(value);

    if (value !== currentMove.value) {
      setAnswerState('wrong');
      return;
    }

    // Correct: show feedback immediately; commit board + advance after delay.
    setAnswerState('correct');

    clearAdvanceTimeout();

    // For the final move, write the solved cell into the grid right now so the
    // completed board is visible during the entire feedback delay. Non-final
    // moves stay stable — the cell is written later by commitCorrectAnswer so
    // the board does not mutate until the step actually changes.
    const isLastMove = moveIndex === lesson.moves.length - 1;
    if (isLastMove) {
      const nextGrid = cloneGrid(grid);
      const row = nextGrid[currentMove.row];
      if (row && currentMove.col >= 0 && currentMove.col < row.length) {
        nextGrid[currentMove.row][currentMove.col] = currentMove.value;
        setGrid(nextGrid);
      }
    }

    advanceTimeoutRef.current = setTimeout(() => {
      void commitCorrectAnswer();
      advanceTimeoutRef.current = null;
    }, ADVANCE_DELAY_MS);
  }, [answerState, clearAdvanceTimeout, commitCorrectAnswer, currentMove, grid, lesson.moves.length, moveIndex]);

  const feedbackText = answerState === 'correct'
    ? lesson.success
    : answerState === 'wrong'
      ? lesson.retry
      : null;
  const isLastMove = moveIndex === lesson.moves.length - 1;
  const statusText = answerState === 'correct'
    ? isLastMove
       ? (isLastLesson
          ? takuzuStrings.play.tutorial.status.finishing
          : takuzuStrings.play.tutorial.status.nextLesson)
       : takuzuStrings.play.tutorial.status.nextStep
    : null;

  const feedbackContent = (
    <View style={s.feedbackStack}>
      {lessonIndex === 0 ? (
        <View style={s.introCard}>
          <Text style={s.introText}>{takuzuStrings.play.tutorial.introNote}</Text>
        </View>
      ) : null}
      {statusText ? <Text style={s.statusText}>{statusText}</Text> : null}
      {feedbackText ? (
        <View style={s.feedbackCard}>
          <Text style={s.feedbackText}>{feedbackText}</Text>
        </View>
      ) : null}
    </View>
  );

  const controlsContent = (
    <View style={s.answerButtons}>
      {[1, 0].map((value) => {
        const isSelected = selectedAnswer === value;
        return (
          <TouchableRipple
            key={value}
            accessibilityRole="button"
            accessibilityLabel={takuzuStrings.play.tutorial.selectAnswerLabel(value as 0 | 1)}
            disabled={answerState === 'correct'}
            onPress={() => handleAnswerPress(value as 0 | 1)}
            style={[
              s.answerButton,
              isSelected ? s.answerButtonSelected : null,
            ]}
          >
            <Text style={s.answerButtonText}>{value}</Text>
          </TouchableRipple>
        );
      })}
    </View>
  );

  return (
    <PuzzleTutorialScaffold
      title={lesson.title}
      body={lesson.body}
      lessonCount={lessons.length}
      activeLessonIndex={lessonIndex}
      boardMinHeight={132}
      feedbackMinHeight={88}
      onNextLesson={() => {
        void advanceLesson();
      }}
      onPreviousLesson={goToPreviousLesson}
      board={(
        <TutorialRow
          grid={displayGrid}
          focusCell={currentMove ? { row: currentMove.row, col: currentMove.col } : null}
          answerState={answerState}
        />
      )}
      feedback={feedbackContent}
      controls={controlsContent}
    />
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  feedbackStack: {
    gap: 10,
  },
  statusText: {
    fontSize: 12,
    lineHeight: 18,
    color: theme.textSecondary,
    textAlign: 'center',
    fontWeight: '600',
  },
  introCard: {
    borderRadius: 14,
    paddingHorizontal: 12,
    paddingVertical: 10,
    backgroundColor: withAlpha(theme.primary, 0.08),
    borderWidth: 1,
    borderColor: withAlpha(theme.primary, 0.2),
  },
  introText: {
    fontSize: 14,
    lineHeight: 21,
    color: theme.textSecondary,
    textAlign: 'center',
  },
  feedbackCard: {
    borderRadius: 14,
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  feedbackText: {
    fontSize: 14,
    lineHeight: 21,
    color: theme.textSecondary,
    textAlign: 'center',
  },
  answerButtons: {
    flexDirection: 'row',
    gap: 10,
  },
  answerButton: {
    flex: 1,
    paddingVertical: 16,
    borderRadius: 14,
    borderWidth: 2,
    borderColor: theme.border,
    backgroundColor: theme.surface,
    alignItems: 'center',
    justifyContent: 'center',
  },
  answerButtonSelected: {
    borderColor: theme.primary,
    backgroundColor: withAlpha(theme.primary, 0.12),
  },
  answerButtonText: {
    fontSize: 18,
    fontWeight: '800',
    color: theme.text,
  },
});
