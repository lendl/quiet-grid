import React, { useCallback, useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Button, Divider } from 'react-native-paper';
import Ionicons from '@react-native-vector-icons/ionicons';
import { StackActions, useNavigation } from '@react-navigation/native';
import type { NavigationProp } from '@react-navigation/native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import GamePageShell from '../components/GamePageShell';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { getGameDefinition } from '../shell/games/gameRegistry';
import type { RootStackParamList } from '../navigation/types';
import type { Theme } from '../theme';
import { markGameHowToPlaySeen } from '../utils/settingsStorage';

type Props = NativeStackScreenProps<RootStackParamList, 'HowToPlay'>;

function SectionHeader({ icon, title, styles }: { icon: React.ComponentProps<typeof Ionicons>['name']; title: string; styles: ReturnType<typeof makeStyles> }) {
  const { theme } = useTheme();
  return (
    <View style={styles.sectionHeader}>
      <Ionicons name={icon} size={18} color={theme.textMuted} style={styles.sectionIcon} />
      <Text style={styles.sectionTitle}>{title}</Text>
    </View>
  );
}

function Accordion({ title, children, styles }: { title: string; children: React.ReactNode; styles: ReturnType<typeof makeStyles> }) {
  const { theme } = useTheme();
  const [expanded, setExpanded] = useState(false);
  return (
    <>
      <TouchableOpacity style={styles.accordionRow} onPress={() => setExpanded(!expanded)} activeOpacity={0.7}>
        <Text style={styles.accordionTitle}>{title}</Text>
        <Ionicons name={expanded ? 'chevron-up' : 'chevron-down'} size={18} color={theme.textMuted} />
      </TouchableOpacity>
      {expanded ? <View style={styles.accordionBody}>{children}</View> : null}
    </>
  );
}

