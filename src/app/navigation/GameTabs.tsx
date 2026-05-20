import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import PuzzleGameTab from '../screens/puzzle/PuzzleGameTab';
import PuzzleRulesTab from '../screens/puzzle/PuzzleRulesTab';
import PuzzleStatsTab from '../screens/puzzle/PuzzleStatsTab';
import PuzzleTutorialEntryTab from '../screens/puzzle/PuzzleTutorialEntryTab';
import type { PuzzleTypeId } from '../shell/types';
import type { PuzzleTabParamList, TransitionDirection } from './types';

const Tab = createBottomTabNavigator<PuzzleTabParamList>();

type Props = {
  puzzleTypeId: PuzzleTypeId;
  initialTab?: keyof PuzzleTabParamList;
  initialDirection?: TransitionDirection;
};

export default function PuzzleTabs({ puzzleTypeId, initialTab = 'Game', initialDirection = 'none' }: Props) {
  return (
    <Tab.Navigator
      initialRouteName={initialTab}
      tabBar={() => null}
      screenOptions={{
        headerShown: false,
      }}
    >
      <Tab.Screen
        name="Game"
        component={PuzzleGameTab}
        initialParams={{ puzzleTypeId, transitionDirection: initialTab === 'Game' ? initialDirection : 'none' }}
      />
      <Tab.Screen
        name="Rules"
        component={PuzzleRulesTab}
        initialParams={{ puzzleTypeId, transitionDirection: initialTab === 'Rules' ? initialDirection : 'none' }}
      />
      <Tab.Screen
        name="Tutorial"
        component={PuzzleTutorialEntryTab}
        initialParams={{ puzzleTypeId, transitionDirection: initialTab === 'Tutorial' ? initialDirection : 'none' }}
      />
      <Tab.Screen
        name="Stats"
        component={PuzzleStatsTab}
        initialParams={{ puzzleTypeId, transitionDirection: initialTab === 'Stats' ? initialDirection : 'none' }}
      />
    </Tab.Navigator>
  );
}
