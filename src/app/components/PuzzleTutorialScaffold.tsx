import React, { useMemo } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Gesture, GestureDetector } from 'react-native-gesture-handler';
import { useTheme } from '../context/ThemeContext';
import type { Theme } from '../theme';
import { withAlpha } from '../utils/color';

const SWIPE_DISTANCE_THRESHOLD = 48;
const SWIPE_VELOCITY_THRESHOLD = 420;

interface PuzzleTutorialScaffoldProps {
  backButton?: React.ReactNode;
  title: string;
  body?: string | null;
  board: React.ReactNode;
  lessonCount: number;
  activeLessonIndex: number;
  feedback?: React.ReactNode;
  controls: React.ReactNode;
  boardMinHeight?: number;
  feedbackMinHeight?: number;
  onNextLesson: () => void;
  onPreviousLesson: () => void;
}

export default function PuzzleTutorialScaffold({
  backButton,
  title,
  body = null,
  board,
  lessonCount,
  activeLessonIndex,
  feedback = null,
  controls,
  boardMinHeight = 184,
  feedbackMinHeight = 72,
  onPreviousLesson,
}: PuzzleTutorialScaffoldProps) {
  const { theme } = useTheme();
  const s = makeStyles(theme);
  const swipeGesture = useMemo(() => Gesture.Pan()
    .runOnJS(true)
    .activeOffsetX([-16, 16])
    .failOffsetY([-20, 20])
    .onEnd((event) => {
      const shouldGoPrevious = event.translationX >= SWIPE_DISTANCE_THRESHOLD
        || event.velocityX >= SWIPE_VELOCITY_THRESHOLD;

      if (shouldGoPrevious && activeLessonIndex > 0) {
        onPreviousLesson();
      }
    }), [activeLessonIndex, onPreviousLesson]);

  return (
    <GestureDetector gesture={swipeGesture}>
      <View style={s.container}>
        <View style={s.topBar}>
          <View style={s.backButtonSlot}>
            {backButton}
          </View>
        </View>

        <View style={s.lessonRegion}>
          <Text style={s.title}>{title}</Text>
          {body ? <Text style={s.body}>{body}</Text> : null}

          <View style={[s.boardWrap, { minHeight: boardMinHeight }]}>
            {board}
          </View>

          <View style={s.dots}>
            {Array.from({ length: lessonCount }, (_, index) => (
              <View
                key={`dot-${index}`}
                style={[s.dot, index === activeLessonIndex ? s.dotActive : null]}
              />
            ))}
          </View>
        </View>

        <View style={[s.feedbackRegion, { minHeight: feedbackMinHeight }]}>
          {feedback ?? <View style={s.feedbackPlaceholder} />}
        </View>

        <View style={s.controlsRegion}>{controls}</View>
      </View>
    </GestureDetector>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.background,
    padding: 20,
    gap: 16,
  },
  topBar: {
    width: '100%',
    minHeight: 40,
    justifyContent: 'center',
  },
  backButtonSlot: {
    alignSelf: 'flex-start',
  },
  lessonRegion: {
    flex: 1,
    width: '100%',
    alignItems: 'center',
    justifyContent: 'flex-start',
    gap: 18,
    paddingTop: 8,
  },
  title: {
    fontSize: 20,
    lineHeight: 34,
    color: theme.text,
    fontWeight: '800',
    textAlign: 'center',
  },
  body: {
    fontSize: 15,
    lineHeight: 23,
    color: theme.textSecondary,
    textAlign: 'center',
  },
  boardWrap: {
    width: '100%',
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
    backgroundColor: withAlpha(theme.border, 0.8),
  },
  dotActive: {
    width: 20,
    backgroundColor: theme.primary,
  },
  feedbackRegion: {
    width: '100%',
    flexShrink: 0,
    justifyContent: 'center',
  },
  feedbackPlaceholder: {
    width: '100%',
  },
  controlsRegion: {
    flexShrink: 0,
    gap: 14,
    paddingBottom: 8,
  },
});
