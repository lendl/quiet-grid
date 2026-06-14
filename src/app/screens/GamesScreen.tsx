import React, { useCallback, useMemo } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { Button, Card, Chip } from 'react-native-paper';
import { useFocusEffect, useNavigation, type NavigationProp } from '@react-navigation/native';
import type { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import ActivePuzzleReplaceDialog from '../components/ActivePuzzleReplaceDialog';
import GlobalPageShell from '../components/GlobalPageShell';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { useActivePuzzleReplacement } from '../hooks/useActivePuzzleReplacement';
import type { RootStackParamList, MainTabParamList } from '../navigation/types';
import { gameRegistry } from '../shell/games/gameRegistry';
import type { Theme } from '../theme';
import { getActivePuzzleDisplay } from '../utils/activePuzzle';
import { withAlpha } from '../utils/color';
import { loadBetaGamesEnabled, shouldAutoShowHowToPlay } from '../utils/settingsStorage';

type Props = BottomTabScreenProps<MainTabParamList, 'Games'>;

export default function GamesScreen(_: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const navigation = useNavigation<NavigationProp<RootStackParamList>>();
  const s = useMemo(() => makeStyles(theme), [theme]);
  const [betaGamesEnabled, setBetaGamesEnabled] = React.useState(false);
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

  const handleSelectGame = useCallback((gameId: typeof gameRegistry[number]['id']) => {
    requestStart(() => {
      void (async () => {
        if (await shouldAutoShowHowToPlay(gameId)) {
          navigation.navigate('HowToPlay', { gameId, isFirstLaunch: true });
          return;
        }

        navigation.navigate('Game', { gameId });
      })();
    });
  }, [navigation, requestStart]);

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
                <Chip key={meta} mode="flat" compact style={s.activeGameMetaChip} textStyle={s.activeGameMetaText}>
                  {meta}
                </Chip>
              ))}
            </View>
            <Button mode="contained" onPress={handleContinue} style={s.activeGameAction}>
              {`▶  ${strings.common.continuePuzzle}`}
            </Button>
          </View>
        ) : null}

        <View style={s.gameList}>
          {readyGames.map((game) => (
            <Card
              key={game.id}
              mode="elevated"
              style={s.card}
              onPress={() => handleSelectGame(game.id)}
            >
              <View style={s.cardBody}>
                <Text style={s.cardTitle}>{game.title}</Text>
                <Text style={s.cardTagline}>{game.tagline}</Text>
              </View>
            </Card>
          ))}
        </View>

        <View style={s.comingSoonSection}>
          <Text style={s.sectionLabel}>{strings.games.comingSoon}</Text>
          {betaGamesEnabled ? (
            <Text style={s.betaDisclaimer}>{strings.games.betaDisclaimer}</Text>
          ) : null}
          {betaGames.map((game) => (
            <Card
              key={game.id}
              mode="elevated"
              style={[s.card, betaGamesEnabled ? s.cardBeta : s.cardDisabled]}
              onPress={betaGamesEnabled ? () => handleSelectGame(game.id) : undefined}
              disabled={!betaGamesEnabled}
            >
              <View style={s.cardBody}>
                <Text style={[s.cardTitle, !betaGamesEnabled && s.cardTitleDisabled]}>{game.title}</Text>
                <Text style={s.cardTagline}>{game.tagline}</Text>
              </View>
            </Card>
          ))}
        </View>
      </ScrollView>

      <ActivePuzzleReplaceDialog
        visible={dialogVisible}
        onContinue={handleContinue}
        onStartNew={handleGiveUpAndStartNew}
        onDismiss={dismissDialog}
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
    backgroundColor: theme.surfaceElevated,
    gap: 10,
    elevation: 3,
    shadowColor: '#000',
    shadowOpacity: 0.18,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 2 },
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
  activeGameMetaChip: {
    backgroundColor: withAlpha(theme.text, 0.08),
  },
  activeGameMetaText: {
    fontSize: 12,
    color: theme.text,
  },
  activeGameAction: {
    marginTop: 4,
    borderRadius: 12,
  },
  gameList: {
    gap: 12,
  },
  card: {
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
  cardBody: {
    padding: 20,
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
