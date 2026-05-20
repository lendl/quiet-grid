import React, { useCallback, useMemo, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import type { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import GamePageShell from '../../components/GamePageShell';
import { useLanguage } from '../../context/LanguageContext';
import { useTheme } from '../../context/ThemeContext';
import type { GameTabParamList } from '../../navigation/types';
import { getGameDefinition } from '../../shell/games/gameRegistry';
import type { Theme } from '../../theme';
import type { AppStats, Difficulty } from '../../types';
import { withAlpha } from '../../utils/color';
import { getDifficultyColor } from '../../utils/format';
import { loadStats } from '../../utils/statsStorage';
import { getPuzzleStats, getPuzzleStreak } from '../../utils/statsUtils';

const DIFFS: Difficulty[] = ['easy', 'medium', 'hard', 'expert'];

function fmtScore(n: number | null): string {
  if (n === null) return '—';
  return String(n);
}

type Props = BottomTabScreenProps<GameTabParamList, 'Stats'>;

export default function GameStatsTab({ route }: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const s = useMemo(() => makeStyles(theme), [theme]);
  const [stats, setStats] = useState<AppStats | null>(null);
  const definition = getGameDefinition(route.params.gameId);

  useFocusEffect(useCallback(() => {
    void loadStats().then(setStats);
    return undefined;
  }, []));

  if (!stats) {
    return (
      <GamePageShell
        activeTab="Games"
        headerMode="brand"
        contentTransitionDirection="forward"
        gameNav={{
          context: 'tabs',
          activeTab: 'Stats',
          gameId: route.params.gameId,
        }}
      />
    );
  }

  const gameStats = getPuzzleStats(stats, route.params.gameId);
  const totalPlayed = DIFFS.reduce((sum, difficulty) => sum + gameStats[difficulty].played, 0);
  const totalSolved = DIFFS.reduce((sum, difficulty) => sum + gameStats[difficulty].solved, 0);
  const winRate = totalPlayed > 0 ? Math.round((totalSolved / totalPlayed) * 100) : 0;
  const streak = getPuzzleStreak(stats, route.params.gameId);

  return (
    <GamePageShell
      activeTab="Games"
      headerMode="brand"
      contentTransitionDirection="forward"
      gameNav={{
        context: 'tabs',
        activeTab: 'Stats',
        gameId: route.params.gameId,
      }}
    >
      <ScrollView contentContainerStyle={s.scroll}>
        <View style={s.header}>
          <Text style={s.headerTitle}>{definition.shortTitle}</Text>
          <Text style={s.headerSubtitle}>{strings.stats.headerSubtitle}</Text>
        </View>

        <View style={s.summaryBand}>
          {([
            [strings.stats.solved, totalSolved],
            [strings.stats.streak, streak],
            [strings.stats.winRate, `${winRate}%`],
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

        {DIFFS.map((key, index) => {
          const difficultyStats = gameStats[key];
          const rate = difficultyStats.played > 0
            ? Math.round((difficultyStats.solved / difficultyStats.played) * 100)
            : 0;

          return (
            <React.Fragment key={key}>
              <View style={s.diffRow}>
                <View style={[s.diffMarker, { backgroundColor: getDifficultyColor(theme, key) }]} />
                <View style={s.diffInfo}>
                  <Text style={s.diffName}>{definition.content.difficultyLabels[key]}</Text>
                  <Text style={s.diffDetail}>{strings.stats.solvedOutOfPlayed(difficultyStats.solved, difficultyStats.played)}</Text>
                  <Text style={s.diffDetail}>{strings.stats.winRateDetail(rate)}</Text>
                </View>
                <View style={s.diffRight}>
                  <Text style={s.bestLabel}>{strings.stats.bestScore}</Text>
                  <Text style={s.bestValue}>{fmtScore(difficultyStats.bestScore)}</Text>
                </View>
              </View>
              {index < DIFFS.length - 1 ? <View style={s.rowDivider} /> : null}
            </React.Fragment>
          );
        })}
      </ScrollView>
    </GamePageShell>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  scroll: { padding: 20, gap: 0 },
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
});
