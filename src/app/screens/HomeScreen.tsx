import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Linking, Animated } from 'react-native';
import { Octicons } from '@expo/vector-icons';
import { useFocusEffect } from '@react-navigation/native';
import type { StackScreenProps } from '@react-navigation/stack';
import AppDialog from '../components/AppDialog';
import ActivePuzzleReplaceDialog from '../components/ActivePuzzleReplaceDialog';
import AppScreen from '../components/AppScreen';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { useActivePuzzleReplacement } from '../hooks/useActivePuzzleReplacement';
import { withAlpha } from '../utils/color';
import { getActivePuzzleDisplay } from '../utils/activePuzzle';
import type { AppDialogProps } from '../components/AppDialog';
import type { RootStackParamList } from '../navigation/types';
import type { Theme } from '../theme';

type Props = StackScreenProps<RootStackParamList, 'Home'>;
const REPO_URL = 'https://github.com/lendl/quiet-grid';

export default function HomeScreen({ navigation }: Props) {
  const { strings } = useLanguage();
  const { theme, isDark } = useTheme();
  const [repoDialogVisible, setRepoDialogVisible] = useState(false);
  const {
    activePuzzle,
    dialogVisible: replaceDialogVisible,
    syncActivePuzzle,
    requestStart,
    handleContinue,
    handleGiveUpAndStartNew,
    dismissDialog,
  } = useActivePuzzleReplacement({ navigation });
  const s = useMemo(() => makeStyles(theme, isDark), [theme, isDark]);
  const heroTiles = useMemo(
    () => [theme.primary, theme.primaryLight, theme.text, theme.primaryLight],
    [theme.primary, theme.primaryLight, theme.text],
  );
  const activeCardProgress = useRef(new Animated.Value(0)).current;
  const activePuzzleDisplay = useMemo(
    () => (activePuzzle ? getActivePuzzleDisplay(activePuzzle) : null),
    [activePuzzle],
  );

  useFocusEffect(useCallback(() => {
    const state = navigation.getState();
    const currentRoute = state.routes[state.index];

    if (currentRoute?.name === 'Home' && state.index > 0) {
      navigation.reset({
        index: 0,
        routes: [{ name: 'Home' }],
      });
    }

    void syncActivePuzzle();

    return undefined;
  }, [navigation, syncActivePuzzle]));

  const handleNewGame = useCallback(() => {
    requestStart(() => {
      navigation.navigate('PuzzleTypePicker');
    });
  }, [navigation, requestStart]);

  const handleOpenRepo = useCallback(async () => {
    const supported = await Linking.canOpenURL(REPO_URL);

    if (!supported) {
      setRepoDialogVisible(true);
      return;
    }

    const opened = await Linking.openURL(REPO_URL).then(
      () => true,
      () => false,
    );

    if (!opened) {
      setRepoDialogVisible(true);
    }
  }, []);

  const repoDialogButtons = useMemo<AppDialogProps['buttons']>(() => [
    {
      text: 'OK',
      onPress: () => setRepoDialogVisible(false),
    },
  ], []);

  useEffect(() => {
    if (!activePuzzle) {
      activeCardProgress.stopAnimation();
      activeCardProgress.setValue(0);
      return;
    }

    const animation = Animated.loop(
      Animated.sequence([
        Animated.timing(activeCardProgress, {
          toValue: 1,
          duration: 1700,
          useNativeDriver: true,
        }),
        Animated.timing(activeCardProgress, {
          toValue: 0,
          duration: 1700,
          useNativeDriver: true,
        }),
      ]),
    );

    animation.start();

    return () => {
      animation.stop();
      activeCardProgress.stopAnimation();
    };
  }, [activeCardProgress, activePuzzle]);

  const activeCardLift = useMemo(() => activeCardProgress.interpolate({
    inputRange: [0, 1],
    outputRange: [0, -4],
  }), [activeCardProgress]);

  return (
    <AppScreen contentStyle={s.container}>
      <View style={s.topActions}>
        <TouchableOpacity
          onPress={() => {
            void handleOpenRepo();
          }}
          style={s.iconBtn}
           accessibilityLabel={strings.home.openRepo}
          accessibilityRole="link"
          activeOpacity={0.8}
        >
          <Octicons name="mark-github" size={24} color={theme.text} />
        </TouchableOpacity>
      </View>

      <View style={s.hero}>
        <Text style={s.wordmarkAccent}>Quiet</Text>
        <Text style={s.title}>Grid</Text>
        <View style={s.wordmarkTiles} accessibilityLabel={strings.home.brandMark}>
          {heroTiles.map((color, index) => (
            <View
              key={`${color}-${index}`}
              style={[
                s.wordmarkTile,
                { backgroundColor: color, opacity: index === 1 || index === 3 ? 0.45 : 1 },
              ]}
            />
          ))}
        </View>
        <Text style={s.subtitle}>{strings.home.subtitle}</Text>
      </View>

      <View style={s.menu}>
        {activePuzzle ? (
          <Animated.View style={[s.activeGameCard, { transform: [{ translateY: activeCardLift }] }]}>
            <Text style={s.activeGameEyebrow}>{activePuzzleDisplay?.label}</Text>
            <Text style={s.activeGameTitle}>{strings.home.activePuzzleWaiting}</Text>
            <View style={s.activeGameMetaRow}>
              {activePuzzleDisplay?.meta.map((meta, i) => (
                <View key={i} style={s.activeGameMetaPill}>
                  <Text style={s.activeGameMetaText}>{meta}</Text>
                </View>
              ))}
            </View>
            <TouchableOpacity style={s.activeGameAction} onPress={handleContinue} activeOpacity={0.85}>
              <Text style={s.activeGameActionText}>{`▶ ${strings.common.continuePuzzle}`}</Text>
            </TouchableOpacity>
          </Animated.View>
        ) : null}
          <TouchableOpacity
          style={[s.btn, activePuzzle ? s.btnSecondary : s.btnPrimary]}
          onPress={handleNewGame}
          activeOpacity={0.8}
        >
          <Text style={[s.btnText, activePuzzle ? s.btnTextSecondary : s.btnTextPrimary]}>{strings.home.startPuzzle}</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[s.btn, s.btnSecondary]}
          onPress={() => navigation.navigate('Config')}
          activeOpacity={0.8}
        >
          <Text style={[s.btnText, s.btnTextSecondary]}>{strings.common.config}</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[s.btn, s.btnSecondary]}
          onPress={() => navigation.navigate('Support')}
          activeOpacity={0.8}
        >
          <Text style={[s.btnText, s.btnTextSecondary]}>{strings.common.support}</Text>
        </TouchableOpacity>
      </View>

      <View style={s.trustStrip}>
        <Text style={s.trustLine}>{strings.home.trustOffline}</Text>
        <Text style={s.trustLine}>{strings.home.trustPrivacy}</Text>
      </View>

      <ActivePuzzleReplaceDialog
        visible={replaceDialogVisible}
        onContinue={handleContinue}
        onStartNew={handleGiveUpAndStartNew}
        onDismiss={dismissDialog}
      />
      <AppDialog
        visible={repoDialogVisible}
        title={strings.home.repoErrorTitle}
        message={strings.home.repoErrorMessage}
        buttons={repoDialogButtons}
        onDismiss={() => setRepoDialogVisible(false)}
      />
    </AppScreen>
  );
}

