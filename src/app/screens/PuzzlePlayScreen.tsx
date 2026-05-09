import React, { useMemo } from 'react';
import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import type { StackScreenProps } from '@react-navigation/stack';
import PuzzlePlayScaffold from '../components/PuzzlePlayScaffold';
import GridHomeIcon from '../components/GridHomeIcon';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import type { RootStackParamList } from '../navigation/types';
import { usePuzzlePlayController } from '../shell/hooks/usePuzzlePlayController';
import type { Theme } from '../theme';
import { withAlpha } from '../utils/color';
import { formatElapsed } from '../utils/formatElapsed';

type Props = StackScreenProps<RootStackParamList, 'PuzzlePlay'>;

export default function PuzzlePlayScreen(props: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const s = useMemo(() => makeStyles(theme), [theme]);
  const layout = usePuzzlePlayController(props);
  const elapsedLabel = formatElapsed(layout.elapsedSeconds);

  return (
    <PuzzlePlayScaffold
      loading={layout.loading}
      loadingLabel={layout.loadingLabel}
      dialog={layout.dialog}
      onDismissDialog={layout.onDismissDialog}
      header={(
        <View style={s.header}>
          <View style={s.headerMainRow}>
            <TouchableOpacity
              accessibilityRole="button"
              accessibilityLabel={strings.common.goHome}
              onPress={() => {
                void layout.exitToHome();
              }}
              style={s.iconButton}
              activeOpacity={0.8}
            >
              <GridHomeIcon />
            </TouchableOpacity>
            <View style={s.headerActions}>
              <View style={s.timerPill}>
                <Text style={s.timerText}>{elapsedLabel}</Text>
              </View>
              {layout.headerActions.map((action) => (
                <TouchableOpacity
                  key={action.key}
                  accessibilityRole="button"
                  accessibilityLabel={action.accessibilityLabel}
                  onPress={action.onPress}
                  style={[s.iconButton, action.active ? s.helperActive : null]}
                  activeOpacity={0.8}
                >
                  <Ionicons
                    name={action.iconName}
                    size={18}
                    color={action.active ? theme.primaryLight : theme.text}
                  />
                </TouchableOpacity>
              ))}
              <TouchableOpacity
                accessibilityRole="button"
                accessibilityLabel={strings.common.endPuzzle}
                onPress={layout.onForfeit}
                style={s.iconButton}
                activeOpacity={0.8}
              >
                <Ionicons
                  name="flag-outline"
                  size={18}
                  color={theme.text}
                />
              </TouchableOpacity>
            </View>
          </View>
          {layout.headerMeta.length > 0 ? (
            <View style={s.metadataRow}>
              {layout.headerMeta.map((item) => (
                <View key={item.key} style={s.metadataPill}>
                  <Text style={s.metadataLabel}>{item.label}</Text>
                  <Text style={s.metadataValue}>{item.value}</Text>
                </View>
              ))}
            </View>
          ) : null}
        </View>
      )}
      main={layout.main}
      footer={layout.footer}
    />
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  header: {
    paddingHorizontal: 10,
    paddingTop: 6,
    paddingBottom: 6,
    gap: 8,
  },
  headerMainRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  headerActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  iconButton: {
    width: 36,
    height: 36,
    borderRadius: 999,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: withAlpha(theme.surfaceElevated, 0.84),
    borderWidth: 1,
    borderColor: withAlpha(theme.border, 0.72),
  },
  helperActive: {
    backgroundColor: withAlpha(theme.primary, 0.18),
    borderColor: withAlpha(theme.primaryLight, 0.72),
  },
  timerPill: {
    minWidth: 70,
    height: 36,
    paddingHorizontal: 12,
    borderRadius: 999,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: withAlpha(theme.surface, 0.9),
    borderWidth: 1,
    borderColor: withAlpha(theme.border, 0.72),
  },
  timerText: {
    color: theme.text,
    fontSize: 14,
    fontWeight: '700',
  },
  metadataRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  metadataPill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    minHeight: 30,
    paddingHorizontal: 10,
    borderRadius: 999,
    backgroundColor: withAlpha(theme.surface, 0.84),
    borderWidth: 1,
    borderColor: withAlpha(theme.border, 0.6),
  },
  metadataLabel: {
    color: theme.textMuted,
    fontSize: 11,
    fontWeight: '700',
    textTransform: 'uppercase',
    letterSpacing: 0.4,
  },
  metadataValue: {
    color: theme.text,
    fontSize: 13,
    fontWeight: '700',
  },
});
