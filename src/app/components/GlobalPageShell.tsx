import React from 'react';
import { StyleSheet, type StyleProp, type ViewStyle } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import AppScreen from './AppScreen';
import AnimatedContentView from './AnimatedContentView';
import AppTopBar from './AppTopBar';
import GlobalBottomNav from './GlobalBottomNav';
import TopBackgroundEffect from './TopBackgroundEffect';
import type { MainTabParamList } from '../navigation/types';

type Props = {
  children?: React.ReactNode;
  contentStyle?: StyleProp<ViewStyle>;
  bodyStyle?: StyleProp<ViewStyle>;
  activeTab?: keyof MainTabParamList;
};

export default function GlobalPageShell({
  children,
  contentStyle,
  bodyStyle,
  activeTab,
}: Props) {
  const insets = useSafeAreaInsets();

  return (
    <AppScreen
      edges={['left', 'right']}
      overlay={false}
      includeBottomInset={false}
      contentStyle={[styles.container, contentStyle]}
    >
      <TopBackgroundEffect topOffset={-insets.top} />
      <AppTopBar mode="brand" />
      <AnimatedContentView style={[styles.body, bodyStyle]}>
        {children}
      </AnimatedContentView>
      <GlobalBottomNav activeTab={activeTab} />
    </AppScreen>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'transparent',
  },
  body: {
    flex: 1,
    minHeight: 0,
  },
});
