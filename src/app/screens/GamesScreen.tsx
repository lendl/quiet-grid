import React, { useCallback, useMemo, useState } from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useFocusEffect, useNavigation, type NavigationProp } from '@react-navigation/native';
import type { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import GameSelectSheet from '../components/GameSelectSheet';
import GlobalPageShell from '../components/GlobalPageShell';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { useActivePuzzleReplacement } from '../hooks/useActivePuzzleReplacement';
import type { RootStackParamList, MainTabParamList } from '../navigation/types';
import { gameRegistry } from '../shell/games/gameRegistry';
import type { GameId } from '../../games/shared/types';
import type { Difficulty } from '../types';
import type { Theme } from '../theme';
import { getActivePuzzleDisplay } from '../utils/activePuzzle';
import { withAlpha } from '../utils/color';
import { loadBetaGamesEnabled } from '../utils/settingsStorage';
import { startGame } from '../utils/gameNavigation';

type Props = BottomTabScreenProps<MainTabParamList, 'Games'>;

export default function GamesScreen(_: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const navigation = useNavigation<NavigationProp<RootStackParamList>>();
  const s = useMemo(() => makeStyles(theme), [theme]);
  const [betaGamesEnabled, setBetaGamesEnabled] = React.useState(false);
  const [selectedGameId, setSelectedGameId] = useState<GameId | null>(null);
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
    void loadBetaGamesEnabled().then(setBetaGamesEnabled);
    return undefined;
  }, [syncActivePuzzle]));

  const handleOpenSheet = useCallback((gameId: GameId) => {
    setSelectedGameId(gameId);
  }, []);

  const handleSheetDismiss = useCallback(() => {
    setSelectedGameId(null);
  }, []);

  const handleSheetSelectDifficulty = useCallback((gameId: GameId, difficulty: Difficulty) => {
    requestStart(() => {
      startGame(navigation, gameId, difficulty);
    });
  }, [navigation, requestStart]);

  const handleSheetRules = useCallback((gameId: GameId) => {
    navigation.navigate('Game', { gameId, initialTab: 'Rules' });
  }, [navigation]);

  const handleSheetTutorial = useCallback((gameId: GameId) => {
    navigation.navigate('Game', { gameId, initialTab: 'Tutorial' });
  }, [navigation]);

  const readyGames = useMemo(() => gameRegistry.filter((g) => !g.beta), []);
  const betaGames = useMemo(() => gameRegistry.filter((g) => g.beta), []);

  return (
    <GlobalPageShell activeTab="Games">
      <ScrollView contentContainerStyle={s.scroll}>
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
          {readyGames.map((game) => (
            <TouchableOpacity
              key={game.id}
              style={s.card}
              onPress={() => handleOpenSheet(game.id)}
              activeOpacity={0.78}
            >
              <Text style={s.cardEmoji}>{game.emoji}</Text>
              <View style={s.cardBody}>
                <Text style={s.cardTitle}>{game.title}</Text>
                <Text style={s.cardTagline}>{game.tagline}</Text>
              </View>
            </TouchableOpacity>
          ))}
        </View>

        <View style={s.comingSoonSection}>
          <Text style={s.sectionLabel}>{strings.games.comingSoon}</Text>
          {betaGamesEnabled ? (
            <Text style={s.betaDisclaimer}>{strings.games.betaDisclaimer}</Text>
          ) : null}
          {betaGames.map((game) => (
            <TouchableOpacity
              key={game.id}
              style={[s.card, betaGamesEnabled ? s.cardBeta : s.cardDisabled]}
              onPress={betaGamesEnabled ? () => handleOpenSheet(game.id) : undefined}
              activeOpacity={betaGamesEnabled ? 0.78 : 1}
            >
              <Text style={[s.cardEmoji, !betaGamesEnabled && s.cardEmojiDisabled]}>{game.emoji}</Text>
              <View style={s.cardBody}>
                <Text style={[s.cardTitle, !betaGamesEnabled && s.cardTitleDisabled]}>{game.title}</Text>
                <Text style={s.cardTagline}>{game.tagline}</Text>
              </View>
            </TouchableOpacity>
          ))}
        </View>
      </ScrollView>

      <GameSelectSheet
        gameId={selectedGameId}
        onDismiss={handleSheetDismiss}
        onSelectDifficulty={handleSheetSelectDifficulty}
        onRules={handleSheetRules}
        onTutorial={handleSheetTutorial}
        dialogVisible={dialogVisible}
        onDialogContinue={handleContinue}
        onDialogStartNew={handleGiveUpAndStartNew}
        onDialogDismiss={dismissDialog}
      />
    </GlobalPageShell>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  scroll: {
    padding: 20,
    gap: 20,
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
    gap: 14,
    padding: 20,
    backgroundColor: theme.surface,
    borderRadius: 16,
  },
  cardBeta: {
    borderWidth: 1,
    borderColor: withAlpha(theme.primaryLight, 0.3),
  },
  cardDisabled: {
    opacity: 0.45,
  },
  cardEmoji: {
    fontSize: 28,
    lineHeight: 36,
  },
  cardEmojiDisabled: {
    opacity: 0.5,
  },
  cardBody: {
    flex: 1,
  },
  cardTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: theme.text,
  },
  cardTitleDisabled: {
    color: theme.textSecondary,
  },
  cardTagline: {
    marginTop: 4,
    fontSize: 13,
    color: theme.textSecondary,
    lineHeight: 19,
  },
  comingSoonSection: {
    gap: 12,
  },
  sectionLabel: {
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 0.8,
    textTransform: 'uppercase',
    color: theme.textMuted,
  },
  betaDisclaimer: {
    fontSize: 13,
    lineHeight: 19,
    color: theme.textSecondary,
    marginTop: -4,
  },
});
