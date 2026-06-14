import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { TouchableRipple } from 'react-native-paper';
import type { LayoutChangeEvent } from 'react-native';
import type { StackScreenProps } from '@react-navigation/stack';
import PuzzleTutorialScaffold from '../../../../app/components/PuzzleTutorialScaffold';
import { useLanguage } from '../../../../app/context/LanguageContext';
import { useTheme } from '../../../../app/context/ThemeContext';
import type { RootStackParamList } from '../../../../app/navigation/types';
import type { Theme } from '../../../../app/theme';
import { makeEmptyBooleanGrid } from '../../../../app/utils/activeSessionStateStorage';
import { markPuzzleTutorialSeen } from '../../../../app/utils/settingsStorage';
import { getSudokuStrings } from '../../content/strings';
import { getSudokuTutorialLessons } from '../../content/tutorialLessons';
import SudokuPuzzleGrid from '../play/components/SudokuPuzzleGrid';

type Props = StackScreenProps<RootStackParamList, 'Tutorial'>;
const ADVANCE_DELAY_MS = 1200;

type AnswerState = 'idle' | 'wrong' | 'correct';

export default function SudokuTutorialScreen({ navigation, route }: Props) {
  const { resolvedLanguage } = useLanguage();
  const { theme } = useTheme();
  const strings = useMemo(() => getSudokuStrings(), [resolvedLanguage]);
  const lessons = useMemo(() => getSudokuTutorialLessons(), [resolvedLanguage]);
  const styles = useMemo(() => makeStyles(theme), [theme]);
  const finishedCells = useMemo(() => makeEmptyBooleanGrid(9), []);
  const [lessonIndex, setLessonIndex] = useState(0);
  const [answerState, setAnswerState] = useState<AnswerState>('idle');
  const [feedbackText, setFeedbackText] = useState<string | null>(null);
  const [boardBounds, setBoardBounds] = useState({ width: 0, height: 0 });
  const advanceTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const lesson = lessons[lessonIndex];
  const isLastLesson = lessonIndex === lessons.length - 1;

  const clearAdvanceTimeout = useCallback(() => {
    if (!advanceTimeoutRef.current) {
      return;
    }

    clearTimeout(advanceTimeoutRef.current);
    advanceTimeoutRef.current = null;
  }, []);

  useEffect(() => () => {
    clearAdvanceTimeout();
  }, [clearAdvanceTimeout]);

  useEffect(() => {
    setAnswerState('idle');
    setFeedbackText(null);
  }, [lessonIndex]);

  const handleBoardLayout = useCallback((event: LayoutChangeEvent) => {
    const { width, height } = event.nativeEvent.layout;
    setBoardBounds({
      width: Math.max(0, width),
      height: Math.max(0, height),
    });
  }, []);

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

    setLessonIndex((current) => current + 1);
  }, [exitTutorial, isLastLesson]);

  const goToPreviousLesson = useCallback(() => {
    if (lessonIndex <= 0) {
      return;
    }

    clearAdvanceTimeout();
    setLessonIndex((current) => current - 1);
  }, [clearAdvanceTimeout, lessonIndex]);

  const handleAnswerPress = useCallback((optionKey: string) => {
    if (!lesson.correctOptionKey || answerState === 'correct') {
      return;
    }

    clearAdvanceTimeout();

    if (optionKey !== lesson.correctOptionKey) {
      setAnswerState('wrong');
      setFeedbackText(lesson.wrongFeedback ?? null);
      return;
    }

    setAnswerState('correct');
    setFeedbackText(lesson.correctFeedback ?? null);
    advanceTimeoutRef.current = setTimeout(() => {
      void advanceLesson();
      advanceTimeoutRef.current = null;
    }, ADVANCE_DELAY_MS);
  }, [
    advanceLesson,
    answerState,
    clearAdvanceTimeout,
    lesson.correctOptionKey,
    lesson.correctFeedback,
    lesson.wrongFeedback,
  ]);

  const feedbackContent = (
    <View style={styles.feedbackStack}>
      <Text style={styles.progressText}>{strings.tutorial.progressLabel(lessonIndex + 1, lessons.length)}</Text>
      <Text style={styles.summaryText}>{lesson.summary}</Text>
      <Text style={styles.controlText}>{lesson.controlHint}</Text>
      {answerState === 'correct' ? (
        <Text style={styles.statusText}>{isLastLesson ? strings.tutorial.status.finishing : strings.tutorial.status.nextLesson}</Text>
      ) : null}
      {feedbackText ? (
        <Text style={[styles.feedbackText, answerState === 'wrong' ? styles.feedbackTextWrong : null]}>
          {feedbackText}
        </Text>
      ) : null}
    </View>
  );

  const controlsContent = lesson.options && lesson.prompt ? (
    <View style={styles.answerControls}>
      <Text style={styles.answerPrompt}>{lesson.prompt}</Text>
      <View style={styles.answerButtonsRow}>
        {Object.entries(lesson.options).map(([key, label]) => (
          <TouchableRipple
            key={key}
            accessibilityRole="button"
            disabled={answerState === 'correct'}
            onPress={() => handleAnswerPress(key)}
            style={[
              styles.answerButton,
              answerState === 'correct' && key === lesson.correctOptionKey
                ? styles.answerButtonCorrect
                : null,
            ]}
          >
            <Text style={styles.answerButtonText}>{label}</Text>
          </TouchableRipple>
        ))}
      </View>
    </View>
  ) : (
    <TouchableRipple
      accessibilityRole="button"
      onPress={() => {
        void advanceLesson();
      }}
      style={styles.continueButton}
    >
      <Text style={styles.continueButtonText}>{lesson.continueLabel ?? strings.tutorial.exitLabel.end}</Text>
    </TouchableRipple>
  );

  return (
    <PuzzleTutorialScaffold
      title={lesson.title}
      body={lesson.body}
      lessonCount={lessons.length}
      activeLessonIndex={lessonIndex}
      boardMinHeight={320}
      feedbackMinHeight={164}
      onNextLesson={() => {
        void advanceLesson();
      }}
      onPreviousLesson={goToPreviousLesson}
      board={(
        <View style={styles.boardHost} onLayout={handleBoardLayout}>
          {boardBounds.width > 0 ? (
            <SudokuPuzzleGrid
              board={lesson.board}
              givens={lesson.givens}
              notes={lesson.notes}
              finishedCells={finishedCells}
              selectedCell={null}
              validatedUnitKeys={[]}
              penalizedUnitKeys={[]}
              boardFeedbackEffects={null}
              interactive={false}
              nextMoveEvidenceCells={lesson.evidenceCells}
              nextMoveTargetCells={lesson.targetCells}
              nextMoveHighlightRows={lesson.highlightRows}
              nextMoveHighlightCols={lesson.highlightCols}
              nextMoveHighlightBoxes={lesson.highlightBoxes}
              showPlacementTargetDigits={false}
              containerWidth={boardBounds.width}
              containerHeight={boardBounds.height}
              onCellPress={() => {}}
            />
          ) : null}
        </View>
      )}
      feedback={feedbackContent}
      controls={controlsContent}
    />
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  boardHost: {
    width: '100%',
    minHeight: 300,
    alignItems: 'center',
    justifyContent: 'center',
  },
  feedbackStack: {
    gap: 10,
  },
  progressText: {
    fontSize: 12,
    lineHeight: 18,
    color: theme.textSecondary,
    textAlign: 'center',
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 0.8,
  },
  summaryText: {
    fontSize: 14,
    lineHeight: 21,
    color: theme.textSecondary,
    textAlign: 'center',
  },
  controlText: {
    fontSize: 14,
    lineHeight: 21,
    color: theme.textSecondary,
    textAlign: 'center',
  },
  statusText: {
    fontSize: 12,
    lineHeight: 18,
    color: theme.textSecondary,
    textAlign: 'center',
    fontWeight: '600',
  },
  feedbackText: {
    fontSize: 14,
    lineHeight: 21,
    color: theme.textSecondary,
    textAlign: 'center',
  },
  feedbackTextWrong: {
    color: theme.difficultyHard,
    fontWeight: '600',
  },
  answerControls: {
    gap: 10,
  },
  answerPrompt: {
    fontSize: 14,
    lineHeight: 20,
    color: theme.textSecondary,
    textAlign: 'center',
    fontWeight: '700',
  },
  answerButtonsRow: {
    flexDirection: 'row',
    gap: 10,
  },
  answerButton: {
    flex: 1,
    minHeight: 52,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: theme.primary,
  },
  answerButtonCorrect: {
    backgroundColor: theme.success,
  },
  answerButtonText: {
    fontSize: 16,
    fontWeight: '800',
    color: theme.onPrimary,
  },
  continueButton: {
    minHeight: 52,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: theme.primary,
  },
  continueButtonText: {
    fontSize: 16,
    fontWeight: '800',
    color: theme.onPrimary,
  },
});
