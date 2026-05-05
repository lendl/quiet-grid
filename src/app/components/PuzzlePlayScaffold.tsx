import React from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import type { DialogConfig } from './AppDialog';
import AppDialog from './AppDialog';
import AppScreen from './AppScreen';
import { useTheme } from '../context/ThemeContext';
import type { Theme } from '../theme';

interface PuzzlePlayScaffoldProps {
  loading: boolean;
  loadingLabel?: string;
  dialog?: DialogConfig | null;
  onDismissDialog: () => void;
  header: React.ReactNode;
  grid: React.ReactNode;
  footer: React.ReactNode;
}

export default function PuzzlePlayScaffold({
  loading,
  loadingLabel,
  dialog = null,
  onDismissDialog,
  header,
  grid,
  footer,
}: PuzzlePlayScaffoldProps) {
  const { theme } = useTheme();
  const s = makeStyles(theme);

  return (
    <AppScreen contentStyle={s.container}>
      {loading ? (
        <View style={s.loadingContainer}>
          <ActivityIndicator size="large" color={theme.primary} />
          {loadingLabel ? <Text style={s.loadingText}>{loadingLabel}</Text> : null}
        </View>
      ) : (
        <>
          <View style={s.headerRegion}>{header}</View>
          <View style={s.gridRegion}>{grid}</View>
          <View style={s.footerRegion}>{footer}</View>
        </>
      )}
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

const makeStyles = (theme: Theme) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.background,
  },
  loadingContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  loadingText: {
    marginTop: 12,
    color: theme.textSecondary,
    fontSize: 15,
  },
  headerRegion: {
    paddingTop: 6,
  },
  gridRegion: {
    flex: 1,
  },
  footerRegion: {
    height: 112,
    flexShrink: 0,
    paddingHorizontal: 10,
    paddingTop: 8,
    paddingBottom: 10,
    overflow: 'hidden',
  },
});
