import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Animated,
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Gesture, GestureDetector } from 'react-native-gesture-handler';
import ActivePuzzleReplaceDialog from './ActivePuzzleReplaceDialog';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { getGameDefinition } from '../shell/games/gameRegistry';
import type { GameId } from '../../games/shared/types';
import type { Difficulty } from '../types';
import type { Theme } from '../theme';
import { withAlpha } from '../utils/color';
import { getDifficultyColor } from '../utils/format';
import { shouldAutoShowTutorial } from '../utils/settingsStorage';

const SHEET_ANIMATION_DURATION = 280;
const DISMISS_SWIPE_THRESHOLD = 80;

interface Props {
  gameId: GameId | null;
  onDismiss: () => void;
  onSelectDifficulty: (gameId: GameId, difficulty: Difficulty) => void;
  onRules: (gameId: GameId) => void;
  onTutorial: (gameId: GameId) => void;
  dialogVisible: boolean;
  onDialogContinue: () => void;
  onDialogStartNew: () => Promise<void> | void;
  onDialogDismiss: () => void;
}

export default function GameSelectSheet({
  gameId,
  onDismiss,
  onSelectDifficulty,
  onRules,
  onTutorial,
  dialogVisible,
  onDialogContinue,
  onDialogStartNew,
  onDialogDismiss,
}: Props) {
  const { theme, isDark } = useTheme();
  const { strings } = useLanguage();
  const insets = useSafeAreaInsets();
  const s = useMemo(() => makeStyles(theme, isDark, insets.bottom), [theme, isDark, insets.bottom]);

  const backdropOpacity = useRef(new Animated.Value(0)).current;
  const sheetTranslateY = useRef(new Animated.Value(600)).current;
  const [showTutorialHint, setShowTutorialHint] = useState(false);
  const [isDismissing, setIsDismissing] = useState(false);

  const definition = useMemo(
    () => (gameId ? getGameDefinition(gameId) : null),
    [gameId],
  );

  useEffect(() => {
    if (!gameId) return;

    setIsDismissing(false);
    setShowTutorialHint(false);

    void shouldAutoShowTutorial(gameId).then(setShowTutorialHint);

    Animated.parallel([
      Animated.timing(backdropOpacity, {
        toValue: 1,
        duration: SHEET_ANIMATION_DURATION,
        useNativeDriver: true,
      }),
      Animated.spring(sheetTranslateY, {
        toValue: 0,
        friction: 9,
        tension: 65,
        useNativeDriver: true,
      }),
    ]).start();
  }, [backdropOpacity, gameId, sheetTranslateY]);

  const animateOut = useCallback((onDone: () => void) => {
    setIsDismissing(true);
    Animated.parallel([
      Animated.timing(backdropOpacity, {
        toValue: 0,
        duration: 200,
        useNativeDriver: true,
      }),
      Animated.timing(sheetTranslateY, {
        toValue: 600,
        duration: 220,
        useNativeDriver: true,
      }),
    ]).start(({ finished }) => {
      if (finished) {
        backdropOpacity.setValue(0);
        sheetTranslateY.setValue(600);
        onDone();
      }
    });
  }, [backdropOpacity, sheetTranslateY]);

  const handleDismiss = useCallback(() => {
    if (isDismissing) return;
    animateOut(onDismiss);
  }, [animateOut, isDismissing, onDismiss]);

  const handleSelectDifficulty = useCallback((difficulty: Difficulty) => {
    if (!gameId || isDismissing) return;
    animateOut(() => {
      onDismiss();
      onSelectDifficulty(gameId, difficulty);
    });
  }, [animateOut, gameId, isDismissing, onDismiss, onSelectDifficulty]);

  const handleRules = useCallback(() => {
    if (!gameId || isDismissing) return;
    animateOut(() => {
      onDismiss();
      onRules(gameId);
    });
  }, [animateOut, gameId, isDismissing, onDismiss, onRules]);

  const handleTutorial = useCallback(() => {
    if (!gameId || isDismissing) return;
    animateOut(() => {
      onDismiss();
      onTutorial(gameId);
    });
  }, [animateOut, gameId, isDismissing, onDismiss, onTutorial]);

  const swipeGesture = useMemo(
    () =>
      Gesture.Pan()
        .runOnJS(true)
        .activeOffsetY([8, 8])
        .failOffsetX([-20, 20])
        .onEnd((event) => {
          if (event.translationY > DISMISS_SWIPE_THRESHOLD || event.velocityY > 600) {
            handleDismiss();
          }
        }),
    [handleDismiss],
  );

  if (!gameId || !definition) return null;

  const levels = definition.difficulties.map((key) => ({
    key,
    label: definition.content.difficultyLabels[key],
    description: definition.content.difficultyDescriptions[key],
  }));

  return (
    <Modal
      transparent
      visible
      animationType="none"
      onRequestClose={handleDismiss}
      statusBarTranslucent
    >
      <Animated.View style={[s.backdrop, { opacity: backdropOpacity }]}>
        <Pressable style={StyleSheet.absoluteFill} onPress={handleDismiss} />
      </Animated.View>

      <Animated.View
        style={[s.sheetContainer, { transform: [{ translateY: sheetTranslateY }] }]}
        pointerEvents="box-none"
      >
        <GestureDetector gesture={swipeGesture}>
          <View style={s.sheet}>
            <View style={s.handle} />

            <View style={s.header}>
              <Text style={s.emoji}>{definition.emoji}</Text>
              <View style={s.titleBlock}>
                <Text style={s.title}>{definition.title}</Text>
                <Text style={s.tagline}>{definition.tagline}</Text>
              </View>
            </View>

            {showTutorialHint && definition.supports.tutorial ? (
              <TouchableOpacity style={s.tutorialHint} onPress={handleTutorial} activeOpacity={0.82}>
                <Text style={s.tutorialHintText}>{strings.gameSheet.newToGame(definition.shortTitle)}</Text>
                <Text style={s.tutorialHintAction}>{strings.common.tutorial} →</Text>
              </TouchableOpacity>
            ) : null}

            <Text style={s.sectionLabel}>{strings.puzzle.chooseDifficulty}</Text>

            <ScrollView
              style={s.levelsScroll}
              contentContainerStyle={s.levelsContent}
              showsVerticalScrollIndicator={false}
              bounces={false}
            >
              {levels.map((level, index) => {
                const color = getDifficultyColor(theme, level.key);
                return (
                  <React.Fragment key={level.key}>
                    <TouchableOpacity
                      style={s.levelRow}
                      onPress={() => handleSelectDifficulty(level.key)}
                      activeOpacity={0.78}
                    >
                      <View style={[s.levelMarker, { backgroundColor: color }]} />
                      <View style={s.levelBody}>
                        <Text style={s.levelLabel}>{level.label}</Text>
                        <Text style={s.levelDesc}>{level.description}</Text>
                      </View>
                      <Text style={[s.levelPlay, { color: theme.primaryLight }]}>{strings.common.play}</Text>
                    </TouchableOpacity>
                    {index < levels.length - 1 ? <View style={s.divider} /> : null}
                  </React.Fragment>
                );
              })}
            </ScrollView>

            <View style={s.footer}>
              <TouchableOpacity style={s.footerLink} onPress={handleRules} activeOpacity={0.75}>
                <Text style={s.footerLinkText}>{strings.common.rules}</Text>
              </TouchableOpacity>
              {definition.supports.tutorial ? (
                <>
                  <View style={s.footerDot} />
                  <TouchableOpacity style={s.footerLink} onPress={handleTutorial} activeOpacity={0.75}>
                    <Text style={s.footerLinkText}>{strings.common.tutorial}</Text>
                  </TouchableOpacity>
                </>
              ) : null}
            </View>
          </View>
        </GestureDetector>
      </Animated.View>

      <ActivePuzzleReplaceDialog
        visible={dialogVisible}
        onContinue={onDialogContinue}
        onStartNew={onDialogStartNew}
        onDismiss={onDialogDismiss}
      />
    </Modal>
  );
}

