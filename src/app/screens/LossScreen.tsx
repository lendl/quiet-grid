import React, { useCallback, useEffect, useMemo, useRef } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Animated } from 'react-native';
import type { StackScreenProps } from '@react-navigation/stack';
import AppScreen from '../components/AppScreen';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { returnToHome } from '../navigation/returnToHome';
import { getPuzzleDefinition } from '../shell/games/gameRegistry';
import { withAlpha } from '../utils/color';
import { formatElapsed, formatDifficultyLabel, getDifficultyColor } from '../utils/format';
import { startGame } from '../utils/gameNavigation';
import type { RootStackParamList } from '../navigation/types';
import type { Theme } from '../theme';

type Props = StackScreenProps<RootStackParamList, 'Loss'>;

function buildLossAccents(theme: Theme, difficultyColor: string) {
  return [
    {
      kind: 'ring' as const,
      top: 20,
      left: 18,
      size: 34,
      color: withAlpha(theme.textSecondary, 0.14),
      travelX: 4,
      travelY: -6,
      minOpacity: 0.1,
      maxOpacity: 0.24,
      startScale: 0.96,
      endScale: 1.04,
    },
    {
      kind: 'ring' as const,
      top: 160,
      right: 26,
      size: 32,
      color: withAlpha(theme.textSecondary, 0.16),
      travelX: -6,
      travelY: 8,
      minOpacity: 0.1,
      maxOpacity: 0.22,
      startScale: 0.96,
      endScale: 1.04,
    },
    {
      kind: 'ring' as const,
      top: 32,
      left: 50,
      size: 170,
      color: withAlpha(difficultyColor, 0.1),
      travelX: 0,
      travelY: 0,
      minOpacity: 0.08,
      maxOpacity: 0.22,
      startScale: 0.96,
      endScale: 1.04,
    },
    {
      kind: 'ring' as const,
      top: 52,
      left: 70,
      size: 132,
      color: withAlpha(theme.border, 0.1),
      travelX: 0,
      travelY: 0,
      minOpacity: 0.06,
      maxOpacity: 0.18,
      startScale: 0.98,
      endScale: 1.02,
    },
  ];
}

export default function LossScreen({ route, navigation }: Props) {
  const { strings } = useLanguage();
  const { theme, isDark } = useTheme();
  const { reason, puzzleTypeId, difficulty, elapsedSeconds } = route.params;
  const definition = getPuzzleDefinition(puzzleTypeId);
  const s = useMemo(() => makeStyles(theme), [theme]);
  const copy = definition.content.loss[reason];
  const difficultyColor = getDifficultyColor(theme, difficulty);
  const difficultyLabel = formatDifficultyLabel(puzzleTypeId, difficulty);
  const elapsedLabel = formatElapsed(elapsedSeconds);
  const lossAccents = useMemo(() => buildLossAccents(theme, difficultyColor), [theme, difficultyColor]);

  const pageOpacity = useRef(new Animated.Value(0)).current;
  const contentTranslateY = useRef(new Animated.Value(20)).current;
  const iconSink = useRef(new Animated.Value(0)).current;
  const accentProgress = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    const sinkLoop = Animated.loop(
      Animated.sequence([
        Animated.timing(iconSink, {
          toValue: 1,
          duration: 1600,
          useNativeDriver: true,
        }),
        Animated.timing(iconSink, {
          toValue: 0,
          duration: 1600,
          useNativeDriver: true,
        }),
      ]),
    );

    const accentLoop = Animated.loop(
      Animated.sequence([
        Animated.timing(accentProgress, {
          toValue: 1,
          duration: 2200,
          useNativeDriver: true,
        }),
        Animated.timing(accentProgress, {
          toValue: 0,
          duration: 2200,
          useNativeDriver: true,
        }),
      ]),
    );

    const entranceAnimation = Animated.parallel([
      Animated.timing(pageOpacity, {
        toValue: 1,
        duration: 240,
        useNativeDriver: true,
      }),
      Animated.spring(contentTranslateY, {
        toValue: 0,
        friction: 9,
        tension: 55,
        useNativeDriver: true,
      }),
    ]);

    entranceAnimation.start();
    sinkLoop.start();
    accentLoop.start();

    return () => {
      entranceAnimation.stop();
      sinkLoop.stop();
      accentLoop.stop();
      pageOpacity.stopAnimation();
      contentTranslateY.stopAnimation();
      iconSink.stopAnimation();
      accentProgress.stopAnimation();
    };
  }, [accentProgress, contentTranslateY, iconSink, pageOpacity]);

  const iconTranslateY = iconSink.interpolate({
    inputRange: [0, 1],
    outputRange: [0, 6],
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
        <View style={s.heroStage}>
          <View style={s.heroCluster} pointerEvents="none">
            {lossAccents.map((accent, index) => {
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
                    s.ringAccent,
                    {
                      top: accent.top,
                      left: accent.left,
                      right: accent.right,
                      width: accent.size,
                      height: accent.size,
                      borderColor: accent.color,
                      opacity,
                      transform: [{ translateX }, { translateY }, { scale }],
                    },
                  ]}
                />
              );
            })}

            <Animated.View
              style={[
                s.heroIcon,
                {
                  backgroundColor: withAlpha(theme.surface, isDark ? 0.2 : 0.16),
                  borderColor: withAlpha(theme.border, 0.4),
                  transform: [{ translateY: iconTranslateY }],
                },
              ]}
            >
              <Text style={s.heroIconText}>{copy.icon}</Text>
            </Animated.View>
          </View>
        </View>

        <View style={s.copySection}>
          <Text style={s.title}>{copy.title}</Text>
          <Text style={s.body}>{copy.body}</Text>
        </View>

        <View style={s.metaSection}>
          <View style={s.metaRow}>
             <Text style={s.metaLabel}>{strings.loss.difficulty}</Text>
            <Text style={s.metaValue}>{difficultyLabel}</Text>
          </View>
          <View style={s.metaDivider} />
          <View style={s.metaRow}>
             <Text style={s.metaLabel}>{strings.loss.elapsedTime}</Text>
            <Text style={s.metaValue}>{elapsedLabel}</Text>
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
    paddingVertical: 24,
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
  heroIcon: {
    marginTop: 78,
    width: 98,
    height: 98,
    borderRadius: 30,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    shadowOpacity: 0.18,
    shadowRadius: 16,
    shadowOffset: { width: 0, height: 10 },
    elevation: 6,
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
  metaSection: {
    marginTop: 20,
    borderTopWidth: 1,
    borderBottomWidth: 1,
    borderColor: withAlpha(theme.border, 0.28),
  },
  metaRow: {
    minHeight: 52,
    paddingVertical: 12,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 16,
  },
  metaLabel: {
    color: theme.textSecondary,
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 0.6,
    textTransform: 'uppercase',
  },
  metaValue: {
    color: theme.text,
    fontSize: 16,
    fontWeight: '700',
  },
  metaDivider: {
    height: 1,
    backgroundColor: withAlpha(theme.border, 0.22),
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
