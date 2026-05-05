import React, { useCallback, useEffect, useMemo, useRef } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Animated } from 'react-native';
import type { StackScreenProps } from '@react-navigation/stack';
import AppScreen from '../components/AppScreen';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { returnToHome } from '../navigation/returnToHome';
import { withAlpha } from '../utils/color';
import { formatElapsed, formatDifficultyLabel, getDifficultyColor } from '../utils/format';
import { startGame } from '../utils/gameNavigation';
import type { RootStackParamList } from '../navigation/types';
import type { CompletionVariant } from '../completion/types';
import type { Theme } from '../theme';

const CELEBRATION_ICONS = ['🎉', '🏆', '⭐', '✨', '🎈', '💜'] as const;

type Props = StackScreenProps<RootStackParamList, 'Completion'>;

type VariantCopy = {
  eyebrow: string;
  title: string;
  body: string;
};

type AccentKind = 'sparkle' | 'ring';

type AccentConfig = {
  kind: AccentKind;
  top: number;
  left?: number;
  right?: number;
  size: number;
  color: string;
  travelX: number;
  travelY: number;
  minOpacity: number;
  maxOpacity: number;
  startScale: number;
  endScale: number;
};

function getVariantCopy(
  variant: CompletionVariant,
  strings: ReturnType<typeof useLanguage>['strings'],
): VariantCopy {
  if (variant === 'new-high-score') {
    return {
      eyebrow: strings.completion.newHighScoreEyebrow,
      title: strings.completion.newHighScoreTitle,
      body: strings.completion.newHighScoreBody,
    };
  }

  if (variant === 'first-score') {
    return {
      eyebrow: strings.completion.firstScoreEyebrow,
      title: strings.completion.firstScoreTitle,
      body: strings.completion.firstScoreBody,
    };
  }

  return {
    eyebrow: strings.completion.solvedEyebrow,
    title: strings.completion.solvedTitle,
    body: strings.completion.solvedBody,
  };
}

function pickCelebrationIcon(score: number, accuracy: number, variant: CompletionVariant): string {
  const seed = Math.max(0, score) + accuracy + variant.length;
  return CELEBRATION_ICONS[seed % CELEBRATION_ICONS.length];
}

