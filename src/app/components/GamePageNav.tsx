import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { StackActions, useNavigation, useNavigationState } from '@react-navigation/native';
import type { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import type { StackNavigationProp } from '@react-navigation/stack';
import { Animated, Easing, StyleSheet, Text, View } from 'react-native';
import { TouchableRipple } from 'react-native-paper';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import type {
  GameTabParamList,
  RootStackParamList,
  TransitionDirection,
} from '../navigation/types';
import type { GameId } from '../../games/shared/types';
import type { Theme } from '../theme';
import { withAlpha } from '../utils/color';

type GameTabName = keyof GameTabParamList;

type BaseProps = {
  activeTab: GameTabName;
  gameId: GameId;
};

type TabContextProps = BaseProps & {
  context: 'tabs';
};

type RootContextProps = BaseProps & {
  context: 'root';
};

type Props = TabContextProps | RootContextProps;

type NavItem = {
  key: GameTabName;
  label: string;
};

export default function GamePageNav(props: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const s = useMemo(() => makeStyles(theme), [theme]);
  const tabNavigation = useNavigation<BottomTabNavigationProp<GameTabParamList>>();
  const rootNavigation = useNavigation<StackNavigationProp<RootStackParamList>>();
  const items = useMemo<NavItem[]>(() => {
    return [
      { key: 'Play', label: strings.common.play },
      { key: 'Rules', label: strings.common.rules },
      { key: 'Stats', label: strings.common.stats },
    ];
  }, [strings]);
  const resolvedActiveTab = useNavigationState((state) => {
    const routeName = state.routeNames[state.index];
    return items.some((item) => item.key === routeName)
      ? routeName as GameTabName
      : props.activeTab;
  });
  const [rowWidth, setRowWidth] = useState(0);
  const [labelLayouts, setLabelLayouts] = useState<Record<string, { width: number; x: number }>>({});
  const activeIndex = useMemo(
    () => items.findIndex((item) => item.key === resolvedActiveTab),
    [items, resolvedActiveTab],
  );
  const indicatorX = useRef(new Animated.Value(0)).current;
  const indicatorWidth = useRef(new Animated.Value(0)).current;
  const indicatorAnimation = useRef<Animated.CompositeAnimation | null>(null);
  const navigationAttempt = useRef(0);

  const segmentWidth = rowWidth > 0 ? rowWidth / items.length : 0;

  const getIndicatorMetrics = useCallback((index: number) => {
    if (index < 0 || segmentWidth <= 0) {
      return null;
    }

    const item = items[index];
    const labelLayout = item ? labelLayouts[item.key] : undefined;
    const textWidth = labelLayout?.width ?? 0;
    const width = Math.max(28, textWidth + 12);
    const defaultTextX = (segmentWidth - textWidth) / 2;
    const textX = labelLayout?.x ?? defaultTextX;
    const x = (index * segmentWidth) + textX - 6;

    return { x, width };
  }, [items, labelLayouts, segmentWidth]);

  useEffect(() => {
    indicatorAnimation.current?.stop();
    navigationAttempt.current += 1;

    const metrics = getIndicatorMetrics(activeIndex);
    if (!metrics) {
      return;
    }

    indicatorX.setValue(metrics.x);
    indicatorWidth.setValue(metrics.width);
  }, [activeIndex, getIndicatorMetrics, indicatorWidth, indicatorX]);

  useEffect(() => {
    return () => {
      indicatorAnimation.current?.stop();
    };
  }, []);

  const navigateTo = (target: GameTabName, direction: TransitionDirection) => {
    if (target === resolvedActiveTab) {
      return;
    }

    if (props.context === 'tabs') {
      tabNavigation.navigate(target, {
        gameId: props.gameId,
        transitionDirection: direction,
      });
      return;
    }

    rootNavigation.dispatch(StackActions.replace('Game', {
      gameId: props.gameId,
      initialTab: target,
      initialDirection: direction,
    }));
  };

  const handlePress = (target: GameTabName) => {
    const targetIndex = items.findIndex((item) => item.key === target);
    if (targetIndex < 0 || target === resolvedActiveTab) {
      return;
    }
    const direction: TransitionDirection = targetIndex > activeIndex ? 'forward' : 'backward';

    const metrics = getIndicatorMetrics(targetIndex);
    if (metrics) {
      navigationAttempt.current += 1;
      indicatorAnimation.current?.stop();

      const animation = Animated.parallel([
        Animated.timing(indicatorX, {
          toValue: metrics.x,
          duration: 200,
          easing: Easing.bezier(0.2, 0, 0, 1),
          useNativeDriver: false,
        }),
        Animated.timing(indicatorWidth, {
          toValue: metrics.width,
          duration: 200,
          easing: Easing.bezier(0.2, 0, 0, 1),
          useNativeDriver: false,
        }),
      ]);

      indicatorAnimation.current = animation;
      animation.start();
    }

    // Navigate immediately so the content transition and indicator slide together.
    navigateTo(target, direction);
  };

  return (
    <View style={s.container}>
      <View style={s.row} onLayout={(event) => setRowWidth(event.nativeEvent.layout.width)}>
        <View style={s.baseLine} />
        {segmentWidth > 0 ? (
          <Animated.View
            style={[
              s.activeIndicator,
              {
                width: indicatorWidth,
                transform: [{ translateX: indicatorX }],
              },
            ]}
          />
        ) : null}
        {items.map((item) => {
          const focused = item.key === resolvedActiveTab;

          return (
            <TouchableRipple
              key={item.key}
              accessibilityRole="button"
              accessibilityLabel={item.label}
              onPress={() => handlePress(item.key)}
              style={s.badge}
              borderless
            >
              <Text
                style={[s.badgeText, focused ? s.badgeTextActive : null]}
                onLayout={(event) => {
                  const { width, x } = event.nativeEvent.layout;
                  setLabelLayouts((current) => {
                    const previous = current[item.key];
                    if (previous && previous.width === width && previous.x === x) {
                      return current;
                    }

                    return {
                      ...current,
                      [item.key]: { width, x },
                    };
                  });
                }}
              >
                {item.label}
              </Text>
            </TouchableRipple>
          );
        })}
      </View>
    </View>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  container: {
    paddingHorizontal: 16,
    paddingBottom: 10,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    position: 'relative',
    paddingBottom: 10,
  },
  baseLine: {
    position: 'absolute',
    right: 0,
    bottom: 0,
    left: 0,
    height: 1,
    backgroundColor: withAlpha(theme.border, 0.72),
  },
  activeIndicator: {
    position: 'absolute',
    bottom: 0,
    height: 3,
    backgroundColor: theme.primary,
  },
  badge: {
    flex: 1,
    minHeight: 34,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 8,
  },
  badgeText: {
    fontSize: 13,
    fontWeight: '700',
    color: theme.textSecondary,
  },
  badgeTextActive: {
    color: theme.primary,
  },
});
