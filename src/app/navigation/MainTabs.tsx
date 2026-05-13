import React, { useMemo } from 'react';
import { StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import GamesScreen from '../screens/GamesScreen';
import SettingsScreen from '../screens/SettingsScreen';
import StatsScreen from '../screens/StatsScreen';
import type { Theme } from '../theme';
import type { MainTabParamList } from './types';

const Tab = createBottomTabNavigator<MainTabParamList>();
const BASE_TAB_BAR_HEIGHT = 64;
const BASE_TAB_BAR_TOP_PADDING = 6;
const BASE_TAB_BAR_BOTTOM_PADDING = 8;

function getTabIcon(routeName: keyof MainTabParamList, focused: boolean): React.ComponentProps<typeof Ionicons>['name'] {
  switch (routeName) {
    case 'Games':
      return focused ? 'grid' : 'grid-outline';
    case 'Stats':
      return focused ? 'stats-chart' : 'stats-chart-outline';
    case 'Settings':
      return focused ? 'settings' : 'settings-outline';
    default:
      throw new Error('Unhandled tab route.');
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

export default function MainTabs() {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const insets = useSafeAreaInsets();
  const s = useMemo(() => makeStyles(theme, insets.bottom), [theme, insets.bottom]);

  return (
    <Tab.Navigator
      initialRouteName="Games"
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
        name="Games"
        component={GamesScreen}
        options={{ tabBarLabel: strings.tabs.games }}
      />
      <Tab.Screen
        name="Stats"
        component={StatsScreen}
        options={{ tabBarLabel: strings.tabs.stats }}
      />
      <Tab.Screen
        name="Settings"
        component={SettingsScreen}
        options={{ tabBarLabel: strings.tabs.settings }}
      />
    </Tab.Navigator>
  );
}
