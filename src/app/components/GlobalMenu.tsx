import React, { useCallback, useMemo, useState } from 'react';
import Ionicons from '@react-native-vector-icons/ionicons';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useNavigation, type NavigationProp } from '@react-navigation/native';
import AppDialog from './AppDialog';
import AppBrand from './AppBrand';
import TopBackgroundEffect from './TopBackgroundEffect';
import { useActivePuzzleNav } from '../hooks/useActivePuzzleNav';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { useMenuThemeCycle } from '../hooks/useMenuThemeCycle';
import type { MainTabParamList, RootStackParamList } from '../navigation/types';
import type { Theme } from '../theme';
import { withAlpha } from '../utils/color';
import { openRepo } from '../utils/supportLinks';

type RootNavigation = NavigationProp<RootStackParamList>;
type GlobalTabName = keyof MainTabParamList;

type Props = {
  activeTab?: GlobalTabName;
  onClose?: () => void;
  onContinue?: () => void;
  topInset?: number;
};

type MenuLink = {
  key: GlobalTabName;
  label: string;
};

export default function GlobalMenu({
  activeTab,
  onClose,
  onContinue,
  topInset = 0,
}: Props) {
  const navigation = useNavigation<RootNavigation>();
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const { iconName: themeIconName, cycleTheme } = useMenuThemeCycle();
  const { activePuzzle, continuePuzzle } = useActivePuzzleNav();
  const [repoErrorVisible, setRepoErrorVisible] = useState(false);
  const s = useMemo(() => makeStyles(theme, topInset), [theme, topInset]);

  const links = useMemo<MenuLink[]>(() => ([
    { key: 'Games', label: strings.tabs.games },
    { key: 'Stats', label: strings.tabs.stats },
    { key: 'Settings', label: strings.tabs.settings },
    { key: 'Support', label: strings.tabs.support },
  ]), [strings]);

  const handleNavigate = useCallback((tab: GlobalTabName) => {
    navigation.navigate('MainTabs', { screen: tab });
    onClose?.();
  }, [navigation, onClose]);

  const handleOpenRepo = useCallback(async () => {
    const opened = await openRepo();
    if (!opened) {
      setRepoErrorVisible(true);
    }
  }, []);

  const handleContinue = useCallback(async () => {
    if (onContinue) {
      onContinue();
      onClose?.();
      return;
    }

    await continuePuzzle();
    onClose?.();
  }, [continuePuzzle, onClose, onContinue]);

  return (
    <View style={s.container}>
      <TopBackgroundEffect />
      <View style={s.topRow}>
        <View style={s.brandRow}>
          <AppBrand />
        </View>

        <View style={s.actions}>
          {activePuzzle ? (
            <TouchableOpacity
              accessibilityRole="button"
              accessibilityLabel={strings.common.continuePuzzle}
              onPress={() => {
                void handleContinue();
              }}
              style={s.playButton}
              activeOpacity={0.82}
            >
              <Ionicons name="play" size={16} color={theme.onPrimary} />
            </TouchableOpacity>
          ) : null}
          <TouchableOpacity
            accessibilityRole="button"
            accessibilityLabel={strings.home.openRepo}
            onPress={() => {
              void handleOpenRepo();
            }}
            style={s.iconButton}
            activeOpacity={0.82}
          >
            <Ionicons name="logo-github" size={24} color={theme.text} />
          </TouchableOpacity>

          <TouchableOpacity
            accessibilityRole="button"
            accessibilityLabel={strings.home.changeTheme}
            onPress={cycleTheme}
            style={s.iconButton}
            activeOpacity={0.82}
          >
            <Ionicons name={themeIconName} size={24} color={theme.text} />
          </TouchableOpacity>
        </View>
      </View>

      <View style={s.linksRow}>
        {links.map((link) => {
          const active = activeTab === link.key;

          return (
            <TouchableOpacity
              key={link.key}
              accessibilityRole="button"
              accessibilityLabel={link.label}
              onPress={() => handleNavigate(link.key)}
              style={[s.linkButton, active ? s.linkButtonActive : null]}
              activeOpacity={0.82}
            >
              <Text style={[s.linkText, active ? s.linkTextActive : null]}>{link.label}</Text>
            </TouchableOpacity>
          );
        })}
      </View>

      <AppDialog
        visible={repoErrorVisible}
        title={strings.home.repoErrorTitle}
        message={strings.home.repoErrorMessage}
        buttons={[
          {
            text: 'OK',
            onPress: () => setRepoErrorVisible(false),
          },
        ]}
        onDismiss={() => setRepoErrorVisible(false)}
      />
    </View>
  );
}

const makeStyles = (theme: Theme, topInset: number) => StyleSheet.create({
  container: {
    gap: 0,
    backgroundColor: theme.background,
    overflow: 'hidden',
  },
  topRow: {
    paddingTop: topInset + 12,
    paddingBottom: 8,
    paddingHorizontal: 16,
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: 12,
  },
  brandRow: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  actions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  iconButton: {
    width: 28,
    height: 28,
    alignItems: 'center',
    justifyContent: 'center',
  },
  linksRow: {
    alignSelf: 'stretch',
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: 0,
    borderBottomWidth: 1,
    borderBottomColor: withAlpha(theme.border, 0.42),
  },
  linkButton: {
    flex: 1,
    minHeight: 42,
    paddingHorizontal: 4,
    alignItems: 'center',
    justifyContent: 'center',
    borderBottomWidth: 2,
    borderBottomColor: 'transparent',
  },
  linkButtonActive: {
    borderBottomColor: theme.primary,
  },
  linkText: {
    fontSize: 13,
    fontWeight: '700',
    color: theme.textSecondary,
  },
  linkTextActive: {
    color: theme.primary,
  },
  playButton: {
    width: 38,
    height: 38,
    borderRadius: 999,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: theme.primary,
    borderWidth: 1,
    borderColor: withAlpha(theme.primaryLight, 0.84),
  },
});
