import React, { useCallback, useMemo } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { Divider, TouchableRipple } from 'react-native-paper';
import { useFocusEffect, useNavigation, type NavigationProp } from '@react-navigation/native';
import ActivePuzzleReplaceDialog from '../../components/ActivePuzzleReplaceDialog';
import { useLanguage } from '../../context/LanguageContext';
import { useTheme } from '../../context/ThemeContext';
import { useActivePuzzleReplacement } from '../../hooks/useActivePuzzleReplacement';
import type { RootStackParamList } from '../../navigation/types';
import { getGameDefinition } from '../../shell/games/gameRegistry';
import type { Theme } from '../../theme';
import type { Difficulty } from '../../types';
import { getDifficultyColor } from '../../utils/format';
import { startGame } from '../../utils/gameNavigation';
import type { GameId } from '../../../games/shared/types';

type Props = {
  gameId: GameId;
};

export default function GamePlayTab({ gameId }: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const navigation = useNavigation<NavigationProp<RootStackParamList>>();
  const definition = getGameDefinition(gameId);
  const s = useMemo(() => makeStyles(theme), [theme]);
  const {
    dialogVisible,
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
      startGame(navigation, gameId, difficulty);
    });
  }, [gameId, navigation, requestStart]);

  const levels = definition.difficulties.map((key) => ({
    key,
    label: definition.content.difficultyLabels[key],
    description: definition.content.difficultyDescriptions[key],
  }));

  return (
    <>
      <ScrollView contentContainerStyle={s.scroll}>
        <View style={s.header}>
          <Text style={s.tagline}>{definition.tagline}</Text>
        </View>

        <Text style={s.sectionLabel}>{strings.puzzle.chooseDifficulty}</Text>

        {levels.map((level, index) => (
          <React.Fragment key={level.key}>
            <TouchableRipple
              style={s.diffRow}
              onPress={() => handleSelectDifficulty(level.key)}
            >
              <View style={s.diffRowContent}>
                <View style={[s.diffMarker, { backgroundColor: getDifficultyColor(theme, level.key) }]} />
                <View style={s.diffBody}>
                  <Text style={s.diffLabel}>{level.label}</Text>
                  <Text style={s.diffDesc}>{level.description}</Text>
                </View>
                <Text style={s.diffAction}>{strings.common.play}</Text>
              </View>
            </TouchableRipple>
            {index < levels.length - 1 ? <Divider /> : null}
          </React.Fragment>
        ))}
      </ScrollView>

      <ActivePuzzleReplaceDialog
        visible={dialogVisible}
        onContinue={handleContinue}
        onStartNew={handleGiveUpAndStartNew}
        onDismiss={dismissDialog}
      />
    </>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  scroll: { padding: 20, gap: 0 },
  header: {
    gap: 6,
  },
  tagline: {
    fontSize: 15,
    lineHeight: 22,
    color: theme.textSecondary,
  },
  sectionLabel: {
    marginTop: 18,
    marginBottom: 4,
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 0.8,
    textTransform: 'uppercase',
    color: theme.textMuted,
  },
  diffRow: {
    paddingVertical: 18,
  },
  diffRowContent: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
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
    color: theme.primaryLight,
  },
});
