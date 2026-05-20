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
  progressLabel: string;
  statusText?: string | null;
  title: string;
  body?: string | null;
  board: React.ReactNode;
  lessonCount: number;
  activeLessonIndex: number;
  footer: React.ReactNode;
  boardMinHeight?: number;
  onNextLesson: () => void;
  onPreviousLesson: () => void;
}

export default function PuzzleTutorialScaffold({
  backButton,
  progressLabel,
  statusText = null,
  title,
  body = null,
  board,
  lessonCount,
  activeLessonIndex,
  footer,
  boardMinHeight = 184,
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
        {backButton}

        <View style={s.header}>
          <Text style={s.progress}>{progressLabel}</Text>
        </View>

        <View style={s.statusRow}>
          {statusText ? <Text style={s.statusText}>{statusText}</Text> : null}
        </View>

        <View style={s.mainContent}>
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

        <View style={s.footerRegion}>{footer}</View>
      </View>
    </GestureDetector>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.background,
    padding: 20,
    gap: 20,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  progress: {
    fontSize: 14,
    fontWeight: '700',
    color: theme.primaryLight,
    letterSpacing: 0.5,
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
  title: {
    maxWidth: 280,
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
  footerRegion: {
    gap: 14,
    paddingTop: 8,
    paddingBottom: 8,
  },
});
