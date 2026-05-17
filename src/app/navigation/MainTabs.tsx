import React, { useMemo } from 'react';
import {
  createMaterialTopTabNavigator,
} from '@react-navigation/material-top-tabs';
import { StyleSheet, View } from 'react-native';
import { useTheme } from '../context/ThemeContext';
import GamesScreen from '../screens/GamesScreen';
import SettingsScreen from '../screens/SettingsScreen';
import StatsScreen from '../screens/StatsScreen';
import SupportScreen from '../screens/SupportScreen';
import type { Theme } from '../theme';
import type { MainTabParamList } from './types';

const Tab = createMaterialTopTabNavigator<MainTabParamList>();

export default function MainTabs() {
  const { theme } = useTheme();
  const s = useMemo(() => makeStyles(theme), [theme]);

  return (
    <View style={s.root}>
      <Tab.Navigator
        initialRouteName="Games"
        tabBar={() => null}
        screenOptions={{
          animationEnabled: false,
          swipeEnabled: false,
        }}
      >
        <Tab.Screen name="Games" component={GamesScreen} />
        <Tab.Screen name="Stats" component={StatsScreen} />
        <Tab.Screen name="Settings" component={SettingsScreen} />
        <Tab.Screen name="Support" component={SupportScreen} />
      </Tab.Navigator>
    </View>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: theme.background,
  },
});
