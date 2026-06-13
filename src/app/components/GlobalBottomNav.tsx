import React, { useMemo } from 'react';
import Ionicons from '@react-native-vector-icons/ionicons';
import { useNavigation, type NavigationProp } from '@react-navigation/native';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
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

type NavItem = {
  key: GlobalTabName;
  label: string;
  icon: React.ComponentProps<typeof Ionicons>['name'];
  activeIcon: React.ComponentProps<typeof Ionicons>['name'];
};

export default function GlobalBottomNav({ activeTab }: Props) {
  const navigation = useNavigation<NavigationProp<RootStackParamList>>();
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const insets = useSafeAreaInsets();
  const s = useMemo(() => makeStyles(theme, insets.bottom), [insets.bottom, theme]);

  const items = useMemo<NavItem[]>(() => ([
    { key: 'Games', label: strings.tabs.games, icon: 'grid-outline', activeIcon: 'grid' },
    { key: 'Stats', label: strings.tabs.stats, icon: 'stats-chart-outline', activeIcon: 'stats-chart' },
    { key: 'Settings', label: strings.tabs.settings, icon: 'settings-outline', activeIcon: 'settings' },
    { key: 'Support', label: strings.tabs.support, icon: 'help-circle-outline', activeIcon: 'help-circle' },
  ]), [strings]);

  return (
    <View style={s.container}>
      <View style={s.row}>
        {items.map((item) => {
          const focused = item.key === activeTab;

          return (
            <TouchableOpacity
              key={item.key}
              accessibilityRole="button"
              accessibilityLabel={item.label}
              onPress={() => navigation.navigate('MainTabs', { screen: item.key })}
              style={s.item}
              activeOpacity={0.82}
            >
              <View style={[s.iconBubble, focused ? s.iconBubbleActive : null]}>
                <Ionicons
                  name={focused ? item.activeIcon : item.icon}
                  size={20}
                  color={focused ? theme.onPrimary : theme.textSecondary}
                />
              </View>
              <Text style={[s.label, focused ? s.labelActive : null]}>{item.label}</Text>
            </TouchableOpacity>
          );
        })}
      </View>
    </View>
  );
}

const makeStyles = (theme: Theme, bottomInset: number) => StyleSheet.create({
  container: {
    borderTopWidth: 1,
    borderTopColor: withAlpha(theme.border, 0.48),
    backgroundColor: withAlpha(theme.surfaceElevated, 0.98),
    paddingHorizontal: 12,
    paddingTop: 7,
    paddingBottom: 7 + bottomInset,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'stretch',
    gap: 8,
  },
  item: {
    flex: 1,
    minHeight: 48,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 2,
    paddingHorizontal: 4,
  },
  iconBubble: {
    minWidth: 50,
    height: 30,
    borderRadius: 999,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 14,
  },
  iconBubbleActive: {
    backgroundColor: theme.primary,
  },
  label: {
    fontSize: 11,
    fontWeight: '700',
    color: theme.textSecondary,
  },
  labelActive: {
    color: theme.primary,
  },
});
