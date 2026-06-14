import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { LayoutChangeEvent } from 'react-native';
import { StyleSheet, Text, View } from 'react-native';
import { TouchableRipple } from 'react-native-paper';
import type { StackScreenProps } from '@react-navigation/stack';
import PuzzleTutorialScaffold from '../../../../app/components/PuzzleTutorialScaffold';
import { useLanguage } from '../../../../app/context/LanguageContext';
import { useTheme } from '../../../../app/context/ThemeContext';
import type { RootStackParamList } from '../../../../app/navigation/types';
import type { Theme } from '../../../../app/theme';
import { withAlpha } from '../../../../app/utils/color';
import { markPuzzleTutorialSeen } from '../../../../app/utils/settingsStorage';
import {
  getWordSearchTutorialLessons,
  type WordSearchTutorialLessonKey,
} from '../../content/tutorialLessons';
import { getWordSearchStrings } from '../../content/strings';
import type { WordSearchPuzzle, WordSearchSelection } from '../../types';
import WordSearchPuzzleGrid from '../play/components/WordSearchPuzzleGrid';

type Props = StackScreenProps<RootStackParamList, 'Tutorial'>;
type AnswerState = 'idle' | 'wrong' | 'correct';

const LESSON_KEYS: readonly WordSearchTutorialLessonKey[] = [
  'win-condition',
  'selection',
  'no-penalty',
  'hidden-word',
];

const STAR_PATH = [
  { row: 0, col: 0 },
  { row: 0, col: 1 },
  { row: 0, col: 2 },
  { row: 0, col: 3 },
] as const;

const MOON_PATH = [
  { row: 2, col: 0 },
  { row: 3, col: 0 },
  { row: 4, col: 0 },
  { row: 5, col: 0 },
] as const;

const HIDDEN_PATH = [
  { row: 1, col: 1 },
  { row: 2, col: 2 },
  { row: 3, col: 3 },
] as const;

const TUTORIAL_PUZZLE: WordSearchPuzzle = {
  id: 'wordsearch-tutorial',
  difficulty: 'easy',
  rows: 6,
  cols: 6,
  language: 'en',
  themeId: 'starter',
  grid: [
    ['S', 'T', 'A', 'R', 'L', 'P'],
    ['Q', 'S', 'K', 'W', 'E', 'R'],
    ['M', 'B', 'U', 'T', 'Y', 'U'],
    ['O', 'H', 'I', 'N', 'A', 'S'],
    ['O', 'J', 'K', 'L', 'D', 'F'],
    ['N', 'Z', 'X', 'C', 'V', 'B'],
  ],
  words: [
    { id: 'star', word: 'STAR', positions: [...STAR_PATH] },
    { id: 'moon', word: 'MOON', positions: [...MOON_PATH] },
  ],
  hiddenWord: {
    word: 'SUN',
    clue: 'Hidden bonus word',
    positions: [...HIDDEN_PATH],
  },
};

function makeSelection(path: readonly { row: number; col: number }[]): WordSearchSelection {
  const start = path[0] ?? { row: 0, col: 0 };
  const end = path[path.length - 1] ?? start;
  return {
    start: { ...start },
    end: { ...end },
    path: path.map((cell) => ({ ...cell })),
  };
}

