import React, { useMemo } from 'react';
import Ionicons from '@react-native-vector-icons/ionicons';
import { useNavigation, type NavigationProp } from '@react-navigation/native';
import { StyleSheet, View } from 'react-native';
import { BottomNavigation } from 'react-native-paper';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import type { MainTabParamList, RootStackParamList } from '../navigation/types';
import type { Theme } from '../theme';
import { withAlpha } from '../utils/color';

type GlobalTabName = keyof MainTabParamList;

type Props = {
  activeTab?: GlobalTabName;
};

export default function GlobalBottomNav({ activeTab }: Props) {
  const navigation = useNavigation<NavigationProp<RootStackParamList>>();
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const insets = useSafeAreaInsets();
  const s = useMemo(() => makeStyles(theme), [theme]);

  const routes = useMemo(() => ([
    {
      key: 'Games' as GlobalTabName,
      title: strings.tabs.games,
      focusedIcon: ({ color, size }: { color: string; size: number }) => (
        <Ionicons name="grid" size={size} color={color} />
      ),
      unfocusedIcon: ({ color, size }: { color: string; size: number }) => (
        <Ionicons name="grid-outline" size={size} color={color} />
      ),
    },
    {
      key: 'Stats' as GlobalTabName,
      title: strings.tabs.stats,
      focusedIcon: ({ color, size }: { color: string; size: number }) => (
        <Ionicons name="stats-chart" size={size} color={color} />
      ),
      unfocusedIcon: ({ color, size }: { color: string; size: number }) => (
        <Ionicons name="stats-chart-outline" size={size} color={color} />
      ),
    },
    {
      key: 'Settings' as GlobalTabName,
      title: strings.tabs.settings,
      focusedIcon: ({ color, size }: { color: string; size: number }) => (
        <Ionicons name="settings" size={size} color={color} />
      ),
      unfocusedIcon: ({ color, size }: { color: string; size: number }) => (
        <Ionicons name="settings-outline" size={size} color={color} />
      ),
    },
    {
      key: 'Support' as GlobalTabName,
      title: strings.tabs.support,
      focusedIcon: ({ color, size }: { color: string; size: number }) => (
        <Ionicons name="help-circle" size={size} color={color} />
      ),
      unfocusedIcon: ({ color, size }: { color: string; size: number }) => (
        <Ionicons name="help-circle-outline" size={size} color={color} />
      ),
    },
  ]), [strings]);

  const activeIndex = Math.max(routes.findIndex((r) => r.key === activeTab), 0);

  return (
    <View style={s.container}>
      <BottomNavigation.Bar
        navigationState={{ index: activeIndex, routes }}
        onTabPress={({ route }) => {
          navigation.navigate('MainTabs', { screen: route.key as GlobalTabName });
        }}
        safeAreaInsets={{ bottom: insets.bottom }}
        inactiveColor={theme.textSecondary}
        theme={{
          colors: {
            secondaryContainer: theme.primary,
            onSecondaryContainer: theme.onPrimary,
            secondary: theme.primary,
          },
        }}
        style={s.bar}
      />
    </View>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  container: {
    borderTopWidth: 1,
    borderTopColor: withAlpha(theme.border, 0.48),
  },
  bar: {
    backgroundColor: withAlpha(theme.surfaceElevated, 0.98),
  },
});
