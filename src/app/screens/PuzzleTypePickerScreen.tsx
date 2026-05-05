// src/app/screens/PuzzleTypePickerScreen.tsx
import React, { useMemo } from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import type { StackScreenProps } from '@react-navigation/stack';
import AppScreen from '../components/AppScreen';
import GridHomeIcon from '../components/GridHomeIcon';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { returnToHome } from '../navigation/returnToHome';
import type { RootStackParamList } from '../navigation/types';
import { puzzleRegistry } from '../shell/games/gameRegistry';
import { shouldAutoShowTutorial } from '../utils/settingsStorage';
import type { Theme } from '../theme';

type Props = StackScreenProps<RootStackParamList, 'PuzzleTypePicker'>;

export default function PuzzleTypePickerScreen({ navigation }: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const s = useMemo(() => makeStyles(theme), [theme]);

  async function handleSelectPuzzleType(puzzleTypeId: typeof puzzleRegistry[number]['id']) {
    const puzzleType = puzzleRegistry.find((definition) => definition.id === puzzleTypeId);

    if (puzzleType?.supports.tutorial && await shouldAutoShowTutorial(puzzleTypeId)) {
      navigation.navigate('Tutorial', { puzzleTypeId, entry: 'startup' });
      return;
    }

    navigation.navigate('Puzzle', { puzzleTypeId });
  }

  return (
    <AppScreen contentStyle={s.container}>
      <View style={s.scroll}>
        <TouchableOpacity
          style={s.backButton}
          onPress={() => returnToHome(navigation)}
            accessibilityLabel={strings.common.goHome}
          activeOpacity={0.8}
        >
          <GridHomeIcon />
        </TouchableOpacity>

        <Text style={s.heading}>{strings.puzzlePicker.heading}</Text>

        {puzzleRegistry.map((puzzleType) => (
          <TouchableOpacity
            key={puzzleType.id}
            style={s.card}
            onPress={() => {
              void handleSelectPuzzleType(puzzleType.id);
            }}
            activeOpacity={0.78}
          >
            <Text style={s.cardEmoji}>{puzzleType.emoji}</Text>
            <View style={s.cardBody}>
              <Text style={s.cardTitle}>{puzzleType.title}</Text>
              <Text style={s.cardTagline}>{puzzleType.tagline}</Text>
            </View>
          </TouchableOpacity>
        ))}
      </View>
    </AppScreen>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  container: { flex: 1, backgroundColor: theme.background },
  scroll: { padding: 20, gap: 16 },
  backButton: {
    alignSelf: 'flex-start',
    minWidth: 44,
    minHeight: 36,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 6,
  },
  heading: {
    fontSize: 26,
    fontWeight: '800',
    color: theme.text,
    marginBottom: 4,
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
    padding: 20,
    backgroundColor: theme.surface,
    borderRadius: 16,
  },
  cardEmoji: { fontSize: 32 },
  cardBody: { flex: 1 },
  cardTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: theme.text,
  },
  cardTagline: {
    marginTop: 4,
    fontSize: 13,
    color: theme.textSecondary,
    lineHeight: 19,
  },
});
