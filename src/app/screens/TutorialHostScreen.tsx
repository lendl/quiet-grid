import React from 'react';
import type { ComponentType } from 'react';
import { Text, View, StyleSheet } from 'react-native';
import type { StackScreenProps } from '@react-navigation/stack';
import AppScreen from '../components/AppScreen';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { getPuzzleDefinition } from '../shell/games/gameRegistry';
import type { RootStackParamList } from '../navigation/types';
import type { Theme } from '../theme';

type Props = StackScreenProps<RootStackParamList, 'Tutorial'>;

export default function TutorialHostScreen({ navigation, route }: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const definition = getPuzzleDefinition(route.params.puzzleTypeId);

  const tutorialNavigation = {
    ...navigation,
    goBack: () => {
      navigation.navigate('Puzzle', { puzzleTypeId: route.params.puzzleTypeId });
    },
  };

  if (!definition.screens.tutorial) {
    const s = makeStyles(theme);
    return (
      <AppScreen contentStyle={s.container}>
        <View style={s.content}>
          <Text style={s.title}>{strings.tutorialHost.unavailableTitle}</Text>
          <Text style={s.body}>{strings.tutorialHost.unavailableBody}</Text>
        </View>
      </AppScreen>
    );
  }

  const screenProps = { navigation: tutorialNavigation, route };
  const Screen = definition.screens.tutorial as ComponentType<typeof screenProps>;

  return <Screen {...screenProps} />;
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.background,
  },
  content: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 28,
    gap: 12,
  },
  title: {
    fontSize: 22,
    fontWeight: '800',
    color: theme.text,
    textAlign: 'center',
  },
  body: {
    fontSize: 15,
    lineHeight: 22,
    color: theme.textSecondary,
    textAlign: 'center',
  },
});
