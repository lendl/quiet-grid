import React, { useState, useCallback, useMemo } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity } from 'react-native';
import type { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { useFocusEffect } from '@react-navigation/native';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import AppDialog from '../components/AppDialog';
import GlobalPageShell from '../components/GlobalPageShell';
import { gameRegistry } from '../shell/games/gameRegistry';
import { getDifficultyColor } from '../utils/format';
import { withAlpha } from '../utils/color';
import { loadStats, clearPlayerData } from '../utils/statsStorage';
import {
  STATS_DIFFICULTIES,
  getMergedPuzzleStats,
  getMergedPuzzleStreak,
  getPuzzleStats,
  getPuzzleStreak,
  getStatsSummary,
} from '../utils/statsUtils';
import type { MainTabParamList } from '../navigation/types';
import type { AppStats, GameId } from '../types';
import type { Theme } from '../theme';

function fmtScore(n: number | null): string {
  if (n === null) return '—';
  return String(n);
}

type GlobalStatsFilter = 'all' | GameId;

type Props = BottomTabScreenProps<MainTabParamList, 'Stats'>;

export default function StatsScreen({ navigation, route }: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const s = useMemo(() => makeStyles(theme), [theme]);
  const [stats, setStats] = useState<AppStats | null>(null);
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
    void loadStats().then(setStats);
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

  const filteredPuzzleTypeIds = effectiveFilter === 'all'
    ? availableDefinitions.map((definition) => definition.id)
    : [effectiveFilter];

  const filteredDefinition = effectiveFilter === 'all'
    ? null
    : availableDefinitions.find((d) => d.id === effectiveFilter) ?? null;
  const mergedDifficultyLabels = availableDefinitions[0]?.content.difficultyLabels ?? null;

  const activeGameStats = effectiveFilter === 'all'
    ? getMergedPuzzleStats(stats, filteredPuzzleTypeIds)
    : getPuzzleStats(stats, effectiveFilter);

  const summary = getStatsSummary(activeGameStats);
  const streak = effectiveFilter === 'all'
    ? getMergedPuzzleStreak(stats, filteredPuzzleTypeIds)
    : getPuzzleStreak(stats, effectiveFilter);

  return (
    <GlobalPageShell activeTab="Stats">
      <ScrollView contentContainerStyle={s.scroll}>
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

        <View style={s.header}>
          <Text style={s.headerTitle}>
            {filteredDefinition ? filteredDefinition.shortTitle : strings.common.stats}
          </Text>
          <Text style={s.headerSubtitle}>{strings.stats.headerSubtitle}</Text>
        </View>

        <View style={s.summaryBand}>
          {([
            [strings.stats.solved, summary.totalSolved],
            [strings.stats.streak, streak],
            [strings.stats.winRate, `${summary.winRate}%`],
          ] as [string, number | string][]).map(([label, value], index) => (
            <React.Fragment key={label}>
              <View style={s.summaryMetric}>
                <Text style={s.summaryValue}>{value}</Text>
                <Text style={s.summaryLabel}>{label}</Text>
              </View>
              {index < 2 ? <View style={s.summaryDivider} /> : null}
            </React.Fragment>
          ))}
        </View>

        <Text style={s.subsectionLabel}>{strings.stats.byDifficulty}</Text>

        {STATS_DIFFICULTIES.map((key, index) => {
          const difficultyStats = activeGameStats[key];
          const rate = difficultyStats.played > 0
            ? Math.round((difficultyStats.solved / difficultyStats.played) * 100)
            : 0;

          const difficultyLabel = filteredDefinition?.content.difficultyLabels[key]
            ?? mergedDifficultyLabels?.[key]
            ?? key.charAt(0).toUpperCase() + key.slice(1);

          return (
            <React.Fragment key={key}>
              <View style={s.diffRow}>
                <View style={[s.diffMarker, { backgroundColor: getDifficultyColor(theme, key) }]} />
                <View style={s.diffInfo}>
                  <Text style={s.diffName}>{difficultyLabel}</Text>
                  <Text style={s.diffDetail}>{strings.stats.solvedOutOfPlayed(difficultyStats.solved, difficultyStats.played)}</Text>
                  <Text style={s.diffDetail}>{strings.stats.winRateDetail(rate)}</Text>
                </View>
                <View style={s.diffRight}>
                  <Text style={s.bestLabel}>{strings.stats.bestScore}</Text>
                  <Text style={s.bestValue}>{fmtScore(difficultyStats.bestScore)}</Text>
                </View>
              </View>
              {index < STATS_DIFFICULTIES.length - 1 ? <View style={s.rowDivider} /> : null}
            </React.Fragment>
          );
        })}

        {scopedDefinition ? null : (
          <View style={s.privacySection}>
            <Text style={s.privacyLabel}>{strings.stats.privacy}</Text>
            <Text style={s.privacyText}>
              {strings.stats.privacyText}
            </Text>
          </View>
        )}

        {scopedDefinition ? null : (
          <TouchableOpacity style={s.clearButton} onPress={handleClear} activeOpacity={0.82}>
            <Text style={s.clearButtonText}>{strings.stats.clearData}</Text>
          </TouchableOpacity>
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
              .then(() => loadStats().then(setStats))
              .finally(() => setClearDialogVisible(false)),
          },
        ]}
        onDismiss={() => setClearDialogVisible(false)}
      />
    </GlobalPageShell>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  container:    { flex: 1, backgroundColor: theme.background },
  scroll:       { padding: 20, gap: 0 },
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
  header: {
    marginBottom: 10,
    gap: 4,
  },
  headerTitle: {
    fontSize: 26,
    fontWeight: '900',
    color: theme.text,
  },
  headerSubtitle: {
    fontSize: 14,
    lineHeight: 21,
    color: theme.textSecondary,
  },
  summaryBand: {
    flexDirection: 'row',
    alignItems: 'stretch',
    paddingVertical: 8,
  },
  summaryMetric: {
    flex: 1,
    alignItems: 'center',
    paddingHorizontal: 10,
  },
  summaryDivider: {
    width: 1,
    backgroundColor: withAlpha(theme.textSecondary, 0.18),
  },
  summaryValue: {
    fontSize: 31,
    fontWeight: '900',
    color: theme.text,
    lineHeight: 34,
  },
  summaryLabel: {
    marginTop: 10,
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 0.8,
    textTransform: 'uppercase',
    color: theme.textSecondary,
  },
  subsectionLabel: {
    marginTop: 10,
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 0.7,
    textTransform: 'uppercase',
    color: theme.textMuted,
  },
  badgeScroller: {
    marginBottom: 12,
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
  diffRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingVertical: 16,
  },
  diffMarker: {
    width: 10,
    height: 44,
    borderRadius: 999,
  },
  diffInfo: { flex: 1 },
  diffName: {
    fontSize: 17,
    fontWeight: '800',
    color: theme.text,
  },
  diffDetail: {
    marginTop: 2,
    fontSize: 13,
    lineHeight: 19,
    color: theme.textSecondary,
  },
  diffRight: { alignItems: 'flex-end' },
  bestLabel: {
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 0.6,
    textTransform: 'uppercase',
    color: theme.textMuted,
  },
  bestValue: {
    marginTop: 5,
    fontSize: 19,
    fontWeight: '800',
    color: theme.text,
  },
  rowDivider: {
    height: 1,
    backgroundColor: withAlpha(theme.textSecondary, 0.14),
  },
  privacySection: {
    marginTop: 18,
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
  clearButton: {
    marginTop: 22,
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
