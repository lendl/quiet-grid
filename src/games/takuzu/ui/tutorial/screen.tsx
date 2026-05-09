import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import type { StackScreenProps } from '@react-navigation/stack';
import AppScreen from '../../../../app/components/AppScreen';
import GridHomeIcon from '../../../../app/components/GridHomeIcon';
import { useLanguage } from '../../../../app/context/LanguageContext';
import { useTheme } from '../../../../app/context/ThemeContext';
import { returnToHome } from '../../../../app/navigation/returnToHome';
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
  const lessonDots = lessons.map((_, index) => index === lessonIndex);

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

  const exitLabel = useMemo(() => {
    if (isReplay || lessonIndex > 0) {
      return takuzuStrings.play.tutorial.exitLabel.end;
    }
    return takuzuStrings.play.tutorial.exitLabel.skip;
  }, [isReplay, lessonIndex, takuzuStrings]);

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
    <AppScreen contentStyle={s.container}>
      {isReplay ? (
        <TouchableOpacity
          style={s.backButton}
          onPress={() => returnToHome(navigation)}
          accessibilityLabel={strings.common.goHome}
          activeOpacity={0.8}
        >
          <GridHomeIcon />
        </TouchableOpacity>
      ) : null}
      <View style={s.header}>
        <Text style={s.progress}>{progressLabel}</Text>
        <TouchableOpacity onPress={() => void exitTutorial()} activeOpacity={0.8}>
          <Text style={s.exitText}>{exitLabel}</Text>
        </TouchableOpacity>
      </View>
      <View style={s.statusRow}>
        {statusText ? <Text style={s.statusText}>{statusText}</Text> : null}
      </View>

      <View style={s.mainContent}>
        <Text style={s.promptText}>{lesson.prompt}</Text>
        {lesson.body ? <Text style={s.body}>{lesson.body}</Text> : null}

        <View style={s.rowWrap}>
          <TutorialRow
            grid={displayGrid}
            focusCell={currentMove ? { row: currentMove.row, col: currentMove.col } : null}
            answerState={answerState}
          />
        </View>

        <View style={s.dots}>
          {lessonDots.map((isActive, index) => (
            <View key={index} style={[s.dot, isActive ? s.dotActive : null]} />
          ))}
        </View>
      </View>

      <View style={s.answerTray}>
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
      </View>
    </AppScreen>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.background,
    padding: 20,
    gap: 20,
  },
  backButton: {
    alignSelf: 'flex-start',
    marginBottom: 6,
    minWidth: 44,
    minHeight: 36,
    alignItems: 'center',
    justifyContent: 'center',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  statusRow: {
    minHeight: 20,
    alignItems: 'flex-end',
  },
  statusText: {
    fontSize: 12,
    fontWeight: '600',
    color: theme.textSecondary,
  },
  progress: {
    fontSize: 14,
    fontWeight: '700',
    color: theme.primaryLight,
    letterSpacing: 0.5,
  },
  exitText: {
    fontSize: 14,
    fontWeight: '700',
    color: theme.textSecondary,
  },
  mainContent: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 18,
  },
  body: {
    maxWidth: 280,
    fontSize: 15,
    lineHeight: 23,
    color: theme.textSecondary,
    textAlign: 'center',
  },
  rowWrap: {
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 132,
  },
  dots: {
    flexDirection: 'row',
    gap: 8,
    alignItems: 'center',
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 999,
    backgroundColor: theme.border,
  },
  dotActive: {
    width: 20,
    backgroundColor: theme.primary,
  },
  promptText: {
    maxWidth: 260,
    fontSize: 28,
    lineHeight: 34,
    color: theme.text,
    fontWeight: '800',
    textAlign: 'center',
  },
  answerTray: {
    gap: 14,
    paddingTop: 8,
    paddingBottom: 8,
  },
  feedbackSlot: {
    minHeight: 52,
    justifyContent: 'center',
    gap: 12,
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
