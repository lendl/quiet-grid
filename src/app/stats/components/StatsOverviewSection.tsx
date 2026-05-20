import React, { useMemo } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useTheme } from '../../context/ThemeContext';
import type { Theme } from '../../theme';
import { withAlpha } from '../../utils/color';
import { getDifficultyColor } from '../../utils/format';
import type { StatsOverviewModel } from '../model/buildStatsOverview';

interface Props {
  title: string;
  solvedLabel: string;
  streakLabel: string;
  winRateLabel: string;
  bestScoreLabel: string;
  solvedOutOfPlayed: (solved: number, played: number) => string;
  winRateDetail: (rate: number) => string;
  model: StatsOverviewModel;
}

function fmtScore(n: number | null): string {
  return n === null ? '—' : String(n);
}

export default function StatsOverviewSection({
  title,
  solvedLabel,
  streakLabel,
  winRateLabel,
  bestScoreLabel,
  solvedOutOfPlayed,
  winRateDetail,
  model,
}: Props) {
  const { theme } = useTheme();
  const s = useMemo(() => makeStyles(theme), [theme]);

  return (
    <View>
      <View style={s.summaryBand}>
        {([
          [solvedLabel, model.summary.totalSolved],
          [streakLabel, model.summary.streak],
          [winRateLabel, `${model.summary.winRate}%`],
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

      <Text style={s.subsectionLabel}>{title}</Text>

      {model.difficultyRows.map((row, index) => (
        <React.Fragment key={row.difficulty}>
          <View style={s.diffRow}>
            <View style={[s.diffMarker, { backgroundColor: getDifficultyColor(theme, row.difficulty) }]} />
            <View style={s.diffInfo}>
              <Text style={s.diffName}>{row.label}</Text>
              <Text style={s.diffDetail}>{solvedOutOfPlayed(row.stats.solved, row.stats.played)}</Text>
              <Text style={s.diffDetail}>{winRateDetail(row.winRate)}</Text>
            </View>
            <View style={s.diffRight}>
              <Text style={s.bestLabel}>{bestScoreLabel}</Text>
              <Text style={s.bestValue}>{fmtScore(row.stats.bestScore)}</Text>
            </View>
          </View>
          {index < model.difficultyRows.length - 1 ? <View style={s.rowDivider} /> : null}
        </React.Fragment>
      ))}
    </View>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
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
    marginTop: 14,
    marginBottom: 4,
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
