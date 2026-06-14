// src/app/screens/WelcomeScreen.tsx
import React, { useCallback, useMemo, useState } from 'react';
import { View, Text, StyleSheet, ScrollView, useWindowDimensions } from 'react-native';
import { Button } from 'react-native-paper';
import type { StackScreenProps } from '@react-navigation/stack';
import { Gesture, GestureDetector } from 'react-native-gesture-handler';
import AppScreen from '../components/AppScreen';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import type { RootStackParamList } from '../navigation/types';
import { getLocalizedGameNameList } from '../shell/games/gameNameList';
import { withAlpha } from '../utils/color';
import { markWelcomeSeen } from '../utils/settingsStorage';
import type { Theme } from '../theme';

type Props = StackScreenProps<RootStackParamList, 'Welcome'>;
const SWIPE_DISTANCE_THRESHOLD = 48;
const SWIPE_VELOCITY_THRESHOLD = 420;

interface Slide {
  emoji: string;
  title: string;
  body: string;
}

type WelcomeLayoutMetrics = {
  contentMaxWidth: number;
  paddingHorizontal: number;
  paddingVertical: number;
  sectionGap: number;
  heroBadgeSize: number;
  heroIconSize: number;
  titleFontSize: number;
  titleLineHeight: number;
  bodyFontSize: number;
  bodyLineHeight: number;
  dotsMarginTop: number;
  buttonMarginTop: number;
  buttonWidth: number;
};

function getWelcomeLayoutMetrics(windowHeight: number): WelcomeLayoutMetrics {
  if (windowHeight <= 700) {
    return {
      contentMaxWidth: 400,
      paddingHorizontal: 20,
      paddingVertical: 20,
      sectionGap: 16,
      heroBadgeSize: 88,
      heroIconSize: 40,
      titleFontSize: 24,
      titleLineHeight: 30,
      bodyFontSize: 15,
      bodyLineHeight: 22,
      dotsMarginTop: 4,
      buttonMarginTop: 10,
      buttonWidth: 220,
    };
  }

  if (windowHeight <= 820) {
    return {
      contentMaxWidth: 420,
      paddingHorizontal: 24,
      paddingVertical: 24,
      sectionGap: 18,
      heroBadgeSize: 96,
      heroIconSize: 44,
      titleFontSize: 26,
      titleLineHeight: 32,
      bodyFontSize: 16,
      bodyLineHeight: 23,
      dotsMarginTop: 6,
      buttonMarginTop: 12,
      buttonWidth: 232,
    };
  }

  return {
    contentMaxWidth: 440,
    paddingHorizontal: 32,
    paddingVertical: 28,
    sectionGap: 20,
    heroBadgeSize: 104,
    heroIconSize: 48,
    titleFontSize: 28,
    titleLineHeight: 34,
    bodyFontSize: 16,
    bodyLineHeight: 24,
    dotsMarginTop: 8,
    buttonMarginTop: 14,
    buttonWidth: 244,
  };
}

function clampIndex(value: number, max: number): number {
  return Math.max(0, Math.min(max, value));
}

