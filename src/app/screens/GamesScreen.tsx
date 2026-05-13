import React, { useCallback, useMemo } from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useFocusEffect, useNavigation, type NavigationProp } from '@react-navigation/native';
import type { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import ActivePuzzleReplaceDialog from '../components/ActivePuzzleReplaceDialog';
import AppScreen from '../components/AppScreen';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { useActivePuzzleReplacement } from '../hooks/useActivePuzzleReplacement';
import type { RootStackParamList, MainTabParamList } from '../navigation/types';
import { puzzleRegistry } from '../shell/games/gameRegistry';
import type { Theme } from '../theme';
import { getActivePuzzleDisplay } from '../utils/activePuzzle';
import { withAlpha } from '../utils/color';
import { shouldAutoShowTutorial } from '../utils/settingsStorage';

type Props = BottomTabScreenProps<MainTabParamList, 'Games'>;

export default function GamesScreen(_: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const navigation = useNavigation<NavigationProp<RootStackParamList>>();
  const s = useMemo(() => makeStyles(theme), [theme]);
  const {
    activePuzzle,
    dialogVisible,
    syncActivePuzzle,
    requestStart,
    handleContinue,
    handleGiveUpAndStartNew,
    dismissDialog,
  } = useActivePuzzleReplacement({ navigation });
  const activePuzzleDisplay = useMemo(
    () => (activePuzzle ? getActivePuzzleDisplay(activePuzzle) : null),
    [activePuzzle],
  );

  useFocusEffect(useCallback(() => {
    void syncActivePuzzle();
    return undefined;
  }, [syncActivePuzzle]));

  const handleSelectPuzzleType = useCallback((puzzleTypeId: typeof puzzleRegistry[number]['id']) => {
    requestStart(() => {
      void (async () => {
        const puzzleType = puzzleRegistry.find((definition) => definition.id === puzzleTypeId);

        if (puzzleType?.supports.tutorial && await shouldAutoShowTutorial(puzzleTypeId)) {
          navigation.navigate('Tutorial', { puzzleTypeId, entry: 'startup' });
          return;
        }

        navigation.navigate('Puzzle', { puzzleTypeId });
      })();
    });
  }, [navigation, requestStart]);

  return (
    <AppScreen contentStyle={s.container}>
      <ScrollView contentContainerStyle={s.scroll}>
        <View style={s.header}>
          <Text style={s.title}>{strings.tabs.games}</Text>
          <Text style={s.subtitle}>{strings.games.subtitle}</Text>
        </View>

        {activePuzzleDisplay ? (
          <View style={s.activeGameCard}>
            <Text style={s.activeGameEyebrow}>{activePuzzleDisplay.label}</Text>
            <Text style={s.activeGameTitle}>{strings.home.activePuzzleWaiting}</Text>
            <View style={s.activeGameMetaRow}>
              {activePuzzleDisplay.meta.map((meta) => (
                <View key={meta} style={s.activeGameMetaPill}>
                  <Text style={s.activeGameMetaText}>{meta}</Text>
                </View>
              ))}
            </View>
            <TouchableOpacity style={s.activeGameAction} onPress={handleContinue} activeOpacity={0.85}>
              <Text style={s.activeGameActionText}>{`▶ ${strings.common.continuePuzzle}`}</Text>
            </TouchableOpacity>
          </View>
        ) : null}

        <View style={s.gameList}>
          {puzzleRegistry.map((puzzleType) => (
            <TouchableOpacity
              key={puzzleType.id}
              style={s.card}
              onPress={() => handleSelectPuzzleType(puzzleType.id)}
              activeOpacity={0.78}
            >
              <Text style={s.cardEmoji}>{puzzleType.emoji}</Text>
              <View style={s.cardBody}>
                <Text style={s.cardTitle}>{puzzleType.title}</Text>
                <Text style={s.cardTagline}>{puzzleType.tagline}</Text>
              </View>
            </TouchableOpacity>
          ))}
        </View>
      </ScrollView>

      <ActivePuzzleReplaceDialog
        visible={dialogVisible}
        onContinue={handleContinue}
        onStartNew={handleGiveUpAndStartNew}
        onDismiss={dismissDialog}
      />
    </AppScreen>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.background,
  },
  scroll: {
    padding: 20,
    gap: 20,
  },
  header: {
    gap: 8,
  },
  title: {
    fontSize: 30,
    fontWeight: '900',
    color: theme.text,
  },
  subtitle: {
    fontSize: 15,
    lineHeight: 22,
    color: theme.textSecondary,
  },
  activeGameCard: {
    borderRadius: 22,
    padding: 18,
    borderWidth: 1,
    borderColor: withAlpha(theme.primaryLight, 0.28),
    backgroundColor: withAlpha(theme.surfaceElevated, 0.94),
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
    backgroundColor: withAlpha(theme.background, 0.5),
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
    color: theme.onPrimary,
  },
  gameList: {
    gap: 12,
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
    padding: 20,
    backgroundColor: theme.surface,
    borderRadius: 16,
  },
  cardEmoji: {
    fontSize: 32,
  },
  cardBody: {
    flex: 1,
  },
  cardTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: theme.text,
  },
  cardTagline: {
    marginTop: 4,
    fontSize: 13,
    color: theme.textSecondary,
    lineHeight: 19,
  },
});
