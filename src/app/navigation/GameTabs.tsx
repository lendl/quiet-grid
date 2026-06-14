import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import GamePlayTab from '../screens/game/GamePlayTab';
import GameRulesTab from '../screens/game/GameRulesTab';
import GameStatsTab from '../screens/game/GameStatsTab';
import type { GameId } from '../../games/shared/types';
import type { GameTabParamList, TransitionDirection } from './types';

const Tab = createBottomTabNavigator<GameTabParamList>();

type Props = {
  gameId: GameId;
  initialTab?: keyof GameTabParamList;
  initialDirection?: TransitionDirection;
};

export default function GameTabs({ gameId, initialTab = 'Play', initialDirection = 'none' }: Props) {
  return (
    <Tab.Navigator
      initialRouteName={initialTab}
      tabBar={() => null}
      screenOptions={{
        headerShown: false,
      }}
    >
      <Tab.Screen
        name="Play"
        component={GamePlayTab}
        initialParams={{ gameId, transitionDirection: initialTab === 'Play' ? initialDirection : 'none' }}
      />
      <Tab.Screen
        name="Rules"
        component={GameRulesTab}
        initialParams={{ gameId, transitionDirection: initialTab === 'Rules' ? initialDirection : 'none' }}
      />
      <Tab.Screen
        name="Stats"
        component={GameStatsTab}
        initialParams={{ gameId, transitionDirection: initialTab === 'Stats' ? initialDirection : 'none' }}
      />
    </Tab.Navigator>
  );
}
