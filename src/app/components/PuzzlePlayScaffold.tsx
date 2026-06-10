import React from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import type { DialogConfig } from './AppDialog';
import AppDialog from './AppDialog';
import AppScreen from './AppScreen';
import TopBackgroundEffect from './TopBackgroundEffect';
import { useTheme } from '../context/ThemeContext';
import type { Theme } from '../theme';

interface PuzzlePlayScaffoldProps {
  loading: boolean;
  loadingLabel?: string;
  dialog?: DialogConfig | null;
  onDismissDialog: () => void;
  topSlot?: React.ReactNode;
  header?: React.ReactNode;
  main: React.ReactNode;
  footer: React.ReactNode;
  bottomSlot?: React.ReactNode;
}

export default function PuzzlePlayScaffold({
  loading,
  loadingLabel,
  dialog = null,
  onDismissDialog,
  topSlot = null,
  header = null,
  main,
  footer,
  bottomSlot = null,
}: PuzzlePlayScaffoldProps) {
  const { theme } = useTheme();
  const insets = useSafeAreaInsets();
  const s = makeStyles(theme, insets.bottom);

  return (
    <AppScreen edges={['left', 'right']} overlay={false} includeBottomInset={false} contentStyle={s.container}>
      <TopBackgroundEffect topOffset={-insets.top} />
      {topSlot}
      {loading ? (
        <View style={s.loadingRegion}>
        <View style={s.loadingContainer}>
          <ActivityIndicator size="large" color={theme.primary} />
          {loadingLabel ? <Text style={s.loadingText}>{loadingLabel}</Text> : null}
        </View>
        </View>
        ) : (
        <>
          {header ? <View style={s.headerRegion}>{header}</View> : null}
          <View style={s.mainRegion}>{main}</View>
          <View style={s.footerRegion}>{footer}</View>
        </>
        )}
      {bottomSlot}
      {dialog ? (
        <AppDialog
          visible
          title={dialog.title}
          message={dialog.message}
          buttons={dialog.buttons}
          onDismiss={onDismissDialog}
        />
      ) : null}
    </AppScreen>
  );
}

const makeStyles = (theme: Theme, bottomInset: number) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.background,
  },
  loadingContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  loadingRegion: {
    flex: 1,
  },
  loadingText: {
    marginTop: 12,
    color: theme.textSecondary,
    fontSize: 15,
  },
  headerRegion: {
    paddingTop: 6,
  },
  mainRegion: {
    flex: 1,
  },
  footerRegion: {
    minHeight: 102 + Math.max(10, bottomInset + 4),
    flexShrink: 0,
    paddingHorizontal: 10,
    paddingTop: 8,
    paddingBottom: Math.max(10, bottomInset + 4),
    overflow: 'hidden',
  },
});
