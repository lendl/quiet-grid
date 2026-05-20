import React, { useState, useCallback, useMemo } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity } from 'react-native';
import type { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { useFocusEffect } from '@react-navigation/native';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import AppDialog from '../components/AppDialog';
import GlobalPageShell from '../components/GlobalPageShell';
import { gameRegistry } from '../shell/games/gameRegistry';
import { withAlpha } from '../utils/color';
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
            <TouchableOpacity
              style={s.backButton}
              onPress={clearScope}
              accessibilityLabel={strings.common.goBack}
              activeOpacity={0.8}
            >
              <Text style={s.backButtonText}>{strings.common.back}</Text>
            </TouchableOpacity>
          ) : null}

          {scopedDefinition ? null : (
            <ScrollView
              horizontal
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={s.badgeRow}
              style={s.badgeScroller}
            >
              <TouchableOpacity
                style={[s.filterBadge, effectiveFilter === 'all' && s.filterBadgeActive]}
                onPress={() => setActiveFilter('all')}
                activeOpacity={0.82}
                accessibilityRole="button"
              >
                <Text style={[s.filterBadgeText, effectiveFilter === 'all' && s.filterBadgeTextActive]}>
                  {strings.common.all}
                </Text>
              </TouchableOpacity>

              {availableDefinitions.map((definition) => {
                const selected = effectiveFilter === definition.id;

                return (
                  <TouchableOpacity
                    key={definition.id}
                    style={[s.filterBadge, selected && s.filterBadgeActive]}
                    onPress={() => setActiveFilter(definition.id)}
                    activeOpacity={0.82}
                    accessibilityRole="button"
                  >
                    <Text style={[s.filterBadgeText, selected && s.filterBadgeTextActive]}>
                      {definition.shortTitle}
                    </Text>
                  </TouchableOpacity>
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
            <TouchableOpacity style={s.clearButton} onPress={handleClear} activeOpacity={0.82}>
              <Text style={s.clearButtonText}>{strings.stats.clearData}</Text>
            </TouchableOpacity>
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
  filterBadge: {
    minHeight: 34,
    paddingHorizontal: 14,
    borderRadius: 999,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: withAlpha(theme.textSecondary, 0.12),
  },
  filterBadgeActive: {
    backgroundColor: theme.text,
  },
  filterBadgeText: {
    fontSize: 13,
    fontWeight: '700',
    color: theme.text,
  },
  filterBadgeTextActive: {
    color: theme.background,
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
  clearButton: {
    backgroundColor: theme.surfaceElevated,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: theme.border,
    paddingVertical: 15,
    alignItems: 'center',
  },
  clearButtonText: {
    color: theme.text,
    fontSize: 16,
    fontWeight: '700',
  },
});