function buildHeroAccents(theme: Theme, difficultyColor: string): AccentConfig[] {
  return [
    {
      kind: 'ring',
      top: 20,
      left: 18,
      size: 34,
      color: withAlpha(theme.primaryLight, 0.18),
      travelX: 6,
      travelY: -8,
      minOpacity: 0.12,
      maxOpacity: 0.34,
      startScale: 0.94,
      endScale: 1.06,
    },
    {
      kind: 'sparkle',
      top: 44,
      right: 24,
      size: 18,
      color: withAlpha(difficultyColor, 0.84),
      travelX: -6,
      travelY: 8,
      minOpacity: 0.2,
      maxOpacity: 0.74,
      startScale: 1,
      endScale: 0.82,
    },
    {
      kind: 'sparkle',
      top: 132,
      left: 28,
      size: 16,
      color: withAlpha(theme.success, 0.8),
      travelX: 6,
      travelY: -8,
      minOpacity: 0.22,
      maxOpacity: 0.86,
      startScale: 0.92,
      endScale: 1.12,
    },
    {
      kind: 'ring',
      top: 160,
      right: 26,
      size: 32,
      color: withAlpha(theme.textSecondary, 0.18),
      travelX: -8,
      travelY: 10,
      minOpacity: 0.12,
      maxOpacity: 0.28,
      startScale: 0.96,
      endScale: 1.08,
    },
    {
      kind: 'ring',
      top: 32,
      left: 50,
      size: 170,
      color: withAlpha(theme.primaryLight, 0.14),
      travelX: 0,
      travelY: 0,
      minOpacity: 0.12,
      maxOpacity: 0.32,
      startScale: 0.94,
      endScale: 1.08,
    },
    {
      kind: 'ring',
      top: 52,
      left: 70,
      size: 132,
      color: withAlpha(difficultyColor, 0.12),
      travelX: 0,
      travelY: 0,
      minOpacity: 0.08,
      maxOpacity: 0.26,
      startScale: 0.96,
      endScale: 1.06,
    },
    {
      kind: 'sparkle',
      top: 24,
      left: 76,
      size: 18,
      color: withAlpha(theme.primaryLight, 0.92),
      travelX: 6,
      travelY: -8,
      minOpacity: 0.22,
      maxOpacity: 0.88,
      startScale: 0.9,
      endScale: 1.12,
    },
    {
      kind: 'sparkle',
      top: 28,
      right: 72,
      size: 18,
      color: withAlpha(difficultyColor, 0.88),
      travelX: -6,
      travelY: 8,
      minOpacity: 0.2,
      maxOpacity: 0.84,
      startScale: 1,
      endScale: 0.84,
    },
    {
      kind: 'ring',
      top: 82,
      left: 66,
      size: 28,
      color: withAlpha(theme.success, 0.18),
      travelX: -8,
      travelY: 10,
      minOpacity: 0.12,
      maxOpacity: 0.3,
      startScale: 0.96,
      endScale: 1.08,
    },
    {
      kind: 'ring',
      top: 92,
      right: 58,
      size: 34,
      color: withAlpha(theme.textSecondary, 0.18),
      travelX: 8,
      travelY: -10,
      minOpacity: 0.12,
      maxOpacity: 0.3,
      startScale: 0.94,
      endScale: 1.08,
    },
    {
      kind: 'sparkle',
      top: 6,
      left: 122,
      size: 16,
      color: withAlpha(theme.success, 0.82),
      travelX: 6,
      travelY: -8,
      minOpacity: 0.22,
      maxOpacity: 0.82,
      startScale: 0.9,
      endScale: 1.08,
    },
    {
      kind: 'sparkle',
      top: 6,
      right: 116,
      size: 16,
      color: withAlpha(theme.difficultyExpert, 0.82),
      travelX: -6,
      travelY: 8,
      minOpacity: 0.2,
      maxOpacity: 0.8,
      startScale: 1,
      endScale: 0.86,
    },
    {
      kind: 'sparkle',
      top: 162,
      left: 132,
      size: 16,
      color: withAlpha(theme.primaryLight, 0.82),
      travelX: -6,
      travelY: 8,
      minOpacity: 0.22,
      maxOpacity: 0.78,
      startScale: 1,
      endScale: 0.84,
    },
    {
      kind: 'sparkle',
      top: 156,
      right: 120,
      size: 15,
      color: withAlpha(difficultyColor, 0.78),
      travelX: 6,
      travelY: -8,
      minOpacity: 0.2,
      maxOpacity: 0.76,
      startScale: 0.9,
      endScale: 1.08,
    },
    {
      kind: 'sparkle',
      top: 156,
      left: 78,
      size: 15,
      color: withAlpha(theme.success, 0.78),
      travelX: 6,
      travelY: -8,
      minOpacity: 0.2,
      maxOpacity: 0.74,
      startScale: 0.9,
      endScale: 1.08,
    },
  ];
}

