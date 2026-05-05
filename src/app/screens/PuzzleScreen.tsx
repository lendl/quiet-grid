import React, { useCallback, useMemo } from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import type { StackScreenProps } from '@react-navigation/stack';
import ActivePuzzleReplaceDialog from '../components/ActivePuzzleReplaceDialog';
import AppScreen from '../components/AppScreen';
import GridHomeIcon from '../components/GridHomeIcon';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { useActivePuzzleReplacement } from '../hooks/useActivePuzzleReplacement';
import { returnToHome } from '../navigation/returnToHome';
import { getPuzzleDefinition } from '../shell/games/gameRegistry';
import { withAlpha } from '../utils/color';
import { getDifficultyColor } from '../utils/format';
import { startGame } from '../utils/gameNavigation';
import type { RootStackParamList } from '../navigation/types';
import type { Difficulty } from '../types';
import type { Theme } from '../theme';

type Props = StackScreenProps<RootStackParamList, 'Puzzle'>;

type PuzzleAction = {
  key: string;
  label: string;
  onPress: () => void;
};

export default function PuzzleScreen({ navigation, route }: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const { puzzleTypeId } = route.params;
  const definition = getPuzzleDefinition(puzzleTypeId);
  const supportsTutorial = definition.supports.tutorial;
  const s = useMemo(() => makeStyles(theme), [theme]);
  const {
    dialogVisible: replaceDialogVisible,
    syncActivePuzzle,
    requestStart,
    handleContinue,
    handleGiveUpAndStartNew,
    dismissDialog,
  } = useActivePuzzleReplacement({ navigation });

  useFocusEffect(useCallback(() => {
    void syncActivePuzzle();
    return undefined;
  }, [syncActivePuzzle]));

  const handleSelectDifficulty = useCallback((difficulty: Difficulty) => {
    requestStart(() => {
      startGame(navigation, puzzleTypeId, difficulty);
    });
  }, [navigation, puzzleTypeId, requestStart]);

  const levels = definition.difficulties.map((key) => ({
    key,
    label: definition.content.difficultyLabels[key],
    description: definition.content.difficultyDescriptions[key],
  }));

  const actions: PuzzleAction[] = [
    {
      key: 'stats',
      label: strings.common.stats,
      onPress: () => navigation.navigate('Stats', { puzzleTypeId }),
    },
    {
      key: 'rules',
      label: strings.common.rules,
      onPress: () => navigation.navigate('HowToPlay', { puzzleTypeId }),
    },
  ];

  if (supportsTutorial) {
    actions.push({
      key: 'tutorial',
      label: strings.common.tutorial,
      onPress: () => navigation.navigate('Tutorial', { puzzleTypeId, entry: 'howToPlay' }),
    });
  }

  return (
    <AppScreen contentStyle={s.container}>
      <ScrollView contentContainerStyle={s.scroll}>
        <TouchableOpacity
          style={s.backButton}
          onPress={() => returnToHome(navigation)}
          accessibilityLabel={strings.common.goHome}
          activeOpacity={0.8}
        >
          <GridHomeIcon />
        </TouchableOpacity>

        <View style={s.header}>
          <Text style={s.title}>{definition.shortTitle}</Text>
          <Text style={s.tagline}>{definition.tagline}</Text>
        </View>

        <View style={s.actionRow}>
          {actions.map((action) => (
            <TouchableOpacity
              key={action.key}
              style={s.actionPill}
              onPress={action.onPress}
              activeOpacity={0.8}
              accessibilityRole="button"
              accessibilityLabel={`${action.label} for ${definition.shortTitle}`}
            >
              <Text style={s.actionPillText}>{action.label}</Text>
            </TouchableOpacity>
          ))}
        </View>

        <Text style={s.sectionLabel}>{strings.puzzle.chooseDifficulty}</Text>

        {levels.map((level, index) => (
          <React.Fragment key={level.key}>
            <TouchableOpacity
              style={s.diffRow}
              onPress={() => handleSelectDifficulty(level.key)}
              activeOpacity={0.78}
            >
              <View style={[s.diffMarker, { backgroundColor: getDifficultyColor(theme, level.key) }]} />
              <View style={s.diffBody}>
                <Text style={s.diffLabel}>{level.label}</Text>
                <Text style={s.diffDesc}>{level.description}</Text>
              </View>
              <Text style={s.diffAction}>{strings.common.play}</Text>
            </TouchableOpacity>
            {index < levels.length - 1 ? <View style={s.rowDivider} /> : null}
          </React.Fragment>
        ))}
      </ScrollView>

      <ActivePuzzleReplaceDialog
        visible={replaceDialogVisible}
        onContinue={handleContinue}
        onStartNew={handleGiveUpAndStartNew}
        onDismiss={dismissDialog}
      />
    </AppScreen>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  container: { flex: 1, backgroundColor: theme.background },
  scroll: { padding: 20, gap: 0 },
  backButton: {
    alignSelf: 'flex-start',
    minWidth: 44,
    minHeight: 36,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  header: {
    gap: 6,
  },
  title: {
    fontSize: 28,
    fontWeight: '900',
    color: theme.text,
  },
  tagline: {
    fontSize: 15,
    lineHeight: 22,
    color: theme.textSecondary,
  },
  actionRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
    marginTop: 18,
    marginBottom: 18,
  },
  actionPill: {
    minHeight: 40,
    paddingHorizontal: 14,
    borderRadius: 999,
    justifyContent: 'center',
    backgroundColor: withAlpha(theme.surfaceElevated, 0.95),
  },
  actionPillText: {
    fontSize: 13,
    fontWeight: '700',
    color: theme.text,
  },
  sectionLabel: {
    marginBottom: 4,
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 0.8,
    textTransform: 'uppercase',
    color: theme.textMuted,
  },
  diffRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
    paddingVertical: 18,
  },
  diffMarker: {
    width: 10,
    height: 46,
    borderRadius: 999,
  },
  diffBody: { flex: 1 },
  diffLabel: {
    fontSize: 19,
    fontWeight: '800',
    color: theme.text,
  },
  diffDesc: {
    marginTop: 4,
    fontSize: 13,
    lineHeight: 19,
    color: theme.textSecondary,
  },
  diffAction: {
    fontSize: 13,
    fontWeight: '700',
    color: theme.textMuted,
  },
  rowDivider: {
    height: 1,
    backgroundColor: withAlpha(theme.textSecondary, 0.14),
  },
});