export default function HowToPlayScreen({ route }: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const navigation = useNavigation<NavigationProp<RootStackParamList>>();
  const definition = getGameDefinition(route.params.gameId);
  const { howToPlay } = definition.content;
  const isFirstLaunch = route.params.isFirstLaunch === true;
  const s = makeStyles(theme);

  useEffect(() => {
    if (isFirstLaunch) {
      void markGameHowToPlaySeen(route.params.gameId);
    }
  }, [isFirstLaunch, route.params.gameId]);

  const handleQuickPlay = useCallback(() => {
    navigation.dispatch(StackActions.replace('PuzzlePlay', { puzzleTypeId: route.params.gameId, difficulty: 'easy' }));
  }, [navigation, route.params.gameId]);

  const handleChooseDifficulty = useCallback(() => {
    navigation.dispatch(StackActions.replace('Game', { gameId: route.params.gameId }));
  }, [navigation, route.params.gameId]);

  return (
    <GamePageShell
      activeTab="Games"
      headerMode="brand"
      contentTransitionDirection="forward"
      gameNav={{
        context: 'root',
        activeTab: 'Rules',
        gameId: route.params.gameId,
      }}
    >
      <ScrollView contentContainerStyle={s.scroll}>

        <SectionHeader icon="trophy-outline" title={strings.howToPlay.goalTitle} styles={s} />
        <Text style={s.bodyText}>{howToPlay.goal}</Text>

        <Divider style={s.divider} />

        <SectionHeader icon="hand-left-outline" title={strings.howToPlay.controlsTitle} styles={s} />
        <Text style={s.bodyText}>{howToPlay.controls}</Text>

        {isFirstLaunch && (
          <View style={s.ctaRow}>
            <Button mode="contained" onPress={handleQuickPlay} style={s.ctaBtn} contentStyle={s.ctaContent}>
              {strings.howToPlay.quickPlay}
            </Button>
            <Button mode="outlined" onPress={handleChooseDifficulty} style={s.ctaBtn} contentStyle={s.ctaContent}>
              {strings.howToPlay.chooseDifficulty}
            </Button>
          </View>
        )}

        {!!howToPlay.wrongMove && (
          <>
            <Divider style={s.divider} />
            <SectionHeader icon="warning-outline" title={strings.howToPlay.wrongMoveTitle} styles={s} />
            <Text style={s.bodyText}>{howToPlay.wrongMove}</Text>
          </>
        )}

        <Divider style={s.divider} />

        <SectionHeader icon="document-text-outline" title={strings.howToPlay.rulesTitle} styles={s} />
        <View style={s.ruleList}>
          {howToPlay.rules.map((rule) => (
            <View key={rule.num} style={s.ruleRow}>
              <View style={s.badge}><Text style={s.badgeText}>{rule.num}</Text></View>
              <View style={s.ruleBody}>
                <Text style={s.ruleTitle}>{rule.title}</Text>
                <Text style={s.ruleText}>{rule.body}</Text>
              </View>
            </View>
          ))}
        </View>

        {howToPlay.techniques.length > 0 && (
          <>
            <Divider style={s.divider} />
            <Accordion title={strings.howToPlay.techniquesTitle} styles={s}>
              <View style={s.techniqueList}>
                {howToPlay.techniques.map((technique) => (
                  <View key={technique.key} style={s.techniqueItem}>
                    <Text style={s.techniqueTitle}>{technique.title}</Text>
                    <Text style={s.techniqueText}>{technique.body}</Text>
                  </View>
                ))}
              </View>
            </Accordion>
          </>
        )}

        {!!howToPlay.scoring && (
          <>
            <Divider style={s.divider} />
            <Accordion title={strings.howToPlay.scoringTitle} styles={s}>
              <Text style={s.bodyText}>{howToPlay.scoring}</Text>
            </Accordion>
          </>
        )}

        {howToPlay.tips.length > 0 && (
          <>
            <Divider style={s.divider} />
            <Accordion title={strings.howToPlay.tipsTitle} styles={s}>
              {howToPlay.tips.map((tip) => (
                <View key={tip.key} style={s.tipItem}>
                  <Text style={s.tipTitle}>{tip.title}</Text>
                  <Text style={s.tipText}>{tip.body}</Text>
                </View>
              ))}
            </Accordion>
          </>
        )}

        <Divider style={s.divider} />

        <Accordion title={strings.howToPlay.inGameIconsTitle} styles={s}>
          <View style={s.iconList}>
            {strings.howToPlay.inGameIcons.map((item) => (
              <View key={item.label} style={s.iconRow}>
                <View style={s.iconBadge}>
                  <Ionicons name={item.icon as React.ComponentProps<typeof Ionicons>['name']} size={20} color={theme.textSecondary} />
                </View>
                <View style={s.iconBody}>
                  <Text style={s.iconLabel}>{item.label}</Text>
                  <Text style={s.iconDescription}>{item.description}</Text>
                </View>
              </View>
            ))}
          </View>
        </Accordion>

      </ScrollView>
    </GamePageShell>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  scroll:          { padding: 20, paddingBottom: 36 },
  divider:         { marginVertical: 20, backgroundColor: theme.border },
  sectionHeader:   { flexDirection: 'row', alignItems: 'center', marginBottom: 12 },
  sectionIcon:     { marginRight: 8 },
  sectionTitle:    { fontSize: 16, fontWeight: '700', color: theme.text },
  bodyText:        { fontSize: 14, color: theme.textSecondary, lineHeight: 22 },
  ctaRow:          { flexDirection: 'row', gap: 10, marginTop: 14 },
  ctaBtn:          { flex: 1, borderRadius: 12 },
  ctaContent:      { paddingVertical: 2 },
  ruleList:        { gap: 10 },
  ruleRow:         { flexDirection: 'row', alignItems: 'flex-start' },
  badge:           { width: 28, height: 28, borderRadius: 7, backgroundColor: theme.primary, alignItems: 'center', justifyContent: 'center', marginRight: 12, flexShrink: 0 },
  badgeText:       { color: '#fff', fontWeight: '800', fontSize: 13 },
  ruleBody:        { flex: 1 },
  ruleTitle:       { fontSize: 14, fontWeight: '700', color: theme.text, marginBottom: 2 },
  ruleText:        { fontSize: 13, color: theme.textSecondary, lineHeight: 20 },
  accordionRow:    { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingVertical: 4 },
  accordionTitle:  { fontSize: 16, fontWeight: '700', color: theme.text },
  accordionBody:   { marginTop: 12 },
  techniqueList:   { gap: 12 },
  techniqueItem:   { paddingVertical: 2 },
  techniqueTitle:  { fontSize: 14, fontWeight: '700', color: theme.text, marginBottom: 3 },
  techniqueText:   { fontSize: 13, color: theme.textSecondary, lineHeight: 20 },
  tipItem:         { paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: theme.border },
  tipTitle:        { fontSize: 14, fontWeight: '700', color: theme.text, marginBottom: 4 },
  tipText:         { fontSize: 13, color: theme.textSecondary, lineHeight: 20 },
  iconList:        { gap: 14 },
  iconRow:         { flexDirection: 'row', alignItems: 'flex-start' },
  iconBadge:       { width: 36, height: 36, borderRadius: 10, backgroundColor: theme.surfaceElevated, alignItems: 'center', justifyContent: 'center', marginRight: 12, flexShrink: 0 },
  iconLabel:       { fontSize: 14, fontWeight: '700', color: theme.text, marginBottom: 2 },
  iconBody:        { flex: 1 },
  iconDescription: { fontSize: 13, color: theme.textSecondary, lineHeight: 20 },
});
