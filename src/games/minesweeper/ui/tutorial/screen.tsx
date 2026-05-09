import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
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
import MinesweeperTutorialBoard from './components/MinesweeperTutorialBoard';
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
  const [lessonIndex, setLessonIndex] = useState(0);
  const [selectedAction, setSelectedAction] = useState<MinesweeperTutorialAction | null>(null);
  const [answerState, setAnswerState] = useState<TutorialAnswerState>('idle');
  const advanceTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const lesson = lessons[lessonIndex];
  const isReplay = route.params.entry === 'howToPlay';
  const isLastLesson = lessonIndex === lessons.length - 1;
  const actionLesson = lesson.kind === 'action' ? lesson : null;
  const board = lesson.kind === 'action'
    ? (answerState === 'correct' ? lesson.resultBoard : lesson.initialBoard)
    : lesson.board;

  useEffect(() => {
    setSelectedAction(null);
    setAnswerState('idle');
  }, [lesson]);

  useEffect(() => {
    return () => {
      if (advanceTimeoutRef.current) {
        clearTimeout(advanceTimeoutRef.current);
      }
    };
  }, []);

  const exitLabel = useMemo(() => {
    if (isReplay || lessonIndex > 0) {
      return resolvedLanguage === 'nl' ? 'Tutorial beëindigen' : 'End tutorial';
    }
    return resolvedLanguage === 'nl' ? 'Tutorial overslaan' : 'Skip tutorial';
  }, [isReplay, lessonIndex, resolvedLanguage]);

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

    setSelectedAction(null);
    setAnswerState('idle');
    setLessonIndex((current) => current + 1);
  }, [exitTutorial, isLastLesson]);

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
    if (advanceTimeoutRef.current) {
      clearTimeout(advanceTimeoutRef.current);
      advanceTimeoutRef.current = null;
    }
    advanceTimeoutRef.current = setTimeout(() => {
      void advanceLesson();
      advanceTimeoutRef.current = null;
    }, ADVANCE_DELAY_MS);
  }, [actionLesson, advanceLesson, answerState]);

  const handlePressFocus = useCallback(() => {
    if (!selectedAction) {
      return;
    }

    submitAction(selectedAction);
  }, [selectedAction, submitAction]);

  const statusText = lesson.kind === 'action' && answerState === 'correct'
     ? (isLastLesson
        ? (resolvedLanguage === 'nl' ? 'Tutorial wordt afgerond…' : 'Tutorial finishing…')
        : (resolvedLanguage === 'nl' ? 'Volgende les start…' : 'Next lesson starting…'))
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
        <Text style={s.progress}>{`${resolvedLanguage === 'nl' ? 'Les' : 'Lesson'} ${lessonIndex + 1}`}</Text>
        <TouchableOpacity onPress={() => void exitTutorial()} activeOpacity={0.8}>
          <Text style={s.exitText}>{exitLabel}</Text>
        </TouchableOpacity>
      </View>

      <View style={s.statusRow}>
        {statusText ? <Text style={s.statusText}>{statusText}</Text> : null}
      </View>

      <View style={s.mainContent}>
        <Text style={s.promptText}>{lesson.title}</Text>
        <Text style={s.body}>{lesson.body}</Text>

        <View style={s.boardWrap}>
          <MinesweeperTutorialBoard
            board={board}
            focusCell={lesson.kind === 'action' ? lesson.focusCell : null}
            answerState={answerState}
            onPressFocus={handlePressFocus}
          />
        </View>

        <View style={s.dots}>
          {lessons.map((_, index) => (
            <View key={index} style={[s.dot, index === lessonIndex ? s.dotActive : null]} />
          ))}
        </View>
      </View>

      <View style={s.actionArea}>
        <Text style={s.actionPrompt}>{lesson.prompt}</Text>
        <View style={s.feedbackSlot}>
          {feedbackText ? (
            <Text style={s.feedbackText}>{feedbackText}</Text>
          ) : null}
        </View>
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
  statusRow: {
    minHeight: 20,
    alignItems: 'flex-end',
  },
  statusText: {
    fontSize: 12,
    fontWeight: '600',
    color: theme.textSecondary,
  },
  mainContent: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 18,
  },
  promptText: {
    maxWidth: 260,
    fontSize: 28,
    lineHeight: 34,
    color: theme.text,
    fontWeight: '800',
    textAlign: 'center',
  },
  body: {
    maxWidth: 280,
    fontSize: 15,
    lineHeight: 23,
    color: theme.textSecondary,
    textAlign: 'center',
  },
  boardWrap: {
    minHeight: 184,
    alignItems: 'center',
    justifyContent: 'center',
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
  actionArea: {
    gap: 14,
    paddingTop: 8,
    paddingBottom: 8,
  },
  actionPrompt: {
    fontSize: 16,
    lineHeight: 24,
    color: theme.text,
    fontWeight: '600',
    textAlign: 'center',
  },
  feedbackSlot: {
    minHeight: 52,
    justifyContent: 'center',
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
