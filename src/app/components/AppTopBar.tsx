import React, { useCallback, useMemo, useState } from 'react';
import Ionicons from '@react-native-vector-icons/ionicons';
import { StyleSheet, View } from 'react-native';
import { Appbar, TouchableRipple } from 'react-native-paper';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import AppBrand from './AppBrand';
import AppDialog from './AppDialog';
import { useActivePuzzleNav } from '../hooks/useActivePuzzleNav';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import { useMenuThemeCycle } from '../hooks/useMenuThemeCycle';
import type { Theme } from '../theme';
import { withAlpha } from '../utils/color';
import { openRepo } from '../utils/supportLinks';

type BrandProps = {
  mode: 'brand';
  gameName?: string;
};

type BackProps = {
  mode: 'back';
  onBack: () => void;
  rightSlot?: React.ReactNode;
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
      <Appbar.Header style={s.header} statusBarHeight={insets.top}>
        {props.mode === 'back' ? (
          <>
            <Appbar.BackAction
              onPress={props.onBack}
              accessibilityLabel={strings.common.goBack}
            />
            <View style={s.flex} />
            {props.rightSlot ? <View style={s.rightSlotWrap}>{props.rightSlot}</View> : null}
          </>
        ) : (
          <>
            <View style={s.brandWrap}>
              <AppBrand compact gameName={props.mode === 'brand' ? props.gameName : undefined} />
            </View>
            {activePuzzle ? (
              <TouchableRipple
                accessibilityRole="button"
                accessibilityLabel={strings.common.continuePuzzle}
                onPress={() => { void continuePuzzle(); }}
                style={s.playButton}
                rippleColor={withAlpha(theme.onPrimary, 0.2)}
              >
                <Ionicons name="play" size={18} color={theme.onPrimary} />
              </TouchableRipple>
            ) : null}
            <Appbar.Action
              accessibilityLabel={strings.home.openRepo}
              icon={({ size, color }) => (
                <Ionicons name="logo-github" size={size} color={color} />
              )}
              onPress={() => { void handleOpenRepo(); }}
            />
            <Appbar.Action
              accessibilityLabel={strings.home.changeTheme}
              icon={({ size, color }) => (
                <Ionicons
                  name={themeIconName as React.ComponentProps<typeof Ionicons>['name']}
                  size={size}
                  color={color}
                />
              )}
              onPress={cycleTheme}
            />
          </>
        )}
      </Appbar.Header>
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
  header: {
    backgroundColor: 'transparent',
    elevation: 0,
    paddingHorizontal: 4,
  },
  flex: { flex: 1 },
  brandWrap: {
    flex: 1,
    paddingLeft: 4,
    minWidth: 0,
  },
  rightSlotWrap: {
    paddingRight: 16,
  },
  playButton: {
    width: 36,
    height: 36,
    borderRadius: 999,
    backgroundColor: theme.primary,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 4,
    overflow: 'hidden',
  },
});
