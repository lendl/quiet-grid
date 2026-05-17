import React, { useMemo } from 'react';
import { StackActions } from '@react-navigation/native';
import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import type { StackScreenProps } from '@react-navigation/stack';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import GlobalBottomNav from '../components/GlobalBottomNav';
import PuzzlePlayScaffold from '../components/PuzzlePlayScaffold';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import type { RootStackParamList } from '../navigation/types';
import { usePuzzlePlayController } from '../shell/hooks/usePuzzlePlayController';
import type { Theme } from '../theme';
import { withAlpha } from '../utils/color';
import { formatElapsed } from '../utils/formatElapsed';
import { loadShowTimerInPlay } from '../utils/settingsStorage';

type Props = StackScreenProps<RootStackParamList, 'PuzzlePlay'>;

export default function PuzzlePlayScreen(props: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const insets = useSafeAreaInsets();
  const s = useMemo(() => makeStyles(theme), [theme]);
  const layout = usePuzzlePlayController(props);
  const elapsedLabel = formatElapsed(layout.elapsedSeconds);
  const visibleHeaderMeta = useMemo(
    () => layout.headerMeta.filter((item) => item.key !== 'difficulty'),
    [layout.headerMeta],
  );
  const [showTimerInPlay, setShowTimerInPlay] = React.useState(true);

  React.useEffect(() => {
    let mounted = true;

    void loadShowTimerInPlay().then((enabled) => {
      if (mounted) {
        setShowTimerInPlay(enabled);
      }
    });

    return () => {
      mounted = false;
    };
  }, []);

  const handleBackToDifficulty = React.useCallback(() => {
    props.navigation.dispatch(StackActions.replace('Puzzle', {
      puzzleTypeId: props.route.params.puzzleTypeId,
    }));
  }, [props.navigation, props.route.params.puzzleTypeId]);

  return (
    <PuzzlePlayScaffold
      loading={layout.loading}
      loadingLabel={layout.loadingLabel}
      dialog={layout.dialog}
      onDismissDialog={layout.onDismissDialog}
      topSlot={(
        <View style={s.topBar}>
          <View style={[s.topBarInner, { paddingTop: insets.top + 12 }]}>
            <TouchableOpacity
              accessibilityRole="button"
              accessibilityLabel={strings.common.goBack}
              onPress={handleBackToDifficulty}
              style={s.backButton}
              activeOpacity={0.82}
            >
              <Ionicons name="arrow-back" size={20} color={theme.text} />
            </TouchableOpacity>
            <View style={s.headerActions}>
              {showTimerInPlay ? (
                <View style={s.timerPill}>
                  <Text style={s.timerText}>{elapsedLabel}</Text>
                </View>
              ) : null}
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
                <Ionicons name="flag-outline" size={18} color={theme.text} />
              </TouchableOpacity>
            </View>
          </View>
        </View>
      )}
      header={visibleHeaderMeta.length > 0 ? (
        <View style={s.metadataRow}>
          {visibleHeaderMeta.map((item) => (
            <View key={item.key} style={s.metadataPill}>
              <Text style={s.metadataLabel}>{item.label}</Text>
              <Text style={s.metadataValue}>{item.value}</Text>
            </View>
          ))}
        </View>
      ) : null}
      main={layout.main}
      footer={layout.footer}
      bottomSlot={<GlobalBottomNav activeTab="Games" />}
    />
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  topBar: {
    paddingHorizontal: 16,
    paddingBottom: 6,
  },
  topBarInner: {
    minHeight: 56,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
  },
  headerActions: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'flex-end',
    gap: 8,
    flexShrink: 1,
  },
  backButton: {
    width: 36,
    height: 36,
    borderRadius: 999,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: theme.surfaceElevated,
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
    paddingHorizontal: 10,
    paddingTop: 2,
    paddingBottom: 6,
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
