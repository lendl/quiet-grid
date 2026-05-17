import React, { useCallback, useMemo, useState } from 'react';
import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, TouchableOpacity, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import AppBrand from './AppBrand';
import AppDialog from './AppDialog';
import { useActivePuzzleNav } from '../hooks/useActivePuzzleNav';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { useMenuThemeCycle } from '../hooks/useMenuThemeCycle';
import type { Theme } from '../theme';
import { openRepo } from '../utils/supportLinks';

type BrandProps = {
  mode: 'brand';
};

type BackProps = {
  mode: 'back';
  onBack: () => void;
};

type Props = BrandProps | BackProps;

export default function AppTopBar(props: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const insets = useSafeAreaInsets();
  const { activePuzzle, continuePuzzle } = useActivePuzzleNav();
  const { iconName: themeIconName, cycleTheme } = useMenuThemeCycle();
  const s = useMemo(() => makeStyles(theme), [theme]);
  const [repoErrorVisible, setRepoErrorVisible] = useState(false);

  const handleOpenRepo = useCallback(async () => {
    const opened = await openRepo();
    if (!opened) {
      setRepoErrorVisible(true);
    }
  }, []);

  return (
    <>
      <View style={s.container}>
        <View style={[s.inner, { paddingTop: insets.top + 12 }]}>
          {props.mode === 'back' ? (
            <TouchableOpacity
              accessibilityRole="button"
            accessibilityLabel={strings.common.goBack}
            onPress={props.onBack}
            style={s.backButton}
            activeOpacity={0.82}
          >
            <Ionicons name="arrow-back" size={20} color={theme.text} />
          </TouchableOpacity>
        ) : (
          <>
            <View style={s.brandWrap}>
              <AppBrand compact />
            </View>
            <View style={s.actions}>
              {activePuzzle ? (
                <TouchableOpacity
                  accessibilityRole="button"
                  accessibilityLabel={strings.common.continuePuzzle}
                  onPress={() => {
                    void continuePuzzle();
                  }}
                  style={[s.iconButton, s.playButton]}
                  activeOpacity={0.82}
                >
                  <Ionicons name="play" size={18} color={theme.onPrimary} />
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
                <Ionicons name="logo-github" size={20} color={theme.text} />
              </TouchableOpacity>
              <TouchableOpacity
                accessibilityRole="button"
                accessibilityLabel={strings.home.changeTheme}
                onPress={cycleTheme}
                style={s.iconButton}
                activeOpacity={0.82}
              >
                <Ionicons name={themeIconName} size={20} color={theme.text} />
              </TouchableOpacity>
            </View>
          </>
        )}
      </View>
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
    </>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  container: {
    paddingHorizontal: 16,
    paddingBottom: 10,
  },
  inner: {
    minHeight: 56,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
  },
  brandWrap: {
    flex: 1,
    minWidth: 0,
  },
  actions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  iconButton: {
    width: 32,
    height: 32,
    borderRadius: 999,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: theme.surfaceElevated,
  },
  backButton: {
    width: 36,
    height: 36,
    borderRadius: 999,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: theme.surfaceElevated,
  },
  playButton: {
    backgroundColor: theme.primary,
  },
});