export default function WelcomeScreen({ navigation }: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const { height: windowHeight } = useWindowDimensions();
  const layout = useMemo(() => getWelcomeLayoutMetrics(windowHeight), [windowHeight]);
  const s = useMemo(() => makeStyles(theme, layout), [theme, layout]);
  const puzzleNames = getLocalizedGameNameList();
  const slides = useMemo<Slide[]>(() => ([
    { emoji: '🧩', ...strings.welcome.slides[0] },
    {
      emoji: '🎮',
      ...strings.welcome.slides[1],
      body: strings.welcome.slides[1].body.replace('{games}', puzzleNames),
    },
    { emoji: '🔒', ...strings.welcome.slides[2] },
  ]), [puzzleNames, strings]);
  const [index, setIndex] = useState(0);
  const slide = slides[index];
  const isLast = index === slides.length - 1;
  const goToPreviousSlide = useCallback(() => {
    setIndex((current) => clampIndex(current - 1, slides.length - 1));
  }, [slides.length]);
  const goToNextSlide = useCallback(() => {
    setIndex((current) => clampIndex(current + 1, slides.length - 1));
  }, [slides.length]);

  const handleNext = useCallback(async () => {
    if (!isLast) {
      goToNextSlide();
      return;
    }

    await markWelcomeSeen();
    navigation.replace('MainTabs');
  }, [goToNextSlide, isLast, navigation]);
  const swipeGesture = useMemo(() => Gesture.Pan()
    .runOnJS(true)
    .activeOffsetX([-16, 16])
    .failOffsetY([-20, 20])
    .onEnd((event) => {
      const shouldGoNext = event.translationX <= -SWIPE_DISTANCE_THRESHOLD
        || event.velocityX <= -SWIPE_VELOCITY_THRESHOLD;
      const shouldGoPrevious = event.translationX >= SWIPE_DISTANCE_THRESHOLD
        || event.velocityX >= SWIPE_VELOCITY_THRESHOLD;

      if (shouldGoNext) {
        goToNextSlide();
        return;
      }

      if (shouldGoPrevious) {
        goToPreviousSlide();
      }
    }), [goToNextSlide, goToPreviousSlide]);

  return (
    <AppScreen contentStyle={s.container}>
      <GestureDetector gesture={swipeGesture}>
        <ScrollView contentContainerStyle={s.scrollContent} showsVerticalScrollIndicator={false}>
          <View style={s.content}>
            <View style={s.heroBadge}>
              <Text style={s.heroEmoji}>{slide.emoji}</Text>
            </View>

            <Text style={s.title}>{slide.title}</Text>
            <Text style={s.body}>{slide.body}</Text>

            <View style={s.dots}>
              {slides.map((_, i) => (
                <View key={i} style={[s.dot, i === index && s.dotActive]} />
              ))}
            </View>

            <Button
              mode="contained"
              onPress={() => { void handleNext(); }}
              style={s.btn}
              labelStyle={s.btnLabel}
            >
              {isLast ? strings.common.getStarted : strings.common.next}
            </Button>
          </View>
        </ScrollView>
      </GestureDetector>
    </AppScreen>
  );
}

const makeStyles = (theme: Theme, layout: WelcomeLayoutMetrics) => StyleSheet.create({
  container: { flex: 1, backgroundColor: theme.background },
  scrollContent: {
    flexGrow: 1,
  },
  content: {
    width: '100%',
    maxWidth: layout.contentMaxWidth,
    alignSelf: 'center',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: layout.paddingHorizontal,
    paddingVertical: layout.paddingVertical,
    gap: layout.sectionGap,
  },
  heroBadge: {
    width: layout.heroBadgeSize,
    height: layout.heroBadgeSize,
    borderRadius: layout.heroBadgeSize / 2,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: withAlpha(theme.primary, 0.12),
  },
  heroEmoji: {
    fontSize: layout.heroIconSize,
  },
  title: {
    fontSize: layout.titleFontSize,
    fontWeight: '800',
    color: theme.text,
    textAlign: 'center',
    lineHeight: layout.titleLineHeight,
  },
  body: {
    fontSize: layout.bodyFontSize,
    color: theme.textSecondary,
    textAlign: 'center',
    lineHeight: layout.bodyLineHeight,
  },
  dots: { flexDirection: 'row', gap: 8, marginTop: layout.dotsMarginTop },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: theme.border,
  },
  dotActive: { backgroundColor: theme.primary, width: 20 },
  btn: {
    width: '100%',
    maxWidth: layout.buttonWidth,
    marginTop: layout.buttonMarginTop,
    borderRadius: 14,
  },
  btnLabel: {
    fontSize: 17,
    fontWeight: '700',
  },
});
