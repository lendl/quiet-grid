import React from 'react';
import {
  StyleSheet,
  type StyleProp,
  View,
  type ViewStyle,
} from 'react-native';
import {
  SafeAreaView,
  useSafeAreaInsets,
  type Edge,
} from 'react-native-safe-area-context';
import { useTheme } from '../context/ThemeContext';
import ScreenOverlay from './ScreenOverlay';

interface AppScreenProps {
  children?: React.ReactNode;
  contentStyle?: StyleProp<ViewStyle>;
  edges?: Edge[];
  overlay?: boolean;
}

function getBaseBottomPadding(style: StyleProp<ViewStyle>): number {
  const flattened = StyleSheet.flatten(style);
  if (!flattened) {
    return 0;
  }
  if (typeof flattened.paddingBottom === 'number') {
    return flattened.paddingBottom;
  }
  if (typeof flattened.paddingVertical === 'number') {
    return flattened.paddingVertical;
  }
  if (typeof flattened.padding === 'number') {
    return flattened.padding;
  }
  return 0;
}

export default function AppScreen({
  children,
  contentStyle,
  edges = ['top', 'left', 'right'],
  overlay = true,
}: AppScreenProps) {
  const { theme } = useTheme();
  const insets = useSafeAreaInsets();
  const baseBottomPadding = getBaseBottomPadding(contentStyle);

  return (
    <SafeAreaView style={[styles.safeArea, { backgroundColor: theme.background }]} edges={edges}>
      {overlay ? <ScreenOverlay /> : null}
      <View style={[styles.content, contentStyle, { paddingBottom: baseBottomPadding + insets.bottom }]}>
        {children}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
  },
  content: {
    flex: 1,
  },
});
