import React from 'react';
import type { ComponentType } from 'react';
import { Text, View, StyleSheet } from 'react-native';
import type { StackScreenProps } from '@react-navigation/stack';
import GamePageShell from '../components/GamePageShell';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { getGameDefinition } from '../shell/games/gameRegistry';
import type { RootStackParamList } from '../navigation/types';

type Props = StackScreenProps<RootStackParamList, 'Tutorial'>;

export default function TutorialHostScreen({ navigation, route }: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const definition = getGameDefinition(route.params.gameId);

  const tutorialNavigation = {
    ...navigation,
    goBack: () => {
      navigation.navigate('Game', { gameId: route.params.gameId });
    },
  };

  if (!definition.screens.tutorial) {
    return (
      <GamePageShell headerMode="brand">
        <View style={styles.content}>
          <Text style={[styles.title, { color: theme.text }]}>{strings.tutorialHost.unavailableTitle}</Text>
          <Text style={[styles.body, { color: theme.textSecondary }]}>{strings.tutorialHost.unavailableBody}</Text>
        </View>
      </GamePageShell>
    );
  }

  const screenProps = { navigation: tutorialNavigation, route };
  const Screen = definition.screens.tutorial as ComponentType<typeof screenProps>;

  return (
    <GamePageShell
      headerMode="back"
      contentTransitionDirection="forward"
      gameNav={{
        context: 'root',
        activeTab: 'Tutorial',
        gameId: route.params.gameId,
        tutorialEntry: route.params.entry,
      }}
    >
      <Screen {...screenProps} />
    </GamePageShell>
  );
}

const styles = StyleSheet.create({
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
    textAlign: 'center',
  },
  body: {
    fontSize: 15,
    lineHeight: 22,
    textAlign: 'center',
  },
});
