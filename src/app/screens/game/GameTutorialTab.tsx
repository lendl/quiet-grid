import React, { useMemo } from 'react';
import type { ComponentType } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useNavigation, type RouteProp } from '@react-navigation/native';
import type { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import type { StackNavigationProp, StackScreenProps } from '@react-navigation/stack';
import GamePageShell from '../../components/GamePageShell';
import { useLanguage } from '../../context/LanguageContext';
import { useTheme } from '../../context/ThemeContext';
import type { GameTabParamList, RootStackParamList } from '../../navigation/types';
import { getGameDefinition } from '../../shell/games/gameRegistry';
import type { Theme } from '../../theme';

type Props = BottomTabScreenProps<GameTabParamList, 'Tutorial'>;
type TutorialScreenProps = StackScreenProps<RootStackParamList, 'Tutorial'>;

export default function GameTutorialTab({ route }: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const rootNavigation = useNavigation<StackNavigationProp<RootStackParamList>>();
  const definition = getGameDefinition(route.params.gameId);
  const s = useMemo(() => makeStyles(theme), [theme]);

  const tutorialRoute = useMemo<RouteProp<RootStackParamList, 'Tutorial'>>(() => ({
    key: `Tutorial-${route.params.gameId}`,
    name: 'Tutorial',
    params: {
      gameId: route.params.gameId,
      entry: 'howToPlay',
    },
  }), [route.params.gameId]);

  const tutorialNavigation = useMemo<StackNavigationProp<RootStackParamList, 'Tutorial'>>(() => {
    return {
      ...rootNavigation,
      goBack: () => {
        rootNavigation.navigate('Game', {
          gameId: route.params.gameId,
        });
      },
      replace: (...args: [keyof RootStackParamList, RootStackParamList[keyof RootStackParamList]?]) => {
        const [name, params] = args;

        if (name === 'Game' && params) {
          rootNavigation.navigate('Game', params as RootStackParamList['Game']);
          return;
        }

        throw new Error('Tutorial replace only supports the Game route.');
      },
    } as StackNavigationProp<RootStackParamList, 'Tutorial'>;
  }, [rootNavigation, route.params.gameId]);

  if (!definition.screens.tutorial) {
    return (
      <GamePageShell
        activeTab="Games"
        headerMode="brand"
        contentTransitionDirection="forward"
        gameNav={{
          context: 'tabs',
          activeTab: 'Tutorial',
          gameId: route.params.gameId,
        }}
      >
        <View style={s.content}>
          <Text style={s.title}>{strings.tutorialHost.unavailableTitle}</Text>
          <Text style={s.body}>{strings.tutorialHost.unavailableBody}</Text>
        </View>
      </GamePageShell>
    );
  }
  const Screen = definition.screens.tutorial as ComponentType<TutorialScreenProps>;

  return (
    <GamePageShell
      activeTab="Games"
      headerMode="brand"
      contentTransitionDirection="forward"
      gameNav={{
        context: 'tabs',
        activeTab: 'Tutorial',
        gameId: route.params.gameId,
      }}
    >
      <Screen
        navigation={tutorialNavigation}
        route={tutorialRoute}
      />
    </GamePageShell>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
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
