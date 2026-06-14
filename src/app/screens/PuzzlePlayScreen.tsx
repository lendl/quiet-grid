import React, { useCallback, useMemo, useRef, useState } from 'react';
import { StackActions } from '@react-navigation/native';
import Ionicons from '@react-native-vector-icons/ionicons';
import { StyleSheet, Text, View } from 'react-native';
import { TouchableRipple } from 'react-native-paper';
import type { StackScreenProps } from '@react-navigation/stack';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Popover, { PopoverPlacement, Rect as PopoverRect } from 'react-native-popover-view';
import PuzzlePlayScaffold from '../components/PuzzlePlayScaffold';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import type { RootStackParamList } from '../navigation/types';
import type { PuzzleHeaderAction } from '../shell/games/playAdapter';
import { usePuzzlePlayController } from '../shell/hooks/usePuzzlePlayController';
import type { Theme } from '../theme';
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
    props.navigation.dispatch(StackActions.replace('Game', {
      gameId: props.route.params.puzzleTypeId,
    }));
  }, [props.navigation, props.route.params.puzzleTypeId]);

  const handleHowToPlay = React.useCallback(() => {
    props.navigation.navigate('HowToPlay', { gameId: props.route.params.puzzleTypeId });
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
            <TouchableRipple
              accessibilityRole="button"
              accessibilityLabel={strings.common.goBack}
              onPress={handleBackToDifficulty}
              style={s.backButton}
              borderless
            >
              <Ionicons name="arrow-back" size={20} color={theme.text} />
            </TouchableRipple>
            <View style={s.headerActions}>
              {showTimerInPlay ? (
                <View style={s.timerPill}>
                  <Text style={s.timerText}>{elapsedLabel}</Text>
                </View>
              ) : null}
              {layout.headerActions.map((action) => (
                action.popoverContent != null ? (
                  <HeaderPopoverButton key={action.key} action={action} theme={theme} s={s} />
                ) : (
                  <TouchableRipple
                    key={action.key}
                    accessibilityRole="button"
                    accessibilityLabel={action.accessibilityLabel}
                    onPress={action.onPress}
                    style={s.iconButton}
                    borderless
                  >
                    <Ionicons
                      name={action.iconName}
                      size={18}
                      color={action.active ? theme.primaryLight : theme.text}
                    />
                  </TouchableRipple>
                )
              ))}
              <TouchableRipple
                accessibilityRole="button"
                accessibilityLabel={strings.common.rules}
                onPress={handleHowToPlay}
                style={s.iconButton}
                borderless
              >
                <Ionicons name="help-outline" size={18} color={theme.text} />
              </TouchableRipple>
              <TouchableRipple
                accessibilityRole="button"
                accessibilityLabel={strings.common.endPuzzle}
                onPress={layout.onForfeit}
                style={s.iconButton}
                borderless
              >
                <Ionicons name="flag-outline" size={18} color={theme.text} />
              </TouchableRipple>
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
    />
  );
}

interface HeaderPopoverButtonProps {
  action: PuzzleHeaderAction;
  theme: Theme;
  s: ReturnType<typeof makeStyles>;
}

function HeaderPopoverButton({ action, theme, s }: HeaderPopoverButtonProps) {
  const buttonRef = useRef<View>(null);
  const [buttonRect, setButtonRect] = useState<PopoverRect | null>(null);

  const measureButton = useCallback(() => {
    buttonRef.current?.measureInWindow((x, y, width, height) => {
      setButtonRect(new PopoverRect(x, y, width, height));
    });
  }, []);

  const isVisible = !!(action.active && action.popoverContent != null && buttonRect != null);

  return (
    <>
      <View ref={buttonRef} collapsable={false} onLayout={measureButton}>
        <TouchableRipple
          accessibilityRole="button"
          accessibilityLabel={action.accessibilityLabel}
          onPress={action.onPress}
          style={s.iconButton}
          borderless
        >
          <Ionicons
            name={action.iconName}
            size={18}
            color={action.active ? theme.primary : theme.text}
          />
        </TouchableRipple>
      </View>
      <Popover
        from={buttonRect ?? new PopoverRect(0, 0, 0, 0)}
        isVisible={isVisible}
        onRequestClose={action.onPress}
        placement={PopoverPlacement.BOTTOM}
        popoverStyle={{ borderRadius: 16, backgroundColor: theme.surfaceElevated }}
        backgroundStyle={{ backgroundColor: 'transparent' }}
      >
        <View style={s.popoverContent}>
          {action.popoverContent}
        </View>
      </Popover>
    </>
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
    alignItems: 'center',
    justifyContent: 'center',
  },
  iconButton: {
    width: 36,
    height: 36,
    alignItems: 'center',
    justifyContent: 'center',
  },
  timerPill: {
    minWidth: 70,
    height: 36,
    paddingHorizontal: 12,
    borderRadius: 999,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: theme.surfaceElevated,
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
    backgroundColor: theme.surfaceElevated,
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
  popoverContent: {
    paddingHorizontal: 14,
    paddingVertical: 12,
    width: 280,
  },
});
