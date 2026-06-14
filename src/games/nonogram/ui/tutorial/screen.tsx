import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { TouchableRipple } from 'react-native-paper';
import type { StackScreenProps } from '@react-navigation/stack';
import PuzzleTutorialScaffold from '../../../../app/components/PuzzleTutorialScaffold';
import { useLanguage } from '../../../../app/context/LanguageContext';
import { useTheme } from '../../../../app/context/ThemeContext';
import type { RootStackParamList } from '../../../../app/navigation/types';
import type { Theme } from '../../../../app/theme';
import { markPuzzleTutorialSeen } from '../../../../app/utils/settingsStorage';
import { getNonogramStrings } from '../../content/i18n';
import { getNonogramTutorialLessons } from '../../content/tutorialLessons';
import NonogramPuzzleGrid from '../play/components/NonogramPuzzleGrid';

type Props = StackScreenProps<RootStackParamList, 'Tutorial'>;

export default function TutorialScreen({ navigation, route }: Props) {
  const { resolvedLanguage } = useLanguage();
  const { theme } = useTheme();
  const strings = useMemo(() => getNonogramStrings(), [resolvedLanguage]);
  const lessons = useMemo(() => getNonogramTutorialLessons(), [resolvedLanguage]);
  const styles = useMemo(() => makeStyles(theme), [theme]);
  const [lessonIndex, setLessonIndex] = useState(0);
  const [showAnswerRetry, setShowAnswerRetry] = useState(false);
  const advanceTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const lesson = lessons[lessonIndex];
  const isLastLesson = lessonIndex === lessons.length - 1;

  const clearAdvanceTimeout = useCallback(() => {
    if (advanceTimeoutRef.current) {
      clearTimeout(advanceTimeoutRef.current);
      advanceTimeoutRef.current = null;
    }
  }, []);

  useEffect(() => () => {
    clearAdvanceTimeout();
  }, [clearAdvanceTimeout]);

  useEffect(() => {
    setShowAnswerRetry(false);
  }, [lessonIndex]);

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

  const feedbackContent = (
    <View style={styles.feedbackStack}>
      <Text style={styles.progressText}>{strings.play.tutorial.progressLabel(lessonIndex + 1)}</Text>
      <Text style={styles.summaryText}>{lesson.summary}</Text>
      {showAnswerRetry ? (
        <Text style={styles.retryText}>{strings.play.tutorial.answerRetry}</Text>
      ) : null}
    </View>
  );

  const expectedAnswer = lesson.targetCells[0]?.value;
  const requiresAnswerChoice = expectedAnswer !== undefined;

  const controlsContent = (
    requiresAnswerChoice ? (
      <View style={styles.answerControls}>
        <Text style={styles.answerPrompt}>{strings.play.tutorial.answerPrompt}</Text>
        <View style={styles.answerButtonsRow}>
          {[1, 0].map((value) => (
            <TouchableRipple
              key={value}
              accessibilityRole="button"
              accessibilityLabel={strings.play.tutorial.selectAnswerLabel(value as 0 | 1)}
              onPress={() => {
                if (value === expectedAnswer) {
                  setShowAnswerRetry(false);
                  void advanceLesson();
                  return;
                }
                setShowAnswerRetry(true);
              }}
              style={styles.answerButton}
            >
              <Text style={styles.answerButtonText}>{strings.play.tutorial.selectAnswerLabel(value as 0 | 1)}</Text>
            </TouchableRipple>
          ))}
        </View>
      </View>
    ) : (
      <TouchableRipple
        accessibilityRole="button"
        accessibilityLabel={lesson.continueLabel}
        onPress={() => {
          void advanceLesson();
        }}
        style={styles.continueButton}
      >
        <Text style={styles.continueButtonText}>{lesson.continueLabel}</Text>
      </TouchableRipple>
    )
  );

  return (
    <PuzzleTutorialScaffold
      title={lesson.title}
      body={lesson.body}
      lessonCount={lessons.length}
      activeLessonIndex={lessonIndex}
      boardMinHeight={196}
      feedbackMinHeight={80}
      onNextLesson={() => {
        void advanceLesson();
      }}
      onPreviousLesson={goToPreviousLesson}
      board={(
        <NonogramPuzzleGrid
          puzzle={lesson.puzzle}
          board={lesson.board}
          interactive={false}
          nextMoveEvidenceCells={lesson.evidenceCells}
          nextMoveTargetCells={lesson.targetCells}
          nextMoveHighlightRows={lesson.highlightRows}
          nextMoveHighlightCols={lesson.highlightCols}
        />
      )}
      feedback={feedbackContent}
      controls={controlsContent}
    />
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  feedbackStack: {
    gap: 8,
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
  retryText: {
    fontSize: 13,
    lineHeight: 18,
    color: theme.difficultyHard,
    textAlign: 'center',
    fontWeight: '600',
  },
  answerControls: {
    gap: 10,
  },
  answerPrompt: {
    fontSize: 13,
    lineHeight: 18,
    color: theme.textSecondary,
    textAlign: 'center',
    fontWeight: '600',
  },
  answerButtonsRow: {
    flexDirection: 'row',
    gap: 10,
  },
  answerButton: {
    flex: 1,
    paddingVertical: 14,
    borderRadius: 12,
    backgroundColor: theme.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  answerButtonText: {
    fontSize: 15,
    fontWeight: '700',
    color: theme.onPrimary,
  },
  continueButton: {
    paddingVertical: 16,
    borderRadius: 14,
    backgroundColor: theme.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  continueButtonText: {
    fontSize: 17,
    fontWeight: '700',
    color: theme.onPrimary,
  },
});
