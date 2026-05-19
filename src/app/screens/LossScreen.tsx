import React, { useCallback, useEffect, useRef } from 'react';
import { Animated, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import type { StackScreenProps } from '@react-navigation/stack';
import { getPuzzleAnalysisAdapter } from '../analysisRegistry';
import OutcomeScreenLayout, { type OutcomeScreenMetrics } from '../components/OutcomeScreenLayout';
import GamePageShell from '../components/GamePageShell';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { getPuzzleDefinition } from '../shell/games/gameRegistry';
import type { RootStackParamList } from '../navigation/types';
import type { Theme } from '../theme';
import { withAlpha } from '../utils/color';
import { formatDifficultyLabel, getDifficultyColor } from '../utils/format';
import { formatElapsed } from '../utils/formatElapsed';
import { startGame } from '../utils/gameNavigation';

type Props = StackScreenProps<RootStackParamList, 'Loss'>;

function buildLossAccents(theme: Theme, difficultyColor: string, scale: number) {
  const sized = (value: number) => Math.round(value * scale);

  return [
    { top: sized(20), left: sized(18), size: sized(34), color: withAlpha(theme.textSecondary, 0.14), travelX: sized(4), travelY: sized(-6), minOpacity: 0.1, maxOpacity: 0.24, startScale: 0.96, endScale: 1.04 },
    { top: sized(160), right: sized(26), size: sized(32), color: withAlpha(theme.textSecondary, 0.16), travelX: sized(-6), travelY: sized(8), minOpacity: 0.1, maxOpacity: 0.22, startScale: 0.96, endScale: 1.04 },
    { top: sized(32), left: sized(50), size: sized(170), color: withAlpha(difficultyColor, 0.1), travelX: 0, travelY: 0, minOpacity: 0.08, maxOpacity: 0.22, startScale: 0.96, endScale: 1.04 },
    { top: sized(52), left: sized(70), size: sized(132), color: withAlpha(theme.border, 0.1), travelX: 0, travelY: 0, minOpacity: 0.06, maxOpacity: 0.18, startScale: 0.98, endScale: 1.02 },
  ];
}

export default function LossScreen({ route, navigation }: Props) {
  const { strings } = useLanguage();
  const { theme, isDark } = useTheme();
  const { reason, puzzleTypeId, difficulty, elapsedSeconds, analysisSource } = route.params;
  const definition = getPuzzleDefinition(puzzleTypeId);
  const analysisAdapter = getPuzzleAnalysisAdapter(puzzleTypeId);
  const copy = definition.content.loss[reason];
  const difficultyColor = getDifficultyColor(theme, difficulty);
  const difficultyLabel = formatDifficultyLabel(puzzleTypeId, difficulty);
  const elapsedLabel = formatElapsed(elapsedSeconds);
  const canAnalyze = analysisAdapter?.supportsAnalysis(analysisSource) ?? false;

  const pageOpacity = useRef(new Animated.Value(0)).current;
  const contentTranslateY = useRef(new Animated.Value(20)).current;
  const iconSink = useRef(new Animated.Value(0)).current;
  const accentProgress = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    const sinkLoop = Animated.loop(Animated.sequence([
      Animated.timing(iconSink, { toValue: 1, duration: 1600, useNativeDriver: true }),
      Animated.timing(iconSink, { toValue: 0, duration: 1600, useNativeDriver: true }),
    ]));
    const accentLoop = Animated.loop(Animated.sequence([
      Animated.timing(accentProgress, { toValue: 1, duration: 2200, useNativeDriver: true }),
      Animated.timing(accentProgress, { toValue: 0, duration: 2200, useNativeDriver: true }),
    ]));
    const entranceAnimation = Animated.parallel([
      Animated.timing(pageOpacity, { toValue: 1, duration: 240, useNativeDriver: true }),
      Animated.spring(contentTranslateY, { toValue: 0, friction: 9, tension: 55, useNativeDriver: true }),
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

  const handleAnalyze = useCallback(() => {
    if (!analysisAdapter || !analysisSource) {
      return;
    }

    navigation.navigate('Analysis', {
      analysis: analysisAdapter.buildAnalysis(analysisSource),
    });
  }, [analysisAdapter, analysisSource, navigation]);

  return (
    <GamePageShell activeTab="Games" headerMode="back" backToPuzzleTypeId={puzzleTypeId}>
      <OutcomeScreenLayout>
        {(layout) => {
          const s = makeStyles(theme, layout);
          const accents = buildLossAccents(theme, difficultyColor, layout.accentScale);

          return (
            <Animated.View style={{ opacity: pageOpacity, transform: [{ translateY: contentTranslateY }] }}>
              <View style={s.heroStage}>
                <View style={s.heroCluster} pointerEvents="none">
                  {accents.map((accent, index) => {
                    const translateX = accentProgress.interpolate({ inputRange: [0, 1], outputRange: [0, accent.travelX] });
                    const translateY = accentProgress.interpolate({ inputRange: [0, 1], outputRange: [0, accent.travelY] });
                    const opacity = accentProgress.interpolate({ inputRange: [0, 0.5, 1], outputRange: [accent.minOpacity, accent.maxOpacity, accent.minOpacity] });
                    const scale = accentProgress.interpolate({ inputRange: [0, 0.5, 1], outputRange: [accent.startScale, accent.endScale, accent.startScale] });

                    return (
                      <Animated.View
                        key={`ring-${index}`}
                        style={[
                          baseStyles.ringAccent,
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
                  <Text style={[baseStyles.metaLabel, { color: theme.textSecondary }]}>{strings.loss.difficulty}</Text>
                  <Text style={[baseStyles.metaValue, { color: theme.text, fontSize: layout.metaValueFontSize, fontWeight: '700' }]}>{difficultyLabel}</Text>
                </View>
                <View style={[baseStyles.horizontalDivider, { backgroundColor: withAlpha(theme.border, 0.22) }]} />
                <View style={s.metaRow}>
                  <Text style={[baseStyles.metaLabel, { color: theme.textSecondary }]}>{strings.loss.elapsedTime}</Text>
                  <Text style={[baseStyles.metaValue, { color: theme.text, fontSize: layout.metaValueFontSize, fontWeight: '700' }]}>{elapsedLabel}</Text>
                </View>
              </View>

              <View style={s.primaryActionRow}>
                <TouchableOpacity style={[baseStyles.actionButton, baseStyles.fullWidthAction, { backgroundColor: theme.primary }]} onPress={handlePlayAgain} activeOpacity={0.82}>
                  <Text style={[baseStyles.primaryButtonText, { color: theme.onPrimary }]}>{strings.common.playAgain}</Text>
                </TouchableOpacity>
              </View>

              <View style={s.actionRow}>
                {canAnalyze ? (
                  <TouchableOpacity
                    style={[
                      baseStyles.actionButton,
                      {
                        backgroundColor: theme.surfaceElevated,
                        borderColor: theme.border,
                        borderWidth: 1,
                      },
                    ]}
                    onPress={handleAnalyze}
                    activeOpacity={0.82}
                  >
                    <Text style={[baseStyles.secondaryButtonText, { color: theme.text }]}>{strings.analysis.analyze}</Text>
                  </TouchableOpacity>
                ) : null}
              </View>
            </Animated.View>
          );
        }}
      </OutcomeScreenLayout>
    </GamePageShell>
  );
}

const baseStyles = StyleSheet.create({
  ringAccent: {
    position: 'absolute',
    borderRadius: 999,
    borderWidth: 1,
  },
  metaLabel: {
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 0.6,
    textTransform: 'uppercase',
  },
  metaValue: {
    marginTop: 4,
  },
  horizontalDivider: {
    height: 1,
  },
  actionButton: {
    flex: 1,
    paddingVertical: 15,
    borderRadius: 14,
    alignItems: 'center',
  },
  fullWidthAction: {
    flex: 1,
  },
  primaryButtonText: {
    fontSize: 16,
    fontWeight: '700',
  },
  secondaryButtonText: {
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
    shadowOpacity: 0.18,
    shadowRadius: 16,
    shadowOffset: { width: 0, height: 10 },
    elevation: 6,
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
  metaSection: {
    marginTop: layout.scoreSectionMarginTop,
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
  primaryActionRow: {
    flexDirection: 'row',
    marginTop: layout.primaryActionMarginTop,
  },
  actionRow: {
    flexDirection: 'row',
    gap: 12,
    marginTop: Math.max(12, layout.actionMarginTop - 8),
    marginBottom: 4,
  },
});