const makeStyles = (theme: Theme, isDark: boolean) => StyleSheet.create({
  container: { flex: 1, backgroundColor: theme.background },
  topActions: {
    flexDirection: 'row',
    justifyContent: 'flex-start',
    alignItems: 'center',
    paddingHorizontal: 14,
    paddingTop: 14,
  },
  iconBtn: {
    minWidth: 44,
    minHeight: 36,
    alignItems: 'center',
    justifyContent: 'center',
  },
  hero: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  wordmarkAccent: {
    fontSize: 14,
    fontWeight: '700',
    letterSpacing: 3,
    textTransform: 'uppercase',
    color: theme.primary,
    marginBottom: 2,
  },
  title: {
    fontSize: 42,
    fontWeight: '900',
    letterSpacing: -1,
    color: theme.text,
  },
  wordmarkTiles: {
    flexDirection: 'row',
    gap: 8,
    marginTop: 16,
    marginBottom: 16,
  },
  wordmarkTile: {
    width: 22,
    height: 22,
    borderRadius: 6,
  },
  subtitle: { fontSize: 15, color: theme.textSecondary },
  menu: { paddingHorizontal: 24, paddingTop: 8, paddingBottom: 24, gap: 12 },
  btn: { paddingVertical: 15, borderRadius: 14, alignItems: 'center' },
  btnPrimary: { backgroundColor: theme.primary },
  btnSecondary: { backgroundColor: theme.surfaceElevated, borderWidth: 1, borderColor: theme.border },
  activeGameCard: {
    borderRadius: 22,
    padding: 18,
    borderWidth: 1,
    borderColor: withAlpha(theme.primaryLight, isDark ? 0.34 : 0.26),
    backgroundColor: withAlpha(theme.surfaceElevated, isDark ? 0.96 : 0.92),
    gap: 10,
  },
  activeGameEyebrow: {
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 1.2,
    textTransform: 'uppercase',
    color: theme.primaryLight,
  },
  activeGameTitle: {
    fontSize: 22,
    fontWeight: '800',
    color: theme.text,
  },
  activeGameMetaRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  activeGameMetaPill: {
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 6,
    backgroundColor: withAlpha(theme.background, isDark ? 0.38 : 0.58),
  },
  activeGameMetaText: {
    fontSize: 12,
    fontWeight: '700',
    color: theme.textSecondary,
  },
  activeGameAction: {
    marginTop: 4,
    minHeight: 50,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: theme.primary,
  },
  activeGameActionText: {
    fontSize: 16,
    fontWeight: '800',
    color: theme.text,
  },
  btnText: { fontSize: 16, fontWeight: '700' },
  btnTextPrimary: { color: theme.text },
  btnTextSecondary: { color: theme.text },
  trustStrip: {
    alignItems: 'center',
    paddingHorizontal: 24,
    paddingBottom: 20,
    gap: 2,
  },
  trustLine: {
    textAlign: 'center',
    fontSize: 12,
    color: theme.textMuted,
  },
});
