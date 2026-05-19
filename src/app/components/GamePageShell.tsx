import React, { useCallback, useMemo } from 'react';
import { StackActions, useNavigation, type NavigationProp } from '@react-navigation/native';
import { StyleSheet, type StyleProp, type ViewStyle } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import AppScreen from './AppScreen';
import AnimatedContentView from './AnimatedContentView';
import AppTopBar from './AppTopBar';
import GlobalBottomNav from './GlobalBottomNav';
import PuzzlePageNav from './PuzzlePageNav';
import TopBackgroundEffect from './TopBackgroundEffect';
import { useTheme } from '../context/ThemeContext';
import type { MainTabParamList, PuzzleTabParamList, RootStackParamList, TransitionDirection, TutorialEntryPoint } from '../navigation/types';
import type { PuzzleTypeId } from '../shell/types';
import type { Theme } from '../theme';

type PuzzleNavConfig =
  | {
    context: 'tabs';
    activeTab: keyof PuzzleTabParamList;
    puzzleTypeId: PuzzleTypeId;
  }
  | {
    context: 'root';
    activeTab: keyof PuzzleTabParamList;
    puzzleTypeId: PuzzleTypeId;
    tutorialEntry?: TutorialEntryPoint;
  };

type Props = {
  children?: React.ReactNode;
  contentStyle?: StyleProp<ViewStyle>;
  bodyStyle?: StyleProp<ViewStyle>;
  activeTab?: keyof MainTabParamList;
  headerMode?: 'brand' | 'back' | 'none';
  headerRight?: React.ReactNode;
  backToPuzzleTypeId?: PuzzleTypeId;
  puzzleNav?: PuzzleNavConfig;
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
  puzzleNav,
  contentTransitionDirection = 'none',
}: Props) {
  const { theme } = useTheme();
  const insets = useSafeAreaInsets();
  const navigation = useNavigation<NavigationProp<RootStackParamList>>();
  const s = useMemo(() => makeStyles(theme), [theme]);
  const handleBack = useCallback(() => {
    if (backToPuzzleTypeId) {
      navigation.dispatch(StackActions.replace('Puzzle', { puzzleTypeId: backToPuzzleTypeId }));
      return;
    }

    navigation.goBack();
  }, [backToPuzzleTypeId, navigation]);

  return (
    <AppScreen edges={['left', 'right']} overlay={false} includeBottomInset={false} contentStyle={[s.container, contentStyle]}>
      <TopBackgroundEffect topOffset={-insets.top} />
      {headerMode === 'brand' ? <AppTopBar mode="brand" /> : null}
      {headerMode === 'back' ? <AppTopBar mode="back" onBack={handleBack} rightSlot={headerRight} /> : null}
      {puzzleNav ? (
        <PuzzlePageNav
          context={puzzleNav.context}
          activeTab={puzzleNav.activeTab}
          puzzleTypeId={puzzleNav.puzzleTypeId}
          tutorialEntry={puzzleNav.context === 'root' ? puzzleNav.tutorialEntry : undefined}
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
