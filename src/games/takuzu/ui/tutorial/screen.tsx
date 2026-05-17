import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
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
import { cloneGrid } from '../../puzzleData';
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
  const [completed, setCompleted] = useState(false);
  const [moveIndex, setMoveIndex] = useState(0);
  const [selectedAnswer, setSelectedAnswer] = useState<0 | 1 | null>(null);
  const [answerState, setAnswerState] = useState<TutorialAnswerState>('idle');
  const advanceTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const lesson = lessons[lessonIndex];
  const isReplay = route.params.entry === 'howToPlay';
  const currentMove = lesson.moves[moveIndex] ?? null;
  const progressLabel = takuzuStrings.play.tutorial.progressLabel(lessonIndex + 1);

  const resetLessonState = useCallback((nextLesson: typeof lesson) => {
    setGrid(cloneGrid(nextLesson.grid));
    setCompleted(false);
    setMoveIndex(0);
    setSelectedAnswer(null);
    setAnswerState('idle');
  }, []);

  useEffect(() => {
    resetLessonState(lesson);
  }, [lesson, resetLessonState]);

  useEffect(() => {
    return () => {
      if (advanceTimeoutRef.current) {
        clearTimeout(advanceTimeoutRef.current);
      }
    };
  }, []);

  const isLastLesson = lessonIndex === lessons.length - 1;

  const exitTutorial = useCallback(async () => {
    await markPuzzleTutorialSeen(route.params.puzzleTypeId);

    if (!isReplay) {
      navigation.replace('Puzzle', { puzzleTypeId: route.params.puzzleTypeId });
      return;
    }

    navigation.goBack();
  }, [isReplay, navigation, route.params.puzzleTypeId]);

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

    resetLessonState(previousLesson);
    setLessonIndex((current) => current - 1);
  }, [lessonIndex, lessons, resetLessonState]);

  useEffect(() => {
    if (!completed) {
      return;
    }

    advanceTimeoutRef.current = setTimeout(() => {
      void advanceLesson();
    }, ADVANCE_DELAY_MS);

    return () => {
      if (advanceTimeoutRef.current) {
        clearTimeout(advanceTimeoutRef.current);
        advanceTimeoutRef.current = null;
      }
    };
  }, [advanceLesson, completed]);

  const displayGrid = useMemo(() => {
    if (!currentMove || selectedAnswer === null || answerState === 'idle') {
      return grid;
    }

    const row = grid[currentMove.row];
    if (!row || currentMove.col < 0 || currentMove.col >= row.length) {
      return grid;
    }

    const nextGrid = cloneGrid(grid);
    nextGrid[currentMove.row][currentMove.col] = selectedAnswer;
    return nextGrid;
  }, [answerState, currentMove, grid, selectedAnswer]);

  const handleAnswerPress = useCallback((value: 0 | 1) => {
    if (!currentMove || completed || answerState === 'correct') {
      return;
    }

    setSelectedAnswer(value);

    if (value !== currentMove.value) {
      setAnswerState('wrong');
      return;
    }

    const nextGrid = cloneGrid(grid);
    const row = nextGrid[currentMove.row];
    if (!row || currentMove.col < 0 || currentMove.col >= row.length) {
      return;
    }

    nextGrid[currentMove.row][currentMove.col] = value;
    setGrid(nextGrid);
    setAnswerState('correct');

    const isLastMove = moveIndex === lesson.moves.length - 1;
    if (advanceTimeoutRef.current) {
      clearTimeout(advanceTimeoutRef.current);
      advanceTimeoutRef.current = null;
    }

    if (isLastMove) {
      setCompleted(true);
      return;
    }

    advanceTimeoutRef.current = setTimeout(() => {
      setMoveIndex((current) => current + 1);
      setSelectedAnswer(null);
      setAnswerState('idle');
      advanceTimeoutRef.current = null;
    }, ADVANCE_DELAY_MS);
  }, [answerState, completed, currentMove, grid, moveIndex]);

  const feedbackText = answerState === 'correct'
    ? lesson.success
    : answerState === 'wrong'
      ? lesson.retry
      : null;
  const statusText = answerState === 'correct'
    ? completed
       ? (isLastLesson
          ? takuzuStrings.play.tutorial.status.finishing
          : takuzuStrings.play.tutorial.status.nextLesson)
       : takuzuStrings.play.tutorial.status.nextStep
    : null;

  return (
    <PuzzleTutorialScaffold
      progressLabel={progressLabel}
      statusText={statusText}
      title={lesson.title}
      body={lesson.body}
      lessonCount={lessons.length}
      activeLessonIndex={lessonIndex}
      boardMinHeight={132}
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
      footer={(
        <>
          {lessonIndex === 0 ? (
            <View style={s.introCard}>
              <Text style={s.introText}>{takuzuStrings.play.tutorial.introNote}</Text>
            </View>
          ) : null}
          <View style={s.feedbackSlot}>
            {feedbackText ? (
              <View style={s.feedbackCard}>
                <Text style={s.feedbackText}>{feedbackText}</Text>
              </View>
            ) : (
              <View style={s.feedbackPlaceholder} />
            )}
          </View>
          <View style={s.answerButtons}>
            {[1, 0].map((value) => {
              const isSelected = selectedAnswer === value;
              return (
                <TouchableOpacity
                  key={value}
                  accessibilityRole="button"
                  accessibilityLabel={takuzuStrings.play.tutorial.selectAnswerLabel(value as 0 | 1)}
                  activeOpacity={0.82}
                  disabled={answerState === 'correct'}
                  onPress={() => handleAnswerPress(value as 0 | 1)}
                  style={[
                    s.answerButton,
                    isSelected ? s.answerButtonSelected : null,
                  ]}
                >
                  <Text style={s.answerButtonText}>{value}</Text>
                </TouchableOpacity>
              );
            })}
          </View>
        </>
      )}
    />
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  feedbackSlot: {
    minHeight: 52,
    justifyContent: 'center',
    gap: 12,
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
  feedbackPlaceholder: {
    minHeight: 44,
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
