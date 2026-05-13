import React, { useMemo } from 'react';
import { StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import SettingsScreen from '../screens/SettingsScreen';
import PuzzleGameTab from '../screens/puzzle/PuzzleGameTab';
import PuzzleRulesTab from '../screens/puzzle/PuzzleRulesTab';
import PuzzleStatsTab from '../screens/puzzle/PuzzleStatsTab';
import PuzzleTutorialEntryTab from '../screens/puzzle/PuzzleTutorialEntryTab';
import type { PuzzleTypeId } from '../shell/types';
import type { Theme } from '../theme';
import type { PuzzleTabParamList } from './types';

const Tab = createBottomTabNavigator<PuzzleTabParamList>();
const BASE_TAB_BAR_HEIGHT = 64;
const BASE_TAB_BAR_TOP_PADDING = 6;
const BASE_TAB_BAR_BOTTOM_PADDING = 8;

function getTabIcon(routeName: keyof PuzzleTabParamList, focused: boolean): React.ComponentProps<typeof Ionicons>['name'] {
  switch (routeName) {
    case 'Game':
      return focused ? 'game-controller' : 'game-controller-outline';
    case 'Stats':
      return focused ? 'stats-chart' : 'stats-chart-outline';
    case 'Rules':
      return focused ? 'book' : 'book-outline';
    case 'Tutorial':
      return focused ? 'school' : 'school-outline';
    case 'Settings':
      return focused ? 'settings' : 'settings-outline';
    default:
      throw new Error('Unhandled puzzle tab route.');
  }
}

const makeStyles = (theme: Theme, bottomInset: number) => StyleSheet.create({
  tabBar: {
    height: BASE_TAB_BAR_HEIGHT + bottomInset,
    paddingTop: BASE_TAB_BAR_TOP_PADDING,
    paddingBottom: BASE_TAB_BAR_BOTTOM_PADDING + bottomInset,
    backgroundColor: theme.surfaceElevated,
    borderTopColor: theme.border,
  },
});

type Props = {
  puzzleTypeId: PuzzleTypeId;
  supportsTutorial: boolean;
};

export default function PuzzleTabs({ puzzleTypeId, supportsTutorial }: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const insets = useSafeAreaInsets();
  const s = useMemo(() => makeStyles(theme, insets.bottom), [theme, insets.bottom]);

  return (
    <Tab.Navigator
      initialRouteName="Game"
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarStyle: s.tabBar,
        tabBarActiveTintColor: theme.primaryLight,
        tabBarInactiveTintColor: theme.textSecondary,
        tabBarLabelStyle: {
          fontSize: 12,
          fontWeight: '700',
        },
        tabBarIcon: ({ focused, color }) => (
          <Ionicons
            name={getTabIcon(route.name, focused)}
            size={20}
            color={color}
          />
        ),
      })}
    >
      <Tab.Screen
        name="Game"
        component={PuzzleGameTab}
        initialParams={{ puzzleTypeId }}
        options={{ tabBarLabel: strings.common.game }}
      />
      <Tab.Screen
        name="Stats"
        component={PuzzleStatsTab}
        initialParams={{ puzzleTypeId }}
        options={{ tabBarLabel: strings.common.stats }}
      />
      <Tab.Screen
        name="Rules"
        component={PuzzleRulesTab}
        initialParams={{ puzzleTypeId }}
        options={{ tabBarLabel: strings.common.rules }}
      />
      {supportsTutorial ? (
        <Tab.Screen
          name="Tutorial"
          component={PuzzleTutorialEntryTab}
          initialParams={{ puzzleTypeId }}
          options={{ tabBarLabel: strings.common.tutorial }}
        />
      ) : null}
      <Tab.Screen
        name="Settings"
        component={SettingsScreen}
        initialParams={{ puzzleTypeId }}
        options={{ tabBarLabel: strings.common.settings }}
      />
    </Tab.Navigator>
  );
}
