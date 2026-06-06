import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import type { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import GamePageShell from '../../components/GamePageShell';
import { useLanguage } from '../../context/LanguageContext';
import { useTheme } from '../../context/ThemeContext';
import type { GameTabParamList } from '../../navigation/types';
import { getGameDefinition } from '../../shell/games/gameRegistry';
import type { HowToPlayCellValue } from '../../shell/games/howToPlayContent';
import type { Theme } from '../../theme';

type Props = BottomTabScreenProps<GameTabParamList, 'Rules'>;

function MiniGrid({ rows }: { rows: readonly (readonly HowToPlayCellValue[])[] }) {
  const { theme } = useTheme();
  return (
    <View style={{ gap: 3, marginTop: 10, alignSelf: 'flex-start' }}>
      {rows.map((row, ri) => (
        <View key={ri} style={{ flexDirection: 'row', gap: 3 }}>
          {row.map((cell, ci) => {
            const isAnswer = cell === 'a0' || cell === 'a1';
            const value = cell === 'a0' ? 0 : cell === 'a1' ? 1 : cell;
            return (
              <View
                key={ci}
                style={{
                  width: 30,
                  height: 30,
                  borderRadius: 4,
                  alignItems: 'center',
                  justifyContent: 'center',
                  backgroundColor: isAnswer ? theme.primary : value !== null ? theme.filledBackground : theme.surfaceElevated,
                  borderWidth: 1,
                  borderColor: isAnswer ? theme.primaryLight : theme.border,
                }}
              >
                <Text
                  style={{
                    fontSize: 13,
                    fontWeight: '700',
                    color: isAnswer ? '#fff' : value !== null ? theme.filled : theme.textMuted,
                  }}
                >
                  {value !== null ? String(value) : ''}
                </Text>
              </View>
            );
          })}
        </View>
      ))}
    </View>
  );
}

export default function GameRulesTab({ route }: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const definition = getGameDefinition(route.params.gameId);
  const { howToPlay } = definition.content;
  const s = makeStyles(theme);

  return (
    <GamePageShell
      headerMode="back"
      contentTransitionDirection="forward"
      gameNav={{
        context: 'tabs',
        activeTab: 'Rules',
        gameId: route.params.gameId,
      }}
    >
      <ScrollView contentContainerStyle={s.scroll}>
        <Text style={s.sectionTitle}>{strings.howToPlay.rulesTitle(definition.shortTitle)}</Text>
        {howToPlay.rules.map((rule) => (
          <View key={rule.num} style={s.ruleCard}>
            <View style={s.badge}><Text style={s.badgeText}>{rule.num}</Text></View>
            <View style={s.ruleBody}>
              <Text style={s.ruleTitle}>{rule.title}</Text>
              <Text style={s.ruleText}>{rule.body}</Text>
            </View>
          </View>
        ))}

        <Text style={[s.sectionTitle, { marginTop: 20 }]}>{strings.howToPlay.tipsTitle}</Text>
        {howToPlay.tips.map((tip) => (
          <View key={tip.key} style={s.tipCard}>
            <Text style={s.tipTitle}>{tip.title}</Text>
            <Text style={s.tipText}>{tip.body}</Text>
            {tip.example ? <MiniGrid rows={tip.example} /> : null}
          </View>
        ))}
      </ScrollView>
    </GamePageShell>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  scroll: { padding: 20, gap: 12 },
  sectionTitle: { fontSize: 20, fontWeight: '800', color: theme.text, marginBottom: 4 },
  ruleCard: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    backgroundColor: theme.surface,
    borderRadius: 14,
    padding: 16,
    borderWidth: 1,
    borderColor: theme.border,
  },
  badge: { width: 32, height: 32, borderRadius: 8, backgroundColor: theme.primary, alignItems: 'center', justifyContent: 'center', marginRight: 12 },
  badgeText: { color: '#fff', fontWeight: '800', fontSize: 14 },
  ruleBody: { flex: 1 },
  ruleTitle: { fontSize: 15, fontWeight: '700', color: theme.text, marginBottom: 3 },
  ruleText: { fontSize: 13, color: theme.textSecondary, lineHeight: 20 },
  tipCard: {
    backgroundColor: theme.surface,
    borderRadius: 14,
    padding: 16,
    borderWidth: 1,
    borderColor: theme.border,
  },
  tipTitle: { fontSize: 14, fontWeight: '700', color: theme.text, marginBottom: 4 },
  tipText: { fontSize: 13, color: theme.textSecondary, lineHeight: 20 },
});
