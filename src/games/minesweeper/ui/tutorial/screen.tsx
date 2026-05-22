import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import type { StackScreenProps } from '@react-navigation/stack';
import PuzzleTutorialScaffold from '../../../../app/components/PuzzleTutorialScaffold';
import { useLanguage } from '../../../../app/context/LanguageContext';
import { useTheme } from '../../../../app/context/ThemeContext';
import type { RootStackParamList } from '../../../../app/navigation/types';
import type { Theme } from '../../../../app/theme';
import { markPuzzleTutorialSeen } from '../../../../app/utils/settingsStorage';
import { withAlpha } from '../../../../app/utils/color';
import MinesweeperTutorialBoard from './components/MinesweeperTutorialBoard';
import { getMinesweeperI18n } from '../../content/i18n';
import {
  getMinesweeperTutorialLessons,
  type MinesweeperTutorialAction,
} from '../../content/tutorialLessons';

type Props = StackScreenProps<RootStackParamList, 'Tutorial'>;
type TutorialAnswerState = 'idle' | 'wrong' | 'correct';

const ADVANCE_DELAY_MS = 1800;

export default function TutorialScreen({ navigation, route }: Props) {
  const { strings, resolvedLanguage } = useLanguage();
  const { theme } = useTheme();
  const s = useMemo(() => makeStyles(theme), [theme]);
  const lessons = useMemo(() => getMinesweeperTutorialLessons(), [resolvedLanguage]);
  const tutorialUi = useMemo(() => getMinesweeperI18n().tutorialUi, [resolvedLanguage]);
  const [lessonIndex, setLessonIndex] = useState(0);
  const [selectedAction, setSelectedAction] = useState<MinesweeperTutorialAction | null>(null);
  const [answerState, setAnswerState] = useState<TutorialAnswerState>('idle');
  const advanceTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const lesson = lessons[lessonIndex];
  const isLastLesson = lessonIndex === lessons.length - 1;
  const tutorialActionLabel = isLastLesson
    ? tutorialUi.exitLabel.end
    : tutorialUi.exitLabel.skip;
  const actionLesson = lesson.kind === 'action' ? lesson : null;
  const board = lesson.kind === 'action'
    ? lesson.initialBoard
    : lesson.board;

  const clearAdvanceTimeout = useCallback(() => {
    if (advanceTimeoutRef.current) {
      clearTimeout(advanceTimeoutRef.current);
      advanceTimeoutRef.current = null;
    }
  }, []);

  useEffect(() => {
    setSelectedAction(null);
    setAnswerState('idle');
  }, [lesson]);

  useEffect(() => {
    return () => {
      clearAdvanceTimeout();
    };
  }, [clearAdvanceTimeout]);

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

    setSelectedAction(null);
    setAnswerState('idle');
    setLessonIndex((current) => current + 1);
  }, [exitTutorial, isLastLesson]);

  const goToPreviousLesson = useCallback(() => {
    if (lessonIndex <= 0) {
      return;
    }

    clearAdvanceTimeout();
    setSelectedAction(null);
    setAnswerState('idle');
    setLessonIndex((current) => current - 1);
  }, [clearAdvanceTimeout, lessonIndex]);

  const submitAction = useCallback((action: MinesweeperTutorialAction) => {
    if (!actionLesson || answerState === 'correct') {
      return;
    }

    setSelectedAction(action);

    if (action !== actionLesson.expectedAction) {
      setAnswerState('wrong');
      return;
    }

    setAnswerState('correct');
    clearAdvanceTimeout();

    advanceTimeoutRef.current = setTimeout(() => {
      void advanceLesson();
      advanceTimeoutRef.current = null;
    }, ADVANCE_DELAY_MS);
  }, [actionLesson, advanceLesson, answerState, clearAdvanceTimeout]);

  const handlePressFocus = useCallback(() => {
    if (!selectedAction) {
      return;
    }

    submitAction(selectedAction);
  }, [selectedAction, submitAction]);

  const statusText = lesson.kind === 'action' && answerState === 'correct'
     ? (isLastLesson ? tutorialUi.status.finishing : tutorialUi.status.nextLesson)
     : null;
  const feedbackText = lesson.kind === 'info'
    ? lesson.summary
    : !selectedAction
      ? null
      : answerState === 'wrong'
        ? lesson.retry
        : answerState === 'correct'
          ? lesson.success
          : null;

  const feedbackContent = (
    <View style={s.feedbackStack}>
      {statusText ? <Text style={s.statusText}>{statusText}</Text> : null}
      {feedbackText ? (
        <Text style={s.feedbackText}>{feedbackText}</Text>
      ) : null}
    </View>
  );

  const controlsContent = (
    <>
      <Text style={s.actionPrompt}>{lesson.prompt}</Text>
      {lesson.kind === 'action' ? (
        <View style={s.actionButtons}>
          {([
            ['reveal', strings.common.reveal],
            ['flag', strings.common.flag],
          ] as const).map(([action, label]) => (
            <TouchableOpacity
              key={action}
              accessibilityRole="button"
              accessibilityLabel={label}
              activeOpacity={0.82}
              onPress={() => {
                submitAction(action);
              }}
              style={[
                s.actionButton,
                selectedAction === action ? s.actionButtonSelected : null,
              ]}
            >
              <Text style={s.actionButtonText}>{label}</Text>
            </TouchableOpacity>
          ))}
        </View>
      ) : (
        <TouchableOpacity
          accessibilityRole="button"
          accessibilityLabel={lesson.continueLabel}
          activeOpacity={0.82}
          onPress={() => {
            void advanceLesson();
          }}
          style={s.continueButton}
        >
          <Text style={s.continueButtonText}>{lesson.continueLabel}</Text>
        </TouchableOpacity>
      )}
    </>
  );

  return (
    <PuzzleTutorialScaffold
      backButton={(
        <TouchableOpacity
          accessibilityRole="button"
          accessibilityLabel={tutorialActionLabel}
          activeOpacity={0.82}
          onPress={() => {
            void exitTutorial();
          }}
          style={s.exitButton}
        >
          <Text style={s.exitButtonText}>{tutorialActionLabel}</Text>
        </TouchableOpacity>
      )}
      title={lesson.title}
      body={lesson.body}
      lessonCount={lessons.length}
      activeLessonIndex={lessonIndex}
      boardMinHeight={184}
      feedbackMinHeight={72}
      onNextLesson={() => {
        void advanceLesson();
      }}
      onPreviousLesson={goToPreviousLesson}
      board={(
        <MinesweeperTutorialBoard
          board={board}
          focusCell={lesson.kind === 'action' ? lesson.focusCell : null}
          answerState={answerState}
          onPressFocus={handlePressFocus}
        />
      )}
      feedback={feedbackContent}
      controls={controlsContent}
    />
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  exitButton: {
    alignSelf: 'flex-start',
    paddingVertical: 8,
    paddingHorizontal: 10,
    borderRadius: 10,
  },
  exitButtonText: {
    fontSize: 14,
    fontWeight: '700',
    color: theme.textSecondary,
  },
  actionPrompt: {
    fontSize: 16,
    lineHeight: 24,
    color: theme.text,
    fontWeight: '600',
    textAlign: 'center',
  },
  feedbackStack: {
    gap: 8,
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
  actionButtons: {
    flexDirection: 'row',
    gap: 10,
  },
  actionButton: {
    flex: 1,
    paddingVertical: 16,
    borderRadius: 14,
    borderWidth: 2,
    borderColor: theme.border,
    backgroundColor: theme.surface,
    alignItems: 'center',
    justifyContent: 'center',
  },
  actionButtonSelected: {
    borderColor: theme.primary,
    backgroundColor: withAlpha(theme.primary, 0.12),
  },
  actionButtonText: {
    fontSize: 18,
    fontWeight: '800',
    color: theme.text,
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