const makeStyles = (theme: Theme, isDark: boolean, bottomInset: number) =>
  StyleSheet.create({
    backdrop: {
      ...StyleSheet.absoluteFillObject,
      backgroundColor: withAlpha(theme.background, isDark ? 0.78 : 0.52),
    },
    sheetContainer: {
      position: 'absolute',
      left: 0,
      right: 0,
      bottom: 0,
    },
    sheet: {
      backgroundColor: theme.surfaceElevated,
      borderTopLeftRadius: 24,
      borderTopRightRadius: 24,
      borderWidth: 1,
      borderBottomWidth: 0,
      borderColor: withAlpha(theme.border, isDark ? 0.7 : 0.5),
      paddingBottom: Math.max(bottomInset, 16),
      shadowColor: '#000',
      shadowOpacity: isDark ? 0.4 : 0.14,
      shadowRadius: 24,
      shadowOffset: { width: 0, height: -6 },
      elevation: 16,
    },
    handle: {
      alignSelf: 'center',
      width: 36,
      height: 4,
      borderRadius: 999,
      backgroundColor: withAlpha(theme.border, 0.8),
      marginTop: 12,
      marginBottom: 4,
    },
    header: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 14,
      paddingHorizontal: 20,
      paddingTop: 12,
      paddingBottom: 4,
    },
    emoji: {
      fontSize: 36,
      lineHeight: 44,
    },
    titleBlock: {
      flex: 1,
    },
    title: {
      fontSize: 22,
      fontWeight: '800',
      color: theme.text,
    },
    tagline: {
      marginTop: 2,
      fontSize: 13,
      lineHeight: 18,
      color: theme.textSecondary,
    },
    tutorialHint: {
      marginHorizontal: 20,
      marginTop: 12,
      padding: 14,
      borderRadius: 14,
      backgroundColor: withAlpha(theme.primary, isDark ? 0.14 : 0.08),
      borderWidth: 1,
      borderColor: withAlpha(theme.primaryLight, isDark ? 0.28 : 0.22),
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: 12,
    },
    tutorialHintText: {
      flex: 1,
      fontSize: 13,
      lineHeight: 18,
      color: theme.text,
    },
    tutorialHintAction: {
      fontSize: 13,
      fontWeight: '700',
      color: theme.primaryLight,
    },
    sectionLabel: {
      marginHorizontal: 20,
      marginTop: 18,
      marginBottom: 4,
      fontSize: 11,
      fontWeight: '800',
      letterSpacing: 0.8,
      textTransform: 'uppercase',
      color: theme.textMuted,
    },
    levelsScroll: {
      maxHeight: 280,
    },
    levelsContent: {
      paddingHorizontal: 20,
    },
    levelRow: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 14,
      paddingVertical: 16,
    },
    levelMarker: {
      width: 8,
      height: 40,
      borderRadius: 999,
    },
    levelBody: {
      flex: 1,
    },
    levelLabel: {
      fontSize: 17,
      fontWeight: '800',
      color: theme.text,
    },
    levelDesc: {
      marginTop: 2,
      fontSize: 12,
      lineHeight: 17,
      color: theme.textSecondary,
    },
    levelPlay: {
      fontSize: 13,
      fontWeight: '700',
    },
    divider: {
      height: 1,
      backgroundColor: withAlpha(theme.border, 0.3),
    },
    footer: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 10,
      paddingHorizontal: 20,
      paddingTop: 16,
    },
    footerLink: {
      minHeight: 36,
      alignItems: 'center',
      justifyContent: 'center',
      paddingHorizontal: 4,
    },
    footerLinkText: {
      fontSize: 13,
      fontWeight: '700',
      color: theme.textSecondary,
    },
    footerDot: {
      width: 3,
      height: 3,
      borderRadius: 999,
      backgroundColor: withAlpha(theme.textSecondary, 0.5),
    },
  });