export default function WordSearchTutorialScreen({ navigation, route }: Props) {
  const { resolvedLanguage } = useLanguage();
  const { theme } = useTheme();
  const strings = useMemo(() => getWordSearchStrings(), [resolvedLanguage]);
  const copies = useMemo(() => getWordSearchTutorialLessons(), [resolvedLanguage]);
  const styles = useMemo(() => makeStyles(theme), [theme]);
  const [lessonIndex, setLessonIndex] = useState(0);
  const [answerState, setAnswerState] = useState<AnswerState>('idle');
  const [feedbackText, setFeedbackText] = useState<string | null>(null);
  const [boardBounds, setBoardBounds] = useState({ width: 0, height: 0 });
  const advanceTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const lessonKey = LESSON_KEYS[lessonIndex];
  const lessonCopy = copies[lessonKey];
  const isCheckpoint = lessonKey === 'selection';
  const isLastLesson = lessonIndex === LESSON_KEYS.length - 1;

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
    if (isCheckpoint && answerState !== 'correct') {
      return;
    }
    if (isLastLesson) {
      await exitTutorial();
      return;
    }
    setLessonIndex((current) => current + 1);
  }, [answerState, exitTutorial, isCheckpoint, isLastLesson]);

  const goToPreviousLesson = useCallback(() => {
    if (lessonIndex <= 0) {
      return;
    }
    clearAdvanceTimeout();
    setLessonIndex((current) => current - 1);
  }, [clearAdvanceTimeout, lessonIndex]);

  const handleCheckpointChoice = useCallback((isValidPath: boolean) => {
    clearAdvanceTimeout();
    if (!isValidPath) {
      setAnswerState('wrong');
      setFeedbackText(strings.tutorial.checkpoint.wrongFeedback);
      return;
    }

    setAnswerState('correct');
    setFeedbackText(strings.tutorial.checkpoint.correctFeedback);
    advanceTimeoutRef.current = setTimeout(() => {
      void advanceLesson();
      advanceTimeoutRef.current = null;
    }, 900);
  }, [advanceLesson, clearAdvanceTimeout, strings.tutorial.checkpoint.correctFeedback, strings.tutorial.checkpoint.wrongFeedback]);

  // Lesson 1 — win-condition: fresh grid, both words shown as dim evidence
  // Lesson 2 — selection: STAR actively selected (tempSelection), checkpoint
  // Lesson 3 — no-penalty: STAR found, MOON's start cell highlighted as next target
  // Lesson 4 — hidden-word: both found, first hidden-word cell highlighted
  const foundWordIds: string[] = lessonKey === 'hidden-word'
    ? ['star', 'moon']
    : lessonKey === 'no-penalty'
      ? ['star']
      : [];
  const tempSelection = lessonKey === 'selection'
    ? makeSelection(STAR_PATH)
    : null;
  const evidenceCells: readonly { row: number; col: number }[] = (() => {
    switch (lessonKey) {
      case 'win-condition': return [...STAR_PATH, ...MOON_PATH];
      case 'selection': return [...STAR_PATH];
      case 'no-penalty': return [MOON_PATH[0]!];
      case 'hidden-word': return [HIDDEN_PATH[0]!];
      default: return [];
    }
  })();
  const targetCells: readonly { row: number; col: number }[] = (() => {
    switch (lessonKey) {
      case 'win-condition': return [...STAR_PATH];
      case 'selection': return [...STAR_PATH];
      case 'no-penalty': return [MOON_PATH[0]!];
      case 'hidden-word': return [HIDDEN_PATH[0]!];
      default: return [];
    }
  })();

  const feedback = (
    <View style={styles.feedbackStack}>
      <Text style={styles.progressText}>
        {strings.tutorial.progressLabel(lessonIndex + 1, LESSON_KEYS.length)}
      </Text>
      <Text style={styles.summaryText}>{lessonCopy.summary}</Text>
      {feedbackText ? (
        <Text style={[styles.feedbackText, answerState === 'wrong' ? styles.feedbackTextWrong : null]}>
          {feedbackText}
        </Text>
      ) : null}
    </View>
  );

  const controls = isCheckpoint ? (
    <View style={styles.answerControls}>
      <Text style={styles.answerPrompt}>{strings.tutorial.checkpoint.prompt}</Text>
      <View style={styles.answerButtonsRow}>
        <TouchableRipple
          accessibilityRole="button"
          disabled={answerState === 'correct'}
          onPress={() => handleCheckpointChoice(true)}
          style={styles.answerButton}
        >
          <Text style={styles.answerButtonText}>{strings.tutorial.checkpoint.validOption}</Text>
        </TouchableRipple>
        <TouchableRipple
          accessibilityRole="button"
          disabled={answerState === 'correct'}
          onPress={() => handleCheckpointChoice(false)}
          style={styles.answerButton}
        >
          <Text style={styles.answerButtonText}>{strings.tutorial.checkpoint.invalidOption}</Text>
        </TouchableRipple>
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
      <Text style={styles.continueButtonText}>
        {lessonCopy.continueLabel}
      </Text>
    </TouchableRipple>
  );

  return (
    <PuzzleTutorialScaffold
      title={lessonCopy.title}
      body={lessonCopy.body}
      lessonCount={LESSON_KEYS.length}
      activeLessonIndex={lessonIndex}
      boardMinHeight={260}
      feedbackMinHeight={110}
      onNextLesson={() => {
        void advanceLesson();
      }}
      onPreviousLesson={goToPreviousLesson}
      board={(
        <View style={styles.boardHost} onLayout={handleBoardLayout}>
          {boardBounds.width > 0 ? (
            <WordSearchPuzzleGrid
              puzzle={TUTORIAL_PUZZLE}
              foundWordIds={foundWordIds}
              tempSelection={tempSelection}
              containerWidth={boardBounds.width}
              containerHeight={boardBounds.height}
              interactive={false}
              allowDrag={false}
              nextMoveEvidenceCells={evidenceCells}
              nextMoveTargetCells={targetCells}
            />
          ) : null}
        </View>
      )}
      feedback={feedback}
      controls={controls}
    />
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  boardHost: {
    width: '100%',
    minHeight: 240,
    alignItems: 'center',
    justifyContent: 'center',
  },
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
  feedbackText: {
    fontSize: 13,
    lineHeight: 20,
    color: theme.textSecondary,
    textAlign: 'center',
    fontWeight: '600',
  },
  feedbackTextWrong: {
    color: theme.difficultyHard,
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
    backgroundColor: withAlpha(theme.primary, 0.16),
    borderWidth: 1,
    borderColor: withAlpha(theme.primary, 0.4),
  },
  answerButtonText: {
    fontSize: 14,
    fontWeight: '800',
    color: theme.text,
    textAlign: 'center',
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
