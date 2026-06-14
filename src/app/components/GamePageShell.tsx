import React, { useCallback, useMemo } from 'react';
import { StackActions, useNavigation, type NavigationProp } from '@react-navigation/native';
import { StyleSheet, type StyleProp, type ViewStyle } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import AppScreen from './AppScreen';
import AnimatedContentView from './AnimatedContentView';
import AppTopBar from './AppTopBar';
import GlobalBottomNav from './GlobalBottomNav';
import GamePageNav from './GamePageNav';
import TopBackgroundEffect from './TopBackgroundEffect';
import { useTheme } from '../context/ThemeContext';
import { getGameDefinition } from '../shell/games/gameRegistry';
import type { GameTabParamList, MainTabParamList, RootStackParamList, TransitionDirection } from '../navigation/types';
import type { GameId } from '../../games/shared/types';
import type { Theme } from '../theme';

type GameNavConfig =
  | {
    context: 'tabs';
    activeTab: keyof GameTabParamList;
    gameId: GameId;
  }
  | {
    context: 'root';
    activeTab: keyof GameTabParamList;
    gameId: GameId;
  };

type Props = {
  children?: React.ReactNode;
  contentStyle?: StyleProp<ViewStyle>;
  bodyStyle?: StyleProp<ViewStyle>;
  activeTab?: keyof MainTabParamList;
  headerMode?: 'brand' | 'back' | 'none';
  headerRight?: React.ReactNode;
  backToPuzzleTypeId?: GameId;
  gameNav?: GameNavConfig;
  contentTransitionDirection?: TransitionDirection;
};

export default function GamePageShell({
  children,
  contentStyle,
  bodyStyle,
  activeTab = 'Games',
  headerMode = 'brand',
  headerRight,
  backToPuzzleTypeId,
  gameNav,
  contentTransitionDirection = 'none',
}: Props) {
  const { theme } = useTheme();
  const insets = useSafeAreaInsets();
  const navigation = useNavigation<NavigationProp<RootStackParamList>>();
  const s = useMemo(() => makeStyles(theme), [theme]);
  const gameName = useMemo(
    () => gameNav ? getGameDefinition(gameNav.gameId).title : undefined,
    [gameNav],
  );
  const handleBack = useCallback(() => {
    if (backToPuzzleTypeId) {
      navigation.dispatch(StackActions.replace('Game', { gameId: backToPuzzleTypeId }));
      return;
    }

    navigation.goBack();
  }, [backToPuzzleTypeId, navigation]);

  return (
    <AppScreen edges={['left', 'right']} overlay={false} includeBottomInset={false} contentStyle={[s.container, contentStyle]}>
      <TopBackgroundEffect topOffset={-insets.top} />
      {headerMode === 'brand' ? <AppTopBar mode="brand" gameName={gameName} /> : null}
      {headerMode === 'back' ? <AppTopBar mode="back" onBack={handleBack} rightSlot={headerRight} /> : null}
      {gameNav ? (
        <GamePageNav
          context={gameNav.context}
          activeTab={gameNav.activeTab}
          gameId={gameNav.gameId}
        />
      ) : null}
      <AnimatedContentView style={[s.body, bodyStyle]} direction={contentTransitionDirection}>
        {children}
      </AnimatedContentView>
      <GlobalBottomNav activeTab={activeTab} />
    </AppScreen>
  );
}

const makeStyles = (_theme: Theme) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'transparent',
  },
  body: {
    flex: 1,
    minHeight: 0,
  },
});
