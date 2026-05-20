import React, { useCallback, useEffect, useRef } from 'react';
import { Animated, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import type { StackScreenProps } from '@react-navigation/stack';
import OutcomeScreenLayout, { type OutcomeScreenMetrics } from '../components/OutcomeScreenLayout';
import GamePageShell from '../components/GamePageShell';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import type { SolvedResultVariant } from '../completion/types';
import type { RootStackParamList } from '../navigation/types';
import type { Theme } from '../theme';
import { withAlpha } from '../utils/color';
import { formatDifficultyLabel, getDifficultyColor } from '../utils/format';
import { formatElapsed } from '../utils/formatElapsed';
import { startGame } from '../utils/gameNavigation';

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
  variant: SolvedResultVariant,
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

function pickCelebrationIcon(score: number, accuracy: number, variant: SolvedResultVariant): string {
  const seed = Math.max(0, score) + accuracy + variant.length;
  return CELEBRATION_ICONS[seed % CELEBRATION_ICONS.length];
}

function buildHeroAccents(theme: Theme, difficultyColor: string, scale: number): AccentConfig[] {
  const sized = (value: number) => Math.round(value * scale);

  return [
    { kind: 'ring', top: sized(20), left: sized(18), size: sized(34), color: withAlpha(theme.primaryLight, 0.18), travelX: sized(6), travelY: sized(-8), minOpacity: 0.12, maxOpacity: 0.34, startScale: 0.94, endScale: 1.06 },
    { kind: 'sparkle', top: sized(44), right: sized(24), size: sized(18), color: withAlpha(difficultyColor, 0.84), travelX: sized(-6), travelY: sized(8), minOpacity: 0.2, maxOpacity: 0.74, startScale: 1, endScale: 0.82 },
    { kind: 'sparkle', top: sized(132), left: sized(28), size: sized(16), color: withAlpha(theme.success, 0.8), travelX: sized(6), travelY: sized(-8), minOpacity: 0.22, maxOpacity: 0.86, startScale: 0.92, endScale: 1.12 },
    { kind: 'ring', top: sized(160), right: sized(26), size: sized(32), color: withAlpha(theme.textSecondary, 0.18), travelX: sized(-8), travelY: sized(10), minOpacity: 0.12, maxOpacity: 0.28, startScale: 0.96, endScale: 1.08 },
    { kind: 'ring', top: sized(32), left: sized(50), size: sized(170), color: withAlpha(theme.primaryLight, 0.14), travelX: 0, travelY: 0, minOpacity: 0.12, maxOpacity: 0.32, startScale: 0.94, endScale: 1.08 },
    { kind: 'ring', top: sized(52), left: sized(70), size: sized(132), color: withAlpha(difficultyColor, 0.12), travelX: 0, travelY: 0, minOpacity: 0.08, maxOpacity: 0.26, startScale: 0.96, endScale: 1.06 },
    { kind: 'sparkle', top: sized(24), left: sized(76), size: sized(18), color: withAlpha(theme.primaryLight, 0.92), travelX: sized(6), travelY: sized(-8), minOpacity: 0.22, maxOpacity: 0.88, startScale: 0.9, endScale: 1.12 },
    { kind: 'sparkle', top: sized(28), right: sized(72), size: sized(18), color: withAlpha(difficultyColor, 0.88), travelX: sized(-6), travelY: sized(8), minOpacity: 0.2, maxOpacity: 0.84, startScale: 1, endScale: 0.84 },
  ];
}

export default function CompletionScreen({ route, navigation }: Props) {
  const { strings } = useLanguage();
  const { theme, isDark } = useTheme();
  const { result, variant } = route.params;
  const { difficulty, score, accuracy, elapsedSeconds, streak, gameId } = result;
  const copy = getVariantCopy(variant, strings);
  const icon = pickCelebrationIcon(score, accuracy, variant);
  const difficultyColor = getDifficultyColor(theme, difficulty);
  const difficultyLabel = formatDifficultyLabel(gameId, difficulty);
  const elapsedLabel = formatElapsed(elapsedSeconds);
  const showStreakBadge = streak >= 2;

  const pageOpacity = useRef(new Animated.Value(0)).current;
  const contentTranslateY = useRef(new Animated.Value(24)).current;
  const iconFloat = useRef(new Animated.Value(0)).current;
  const accentProgress = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    const floatingLoop = Animated.loop(Animated.sequence([
      Animated.timing(iconFloat, { toValue: 1, duration: 1100, useNativeDriver: true }),
      Animated.timing(iconFloat, { toValue: 0, duration: 1100, useNativeDriver: true }),
    ]));
    const accentLoop = Animated.loop(Animated.sequence([
      Animated.timing(accentProgress, { toValue: 1, duration: 1800, useNativeDriver: true }),
      Animated.timing(accentProgress, { toValue: 0, duration: 1800, useNativeDriver: true }),
    ]));
    const entranceAnimation = Animated.parallel([
      Animated.timing(pageOpacity, { toValue: 1, duration: 220, useNativeDriver: true }),
      Animated.spring(contentTranslateY, { toValue: 0, friction: 8, tension: 65, useNativeDriver: true }),
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
    startGame(navigation, gameId, difficulty, true);
  }, [difficulty, gameId, navigation]);

  return (
    <GamePageShell
      activeTab="Games"
      headerMode="back"
      backToPuzzleTypeId={gameId}
      headerRight={(
        <View style={baseStyles.headerBadgeRow}>
          <View style={[baseStyles.eyebrow, { borderColor: difficultyColor, backgroundColor: withAlpha(theme.surface, 0.7) }]}>
            <View style={[baseStyles.eyebrowDot, { backgroundColor: difficultyColor }]} />
            <Text style={[baseStyles.eyebrowText, { color: difficultyColor }]}>{copy.eyebrow}</Text>
          </View>
          {showStreakBadge ? (
            <View style={[baseStyles.streakBadge, { borderColor: withAlpha(theme.primaryLight, 0.35), backgroundColor: withAlpha(theme.surface, 0.7) }]}>
              <Text style={baseStyles.streakBadgeIcon}>🔥</Text>
              <Text style={[baseStyles.streakBadgeText, { color: theme.primaryLight }]}>{strings.completion.streakBadge(streak)}</Text>
            </View>
          ) : null}
        </View>
      )}
    >
      <OutcomeScreenLayout contentStyle={baseStyles.content}>
        {(layout) => {
          const s = makeStyles(theme, layout);
          const heroAccents = buildHeroAccents(theme, difficultyColor, layout.accentScale);

          return (
            <Animated.View style={{ opacity: pageOpacity, transform: [{ translateY: contentTranslateY }] }}>
              <View style={s.heroStage}>
                <View style={s.heroCluster} pointerEvents="none">
                  {heroAccents.map((accent, index) => {
                    const translateX = accentProgress.interpolate({ inputRange: [0, 1], outputRange: [0, accent.travelX] });
                    const translateY = accentProgress.interpolate({ inputRange: [0, 1], outputRange: [0, accent.travelY] });
                    const opacity = accentProgress.interpolate({ inputRange: [0, 0.5, 1], outputRange: [accent.minOpacity, accent.maxOpacity, accent.minOpacity] });
                    const scale = accentProgress.interpolate({ inputRange: [0, 0.5, 1], outputRange: [accent.startScale, accent.endScale, accent.startScale] });

                    return (
                      <Animated.View
                        key={`${accent.kind}-${index}`}
                        style={[
                          accent.kind === 'ring' ? baseStyles.ringAccent : baseStyles.sparkleAccent,
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
                        {accent.kind === 'sparkle' ? <Text style={[baseStyles.sparkleAccentText, { color: accent.color }]}>{'✦'}</Text> : null}
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
                  <View style={baseStyles.metaItem}>
                    <Text style={[baseStyles.metaLabel, { color: theme.textSecondary }]}>{strings.completion.elapsedTime}</Text>
                    <Text style={[baseStyles.metaValue, { color: theme.text, fontSize: layout.metaValueFontSize }]}>{elapsedLabel}</Text>
                  </View>
                  <View style={[baseStyles.verticalMetaDivider, { height: layout.metaDividerHeight, backgroundColor: withAlpha(theme.border, 0.8) }]} />
                  <View style={baseStyles.metaItem}>
                    <Text style={[baseStyles.metaLabel, { color: theme.textSecondary }]}>{strings.completion.accuracy}</Text>
                    <Text style={[baseStyles.metaValue, { color: theme.text, fontSize: layout.metaValueFontSize }]}>{`${accuracy}%`}</Text>
                  </View>
                </View>
              </View>

              <View style={s.actionRow}>
                <TouchableOpacity style={[baseStyles.actionButton, { backgroundColor: theme.primary }]} onPress={handlePlayAgain} activeOpacity={0.82}>
                  <Text style={[baseStyles.primaryButtonText, { color: theme.onPrimary }]}>{strings.common.playAgain}</Text>
                </TouchableOpacity>
              </View>
            </Animated.View>
          );
        }}
      </OutcomeScreenLayout>
    </GamePageShell>
  );
}

const baseStyles = StyleSheet.create({
  headerBadgeRow: {
    flexDirection: 'row',
    gap: 8,
    flexWrap: 'wrap',
    justifyContent: 'flex-end',
    maxWidth: '100%',
  },
  content: {
    width: '100%',
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
  metaItem: {
    alignItems: 'center',
  },
  metaLabel: {
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 0.8,
    textTransform: 'uppercase',
  },
  metaValue: {
    marginTop: 4,
    fontWeight: '800',
  },
  verticalMetaDivider: {
    width: 1,
  },
  actionButton: {
    flex: 1,
    paddingVertical: 15,
    borderRadius: 14,
    alignItems: 'center',
  },
  primaryButtonText: {
    fontSize: 16,
    fontWeight: '700',
  },
});

const makeStyles = (theme: Theme, layout: OutcomeScreenMetrics) => StyleSheet.create({
  heroStage: {
    height: layout.heroStageHeight,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
    marginTop: Math.max(8, layout.contentGap - 10),
  },
  heroCluster: {
    width: layout.heroClusterWidth,
    height: layout.heroClusterHeight,
    position: 'relative',
    alignItems: 'center',
  },
  heroIcon: {
    marginTop: layout.heroIconMarginTop,
    width: layout.heroIconSize,
    height: layout.heroIconSize,
    borderRadius: layout.heroIconRadius,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    shadowOpacity: 0.35,
    shadowRadius: 20,
    shadowOffset: { width: 0, height: 14 },
    elevation: 10,
  },
  heroIconText: {
    fontSize: layout.heroSymbolSize,
  },
  copySection: {
    marginTop: layout.copyMarginTop,
  },
  title: {
    color: theme.text,
    textAlign: 'center',
    fontSize: layout.titleFontSize,
    fontWeight: '800',
    lineHeight: layout.titleLineHeight,
  },
  body: {
    marginTop: layout.bodyMarginTop,
    color: theme.textSecondary,
    textAlign: 'center',
    fontSize: layout.bodyFontSize,
    lineHeight: layout.bodyLineHeight,
  },
  difficultyLabel: {
    marginTop: layout.difficultyMarginTop,
    textAlign: 'center',
    fontSize: 13,
    fontWeight: '700',
    letterSpacing: 0.6,
  },
  scoreSection: {
    marginTop: layout.scoreSectionMarginTop,
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
    marginTop: layout.scoreValueMarginTop,
    color: theme.text,
    fontSize: layout.scoreFontSize,
    fontWeight: '900',
    letterSpacing: -2.5,
    lineHeight: layout.scoreLineHeight,
  },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 18,
    flexWrap: 'wrap',
    marginTop: layout.metaRowMarginTop,
  },
  actionRow: {
    flexDirection: 'row',
    gap: 12,
    marginTop: layout.actionMarginTop,
    marginBottom: 4,
  },
});
