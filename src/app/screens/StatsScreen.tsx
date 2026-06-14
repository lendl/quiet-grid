import React, { useState, useCallback, useMemo } from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import { Button, Chip, TouchableRipple } from 'react-native-paper';
import type { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { useFocusEffect } from '@react-navigation/native';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import AppDialog from '../components/AppDialog';
import GlobalPageShell from '../components/GlobalPageShell';
import { gameRegistry } from '../shell/games/gameRegistry';
import { clearPlayerData } from '../utils/statsStorage';
import type { MainTabParamList } from '../navigation/types';
import type { GameId } from '../types';
import type { Theme } from '../theme';
import StatsOverviewSection from '../stats/components/StatsOverviewSection';
import { useStatsSnapshot } from '../stats/hooks/useStatsSnapshot';
import { buildStatsOverview, type StatsScope } from '../stats/model/buildStatsOverview';

type GlobalStatsFilter = 'all' | GameId;

type Props = BottomTabScreenProps<MainTabParamList, 'Stats'>;

export default function StatsScreen({ navigation, route }: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const s = useMemo(() => makeStyles(theme), [theme]);
  const { stats, reload } = useStatsSnapshot();
  const [clearDialogVisible, setClearDialogVisible] = useState(false);
  const [activeFilter, setActiveFilter] = useState<GlobalStatsFilter>('all');
  const scopedGameId = route.params?.gameId;
  const scopedDefinition = scopedGameId ? gameRegistry.find((d) => d.id === scopedGameId) ?? null : null;
  const clearScope = useCallback(() => {
    navigation.setParams({
      gameId: undefined,
    });
  }, [navigation]);

  useFocusEffect(useCallback(() => {
    setActiveFilter('all');
    return undefined;
  }, []));

  if (!stats) {
    return (
      <GlobalPageShell activeTab="Stats" />
    );
  }

  const handleClear = () => setClearDialogVisible(true);

  const availableDefinitions = gameRegistry;
  const hasActiveDefinition = activeFilter === 'all'
    || availableDefinitions.some((definition) => definition.id === activeFilter);
  const effectiveFilter: GlobalStatsFilter = scopedDefinition
    ? scopedDefinition.id
    : hasActiveDefinition
      ? activeFilter
      : 'all';
  const overviewScope: StatsScope = effectiveFilter === 'all'
    ? { kind: 'all' }
    : { kind: 'game', gameId: effectiveFilter };
  const overview = buildStatsOverview(stats, overviewScope);

  return (
    <GlobalPageShell activeTab="Stats">
      <ScrollView contentContainerStyle={s.scroll}>
        <View style={s.controlsSection}>
          {scopedDefinition ? (
            <TouchableRipple
              style={s.backButton}
              onPress={clearScope}
              accessibilityLabel={strings.common.goBack}
              borderless
            >
              <Text style={s.backButtonText}>{strings.common.back}</Text>
            </TouchableRipple>
          ) : null}

          {scopedDefinition ? null : (
            <ScrollView
              horizontal
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={s.badgeRow}
              style={s.badgeScroller}
            >
              <Chip
                mode="flat"
                selected={effectiveFilter === 'all'}
                onPress={() => setActiveFilter('all')}
                compact
              >
                {strings.common.all}
              </Chip>

              {availableDefinitions.map((definition) => {
                const selected = effectiveFilter === definition.id;

                return (
                  <Chip
                    key={definition.id}
                    mode="flat"
                    selected={selected}
                    onPress={() => setActiveFilter(definition.id)}
                    compact
                  >
                    {definition.shortTitle}
                  </Chip>
                );
              })}
            </ScrollView>
          )}
        </View>

        <View style={s.contentSection}>
          <StatsOverviewSection
            title={strings.stats.byDifficulty}
            solvedLabel={strings.stats.solved}
            streakLabel={strings.stats.streak}
            winRateLabel={strings.stats.winRate}
            bestScoreLabel={strings.stats.bestScore}
            solvedOutOfPlayed={strings.stats.solvedOutOfPlayed}
            winRateDetail={strings.stats.winRateDetail}
            model={overview}
          />
        </View>

        {scopedDefinition ? null : (
          <View style={s.privacySection}>
            <Text style={s.privacyLabel}>{strings.stats.privacy}</Text>
            <Text style={s.privacyText}>
              {strings.stats.privacyText}
            </Text>
          </View>
        )}

        {scopedDefinition ? null : (
          <View style={s.clearDataSection}>
            <Button mode="text" onPress={handleClear} textColor={theme.difficultyExpert}>
            {strings.stats.clearData}
          </Button>
          </View>
        )}
      </ScrollView>

      <AppDialog
        visible={clearDialogVisible}
        title={strings.stats.clearDataTitle}
        message={strings.stats.clearDataMessage}
        buttons={[
          { text: strings.common.cancel, style: 'cancel', onPress: () => setClearDialogVisible(false) },
          {
            text: strings.common.clear,
            onPress: () => clearPlayerData()
              .then(reload)
              .finally(() => setClearDialogVisible(false)),
          },
        ]}
        onDismiss={() => setClearDialogVisible(false)}
      />
    </GlobalPageShell>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  scroll: { padding: 20, paddingBottom: 32 },
  controlsSection: {
    marginBottom: 8,
  },
  contentSection: {
    marginBottom: 20,
  },
  backButton: {
    alignSelf: 'flex-start',
    minHeight: 36,
    justifyContent: 'center',
    marginBottom: 6,
  },
  backButtonText: {
    fontSize: 13,
    fontWeight: '700',
    color: theme.textSecondary,
  },
  badgeScroller: {
    marginBottom: 4,
  },
  badgeRow: {
    gap: 10,
    paddingRight: 6,
  },
  privacySection: {
    marginTop: 4,
  },
  privacyLabel: {
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 0.8,
    textTransform: 'uppercase',
    color: theme.textSecondary,
  },
  privacyText: {
    marginTop: 6,
    fontSize: 13,
    lineHeight: 20,
    color: theme.textSecondary,
  },
  clearDataSection: {
    marginTop: 24,
  },
});
