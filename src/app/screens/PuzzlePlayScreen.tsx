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
import { formatElapsed } from '../utils/format';

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
            {layout.showHelperToggle ? (
              <TouchableOpacity
                accessibilityRole="button"
                accessibilityLabel={layout.helperToggleLabel}
                onPress={layout.onToggleHelper}
                style={[s.iconButton, layout.helperVisible ? s.helperActive : null]}
                activeOpacity={0.8}
              >
                <Ionicons
                  name={layout.helperVisible ? 'bulb' : 'bulb-outline'}
                  size={18}
                  color={layout.helperVisible ? theme.primaryLight : theme.text}
                />
              </TouchableOpacity>
            ) : null}
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
      )}
      grid={layout.grid}
      footer={layout.footer}
    />
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 10,
    paddingTop: 6,
    paddingBottom: 6,
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
});
