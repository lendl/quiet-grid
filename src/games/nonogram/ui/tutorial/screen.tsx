import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import type { StackScreenProps } from '@react-navigation/stack';
import GridHomeIcon from '../../../../app/components/GridHomeIcon';
import PuzzleTutorialScaffold from '../../../../app/components/PuzzleTutorialScaffold';
import { useLanguage } from '../../../../app/context/LanguageContext';
import { useTheme } from '../../../../app/context/ThemeContext';
import { returnToHome } from '../../../../app/navigation/returnToHome';
import type { RootStackParamList } from '../../../../app/navigation/types';
import type { Theme } from '../../../../app/theme';
import { withAlpha } from '../../../../app/utils/color';
import { markPuzzleTutorialSeen } from '../../../../app/utils/settingsStorage';
import { getNonogramStrings } from '../../content/strings';
import { getNonogramTutorialLessons } from '../../content/tutorialLessons';
import type { NonogramCellState, NonogramTutorialAction } from '../../types';
import { cellIndex } from '../../gameplay/rules/board';
import NonogramTutorialBoard from './components/NonogramTutorialBoard';

type Props = StackScreenProps<RootStackParamList, 'Tutorial'>;
type TutorialAnswerState = 'idle' | 'wrong' | 'correct';

const ADVANCE_DELAY_MS = 1200;

function makeStyles(theme: Theme) {
  return StyleSheet.create({
    backButton: {
      alignSelf: 'flex-start',
      marginBottom: 6,
      minWidth: 44,
      minHeight: 36,
      alignItems: 'center',
      justifyContent: 'center',
    },
    answerTray: {
      gap: 12,
    },
    feedbackCard: {
      minHeight: 54,
      paddingHorizontal: 14,
      justifyContent: 'center',
      borderRadius: 16,
      borderWidth: 1,
      borderColor: withAlpha(theme.primaryLight, 0.35),
      backgroundColor: withAlpha(theme.surfaceElevated, 0.96),
    },
    feedbackPlaceholder: {
      minHeight: 54,
    },
    feedbackText: {
      color: theme.textSecondary,
      fontSize: 14,
      lineHeight: 20,
    },
    answerButtons: {
      flexDirection: 'row',
      gap: 12,
    },
    answerButton: {
      flex: 1,
      minHeight: 54,
      alignItems: 'center',
      justifyContent: 'center',
      borderRadius: 18,
      borderWidth: 1,
      borderColor: theme.border,
      backgroundColor: theme.surface,
    },
    answerButtonSelected: {
      borderColor: theme.primary,
      backgroundColor: withAlpha(theme.primary, 0.12),
    },
    answerButtonText: {
      color: theme.text,
      fontSize: 16,
      fontWeight: '800',
    },
    actionPrompt: {
      fontSize: 16,
      lineHeight: 24,
      color: theme.text,
      fontWeight: '600',
      textAlign: 'center',
    },
  });
}

function applyLessonAction(
  cells: readonly NonogramCellState[],
  lesson: ReturnType<typeof getNonogramTutorialLessons>[number],
  action: NonogramTutorialAction,
): NonogramCellState[] {
  const next = [...cells];
  lesson.targetCells.forEach(({ row, col }) => {
    const index = cellIndex(row, col, lesson.puzzle.cols);
    next[index] = action;
  });
  return next;
}

export default function NonogramTutorialScreen({ navigation, route }: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const nonogramStrings = getNonogramStrings();
  const styles = useMemo(() => makeStyles(theme), [theme]);
  const lessons = useMemo(() => getNonogramTutorialLessons(), []);
  const [lessonIndex, setLessonIndex] = useState(0);
  const [cells, setCells] = useState<NonogramCellState[]>(lessons[0]?.initialCells ?? []);
  const [answerState, setAnswerState] = useState<TutorialAnswerState>('idle');
  const [selectedAction, setSelectedAction] = useState<NonogramTutorialAction | null>(null);
  const advanceTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isReplay = route.params.entry === 'howToPlay';

  const lesson = lessons[lessonIndex];
  const isLastLesson = lessonIndex === lessons.length - 1;

  const resetLessonState = useCallback((nextLesson: typeof lesson) => {
    setCells(nextLesson.initialCells);
    setAnswerState('idle');
    setSelectedAction(null);
  }, []);

  useEffect(() => {
    if (!lesson) {
      return;
    }
    resetLessonState(lesson);
  }, [lesson, resetLessonState]);

  useEffect(() => () => {
    if (advanceTimeoutRef.current) {
      clearTimeout(advanceTimeoutRef.current);
    }
  }, []);

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

    setLessonIndex((current) => current + 1);
    resetLessonState(nextLesson);
  }, [exitTutorial, isLastLesson, lessonIndex, lessons, resetLessonState]);

  const handleActionPress = useCallback((action: NonogramTutorialAction) => {
    if (!lesson || answerState === 'correct') {
      return;
    }

    setSelectedAction(action);
    if (action !== lesson.action) {
      setAnswerState('wrong');
      return;
    }

    setCells(applyLessonAction(lesson.initialCells, lesson, action));
    setAnswerState('correct');
    if (advanceTimeoutRef.current) {
      clearTimeout(advanceTimeoutRef.current);
    }
    advanceTimeoutRef.current = setTimeout(() => {
      void advanceLesson();
    }, ADVANCE_DELAY_MS);
  }, [advanceLesson, answerState, lesson]);

  if (!lesson) {
    return null;
  }

  const feedbackText = answerState === 'correct'
    ? lesson.success
    : answerState === 'wrong'
      ? lesson.retry
      : null;
  const statusText = answerState === 'correct'
    ? (isLastLesson ? nonogramStrings.tutorial.status.finishing : nonogramStrings.tutorial.status.nextLesson)
    : null;
  const exitLabel = isReplay || lessonIndex > 0
    ? nonogramStrings.tutorial.exitLabel.end
    : nonogramStrings.tutorial.exitLabel.skip;

  return (
    <PuzzleTutorialScaffold
      backButton={isReplay ? (
        <TouchableOpacity
          style={styles.backButton}
          onPress={() => returnToHome(navigation)}
          accessibilityLabel={strings.common.goHome}
          activeOpacity={0.8}
        >
          <GridHomeIcon />
        </TouchableOpacity>
      ) : undefined}
      progressLabel={nonogramStrings.tutorial.progressLabel(lessonIndex + 1, lessons.length)}
      exitLabel={exitLabel}
      onExit={() => {
        void exitTutorial();
      }}
      statusText={statusText}
      title={lesson.title}
      body={lesson.body}
      lessonCount={lessons.length}
      activeLessonIndex={lessonIndex}
      boardMinHeight={232}
      board={<NonogramTutorialBoard lesson={lesson} cells={cells} />}
      footer={(
        <View style={styles.answerTray}>
          <Text style={styles.actionPrompt}>{lesson.prompt}</Text>
          {feedbackText ? (
            <View style={styles.feedbackCard}>
              <Text style={styles.feedbackText}>{feedbackText}</Text>
            </View>
          ) : (
            <View style={styles.feedbackPlaceholder} />
          )}

          <View style={styles.answerButtons}>
            {(['filled', 'marked'] as const).map((action) => (
              <TouchableOpacity
                key={action}
                style={[
                  styles.answerButton,
                  selectedAction === action ? styles.answerButtonSelected : null,
                ]}
                onPress={() => handleActionPress(action)}
                activeOpacity={0.82}
              >
                <Text style={styles.answerButtonText}>
                  {nonogramStrings.tutorial.actionLabels[action]}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>
      )}
    />
  );
}