export default function CompletionScreen({ route, navigation }: Props) {
  const { strings } = useLanguage();
  const { theme, isDark } = useTheme();
  const { outcome, variant } = route.params;
  const { difficulty, score, accuracy, elapsedSeconds, streak, puzzleTypeId } = outcome;
  const s = useMemo(() => makeStyles(theme), [theme]);
  const copy = getVariantCopy(variant, strings);
  const icon = pickCelebrationIcon(score, accuracy, variant);
  const difficultyColor = getDifficultyColor(theme, difficulty);
  const difficultyLabel = formatDifficultyLabel(puzzleTypeId, difficulty);
  const elapsedLabel = formatElapsed(elapsedSeconds);
  const heroAccents = useMemo(() => buildHeroAccents(theme, difficultyColor), [theme, difficultyColor]);
  const showStreakBadge = streak >= 2;

  const pageOpacity = useRef(new Animated.Value(0)).current;
  const contentTranslateY = useRef(new Animated.Value(24)).current;
  const iconFloat = useRef(new Animated.Value(0)).current;
  const accentProgress = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    const floatingLoop = Animated.loop(
      Animated.sequence([
        Animated.timing(iconFloat, {
          toValue: 1,
          duration: 1100,
          useNativeDriver: true,
        }),
        Animated.timing(iconFloat, {
          toValue: 0,
          duration: 1100,
          useNativeDriver: true,
        }),
      ]),
    );

    const accentLoop = Animated.loop(
      Animated.sequence([
        Animated.timing(accentProgress, {
          toValue: 1,
          duration: 1800,
          useNativeDriver: true,
        }),
        Animated.timing(accentProgress, {
          toValue: 0,
          duration: 1800,
          useNativeDriver: true,
        }),
      ]),
    );

    const entranceAnimation = Animated.parallel([
      Animated.timing(pageOpacity, {
        toValue: 1,
        duration: 220,
        useNativeDriver: true,
      }),
      Animated.spring(contentTranslateY, {
        toValue: 0,
        friction: 8,
        tension: 65,
        useNativeDriver: true,
      }),
    ]);

    entranceAnimation.start();
    floatingLoop.start();
    accentLoop.start();

    return () => {
      entranceAnimation.stop();
      floatingLoop.stop();
      accentLoop.stop();
      pageOpacity.stopAnimation();
      contentTranslateY.stopAnimation();
      iconFloat.stopAnimation();
      accentProgress.stopAnimation();
    };
  }, [accentProgress, contentTranslateY, iconFloat, pageOpacity]);

  const iconTranslateY = iconFloat.interpolate({
    inputRange: [0, 1],
    outputRange: [0, -14],
  });

  const handlePlayAgain = useCallback(() => {
    startGame(navigation, puzzleTypeId, difficulty, true);
  }, [difficulty, navigation, puzzleTypeId]);

  const handleGoHome = useCallback(() => {
    returnToHome(navigation);
  }, [navigation]);

  return (
    <AppScreen contentStyle={s.container}>
      <Animated.View
        style={[
          s.content,
          {
            opacity: pageOpacity,
            transform: [{ translateY: contentTranslateY }],
          },
        ]}
      >
        <View style={s.badgeRow}>
          <View style={[s.eyebrow, { borderColor: difficultyColor, backgroundColor: withAlpha(theme.surface, 0.7) }]}>
            <View style={[s.eyebrowDot, { backgroundColor: difficultyColor }]} />
            <Text style={[s.eyebrowText, { color: difficultyColor }]}>{copy.eyebrow}</Text>
          </View>
          {showStreakBadge ? (
            <View style={[s.streakBadge, { borderColor: withAlpha(theme.primaryLight, 0.35), backgroundColor: withAlpha(theme.surface, 0.7) }]}>
              <Text style={s.streakBadgeIcon}>🔥</Text>
              <Text style={[s.streakBadgeText, { color: theme.primaryLight }]}>{strings.completion.streakBadge(streak)}</Text>
            </View>
          ) : null}
        </View>

        <View style={s.heroStage}>
          <View style={s.heroCluster} pointerEvents="none">
            {heroAccents.map((accent, index) => {
              const translateX = accentProgress.interpolate({
                inputRange: [0, 1],
                outputRange: [0, accent.travelX],
              });
              const translateY = accentProgress.interpolate({
                inputRange: [0, 1],
                outputRange: [0, accent.travelY],
              });
              const opacity = accentProgress.interpolate({
                inputRange: [0, 0.5, 1],
                outputRange: [accent.minOpacity, accent.maxOpacity, accent.minOpacity],
              });
              const scale = accentProgress.interpolate({
                inputRange: [0, 0.5, 1],
                outputRange: [accent.startScale, accent.endScale, accent.startScale],
              });

              return (
                <Animated.View
                  key={`${accent.kind}-${index}`}
                  style={[
                    accent.kind === 'ring' ? s.ringAccent : s.sparkleAccent,
                    {
                      top: accent.top,
                      left: accent.left,
                      right: accent.right,
                      width: accent.size,
                      height: accent.size,
                      borderColor: accent.kind === 'ring' ? accent.color : undefined,
                      opacity,
                      transform: [{ translateX }, { translateY }, { scale }],
                    },
                  ]}
                >
                  {accent.kind === 'sparkle' ? (
                    <Text style={[s.sparkleAccentText, { color: accent.color }]}>{'✦'}</Text>
                  ) : null}
                </Animated.View>
              );
            })}

            <Animated.View
              style={[
                s.heroIcon,
                {
                  backgroundColor: withAlpha(theme.primary, isDark ? 0.16 : 0.12),
                  borderColor: withAlpha(theme.primaryLight, 0.3),
                  shadowColor: theme.primary,
                  transform: [{ translateY: iconTranslateY }],
                },
              ]}
            >
              <Text style={s.heroIconText}>{icon}</Text>
            </Animated.View>
          </View>
        </View>

        <View style={s.copySection}>
          <Text style={s.title}>{copy.title}</Text>
          <Text style={s.body}>{copy.body}</Text>
          <Text style={[s.difficultyLabel, { color: difficultyColor }]}>{difficultyLabel}</Text>
        </View>

        <View style={s.scoreSection}>
          <Text style={s.scoreLabel}>{strings.completion.score}</Text>
          <Text style={s.scoreValue}>{score}</Text>

          <View style={s.metaRow}>
            <View style={s.metaItem}>
               <Text style={s.metaLabel}>{strings.completion.elapsedTime}</Text>
              <Text style={s.metaValue}>{elapsedLabel}</Text>
            </View>
            <View style={s.metaDivider} />
            <View style={s.metaItem}>
               <Text style={s.metaLabel}>{strings.completion.accuracy}</Text>
              <Text style={s.metaValue}>{`${accuracy}%`}</Text>
            </View>
          </View>
        </View>

        <View style={s.actionRow}>
          <TouchableOpacity style={[s.actionButton, s.primaryButton]} onPress={handlePlayAgain} activeOpacity={0.82}>
            <Text style={s.primaryButtonText}>{strings.common.playAgain}</Text>
          </TouchableOpacity>
          <TouchableOpacity style={[s.actionButton, s.secondaryButton]} onPress={handleGoHome} activeOpacity={0.82}>
            <Text style={s.secondaryButtonText}>{strings.common.home}</Text>
          </TouchableOpacity>
        </View>
      </Animated.View>
    </AppScreen>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.background,
    paddingHorizontal: 24,
    paddingVertical: 24
  },
  content: {
    flex: 1,
    position: 'relative',
    justifyContent: 'space-between',
  },
  badgeRow: {
    flexDirection: 'row',
    gap: 8,
    flexWrap: 'wrap',
    alignItems: 'flex-start',
  },
  eyebrow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
  },
  eyebrowDot: {
    width: 10,
    height: 10,
    borderRadius: 999,
  },
  eyebrowText: {
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 0.6,
  },
  streakBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
  },
  streakBadgeIcon: {
    fontSize: 13,
  },
  streakBadgeText: {
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 0.4,
  },
  heroStage: {
    height: 318,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  heroCluster: {
    width: 280,
    height: 240,
    position: 'relative',
    alignItems: 'center',
  },
  ringAccent: {
    position: 'absolute',
    borderRadius: 999,
    borderWidth: 1,
  },
  sparkleAccent: {
    position: 'absolute',
    alignItems: 'center',
    justifyContent: 'center',
  },
  sparkleAccentText: {
    fontSize: 18,
    lineHeight: 18,
  },
  heroIcon: {
    marginTop: 78,
    width: 98,
    height: 98,
    borderRadius: 30,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    shadowOpacity: 0.35,
    shadowRadius: 20,
    shadowOffset: { width: 0, height: 14 },
    elevation: 10,
  },
  heroIconText: {
    fontSize: 42,
  },
  copySection: {
    marginTop: -10,
  },
  title: {
    color: theme.text,
    textAlign: 'center',
    fontSize: 31,
    fontWeight: '800',
    lineHeight: 36,
  },
  body: {
    marginTop: 10,
    color: theme.textSecondary,
    textAlign: 'center',
    fontSize: 15,
    lineHeight: 22,
  },
  difficultyLabel: {
    marginTop: 8,
    textAlign: 'center',
    fontSize: 13,
    fontWeight: '700',
    letterSpacing: 0.6,
  },
  scoreSection: {
    marginTop: 20,
    alignItems: 'center',
  },
  scoreLabel: {
    color: theme.textSecondary,
    fontSize: 13,
    fontWeight: '700',
    letterSpacing: 1,
    textTransform: 'uppercase',
  },
  scoreValue: {
    marginTop: 8,
    color: theme.text,
    fontSize: 70,
    fontWeight: '900',
    letterSpacing: -2.5,
    lineHeight: 74,
  },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 18,
    flexWrap: 'wrap',
    marginTop: 14,
  },
  metaItem: {
    alignItems: 'center',
  },
  metaLabel: {
    color: theme.textSecondary,
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 0.8,
    textTransform: 'uppercase',
  },
  metaValue: {
    marginTop: 4,
    color: theme.text,
    fontSize: 22,
    fontWeight: '800',
  },
  metaDivider: {
    width: 1,
    height: 30,
    backgroundColor: withAlpha(theme.border, 0.8),
  },
  actionRow: {
    flexDirection: 'row',
    gap: 12,
    marginTop: 28,
    marginBottom: 4,
  },
  actionButton: {
    flex: 1,
    paddingVertical: 15,
    borderRadius: 14,
    alignItems: 'center',
  },
  primaryButton: {
    backgroundColor: theme.primary,
  },
  primaryButtonText: {
    color: theme.text,
    fontSize: 16,
    fontWeight: '700',
  },
  secondaryButton: {
    backgroundColor: theme.surfaceElevated,
    borderColor: theme.border,
    borderWidth: 1,
  },
  secondaryButtonText: {
    color: theme.text,
    fontSize: 16,
    fontWeight: '700',
  },
});
