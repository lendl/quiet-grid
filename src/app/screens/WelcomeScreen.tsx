// src/app/screens/WelcomeScreen.tsx
import React, { useMemo, useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import type { StackScreenProps } from '@react-navigation/stack';
import AppScreen from '../components/AppScreen';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import type { RootStackParamList } from '../navigation/types';
import { getLocalizedPuzzleNameList } from '../shell/games/gameNameList';
import { markWelcomeSeen } from '../utils/settingsStorage';
import type { Theme } from '../theme';

type Props = StackScreenProps<RootStackParamList, 'Welcome'>;

interface Slide {
  emoji: string;
  title: string;
  body: string;
}

export default function WelcomeScreen({ navigation }: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const s = useMemo(() => makeStyles(theme), [theme]);
  const puzzleNames = getLocalizedPuzzleNameList();
  const slides = useMemo<Slide[]>(() => ([
    { emoji: '🧩', ...strings.welcome.slides[0] },
    {
      emoji: '🎮',
      ...strings.welcome.slides[1],
      body: strings.welcome.slides[1].body.replace('{games}', puzzleNames),
    },
    { emoji: '🔒', ...strings.welcome.slides[2] },
  ]), [puzzleNames, strings]);
  const [index, setIndex] = useState(0);
  const slide = slides[index];
  const isLast = index === slides.length - 1;

  const handleNext = async () => {
    if (!isLast) {
      setIndex(index + 1);
      return;
    }
    await markWelcomeSeen();
    navigation.replace('Home');
  };

  return (
    <AppScreen contentStyle={s.container}>
      <View style={s.content}>
        <Text style={s.emoji}>{slide.emoji}</Text>
        <Text style={s.title}>{slide.title}</Text>
        <Text style={s.body}>{slide.body}</Text>

        <View style={s.dots}>
          {slides.map((_, i) => (
            <View key={i} style={[s.dot, i === index && s.dotActive]} />
          ))}
        </View>

        <TouchableOpacity style={s.btn} onPress={() => { void handleNext(); }} activeOpacity={0.8}>
          <Text style={s.btnText}>{isLast ? strings.common.getStarted : strings.common.next}</Text>
        </TouchableOpacity>
      </View>
    </AppScreen>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  container: { flex: 1, backgroundColor: theme.background },
  content: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 32,
    gap: 20,
  },
  emoji: { fontSize: 64 },
  title: {
    fontSize: 28,
    fontWeight: '800',
    color: theme.text,
    textAlign: 'center',
    lineHeight: 34,
  },
  body: {
    fontSize: 16,
    color: theme.textSecondary,
    textAlign: 'center',
    lineHeight: 24,
  },
  dots: { flexDirection: 'row', gap: 8, marginTop: 8 },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: theme.border,
  },
  dotActive: { backgroundColor: theme.primary, width: 20 },
  btn: {
    marginTop: 8,
    backgroundColor: theme.primary,
    paddingVertical: 16,
    paddingHorizontal: 48,
    borderRadius: 14,
    alignItems: 'center',
  },
  btnText: {
    color: theme.onPrimary,
    fontSize: 17,
    fontWeight: '700',
  },
});
