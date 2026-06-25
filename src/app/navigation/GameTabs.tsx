import React, { useCallback, useMemo, useRef, useState } from 'react';
import { Animated, Easing, StyleSheet } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import AppScreen from '../components/AppScreen';
import AppTopBar from '../components/AppTopBar';
import GamePageNav from '../components/GamePageNav';
import GlobalBottomNav from '../components/GlobalBottomNav';
import TopBackgroundEffect from '../components/TopBackgroundEffect';
import { useTheme } from '../context/ThemeContext';
import { getGameDefinition } from '../shell/games/gameRegistry';
import type { GameTabParamList, TransitionDirection } from './types';
import type { GameId } from '../../games/shared/types';
import GamePlayTab from '../screens/game/GamePlayTab';
import GameRulesTab from '../screens/game/GameRulesTab';
import GameStatsTab from '../screens/game/GameStatsTab';

type GameTabName = keyof GameTabParamList;

type Props = {
  gameId: GameId;
  initialTab?: GameTabName;
  initialDirection?: TransitionDirection;
};

export default function GameTabs({ gameId, initialTab = 'Play' }: Props) {
  const { theme } = useTheme();
  const insets = useSafeAreaInsets();
  const [activeTab, setActiveTab] = useState<GameTabName>(initialTab);
  const gameName = useMemo(() => getGameDefinition(gameId).title, [gameId]);

  const opacity = useRef(new Animated.Value(1)).current;
  const translateX = useRef(new Animated.Value(0)).current;
  const animationRef = useRef<Animated.CompositeAnimation | null>(null);

  const handleNavigate = useCallback((tab: GameTabName, direction: TransitionDirection) => {
    animationRef.current?.stop();

    if (direction === 'none') {
      opacity.setValue(1);
      translateX.setValue(0);
      setActiveTab(tab);
      return;
    }

    const startOffset = direction === 'backward' ? -8 : 8;
    opacity.setValue(0.82);
    translateX.setValue(startOffset);
    setActiveTab(tab);

    const animation = Animated.parallel([
      Animated.timing(opacity, {
        toValue: 1,
        duration: 150,
        easing: Easing.out(Easing.quad),
        useNativeDriver: true,
      }),
      Animated.timing(translateX, {
        toValue: 0,
        duration: 160,
        easing: Easing.out(Easing.cubic),
        useNativeDriver: true,
      }),
    ]);
    animationRef.current = animation;
    animation.start(() => { animationRef.current = null; });
  }, [opacity, translateX]);

  return (
    <AppScreen
      edges={['left', 'right']}
      overlay={false}
      includeBottomInset={false}
      contentStyle={[styles.container, { backgroundColor: theme.background }]}
    >
      <TopBackgroundEffect topOffset={-insets.top} />
      <AppTopBar mode="brand" gameName={gameName} />
      <GamePageNav
        context="inline"
        gameId={gameId}
        activeTab={activeTab}
        onNavigate={handleNavigate}
      />
      <Animated.View style={[styles.content, { opacity, transform: [{ translateX }] }]}>
        {activeTab === 'Play' ? (
          <GamePlayTab gameId={gameId} />
        ) : activeTab === 'Rules' ? (
          <GameRulesTab gameId={gameId} />
        ) : (
          <GameStatsTab gameId={gameId} />
        )}
      </Animated.View>
      <GlobalBottomNav activeTab="Games" />
    </AppScreen>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'transparent',
  },
  content: {
    flex: 1,
    minHeight: 0,
  },
});
