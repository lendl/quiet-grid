import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Animated, BackHandler, Easing, Pressable, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import GlobalMenu from './GlobalMenu';
import { useTheme } from '../context/ThemeContext';
import type { Theme } from '../theme';
import { withAlpha } from '../utils/color';

type Props = {
  visible: boolean;
  onClose: () => void;
  onContinue?: () => void;
};

export default function GameMenuOverlay({ visible, onClose, onContinue }: Props) {
  const { theme } = useTheme();
  const insets = useSafeAreaInsets();
  const s = useMemo(() => makeStyles(theme, insets.top), [insets.top, theme]);
  const translateY = useRef(new Animated.Value(-24)).current;
  const opacity = useRef(new Animated.Value(0)).current;
  const [rendered, setRendered] = useState(visible);

  useEffect(() => {
    if (!rendered) {
      return undefined;
    }

    const subscription = BackHandler.addEventListener('hardwareBackPress', () => {
      onClose();
      return true;
    });

    return () => subscription.remove();
  }, [onClose, rendered]);

  useEffect(() => {
    if (visible) {
      setRendered(true);
      translateY.stopAnimation();
      opacity.stopAnimation();
      translateY.setValue(-24);
      opacity.setValue(0);
      Animated.parallel([
        Animated.timing(translateY, {
          toValue: 0,
          duration: 220,
          easing: Easing.out(Easing.cubic),
          useNativeDriver: true,
        }),
        Animated.timing(opacity, {
          toValue: 1,
          duration: 180,
          easing: Easing.out(Easing.quad),
          useNativeDriver: true,
        }),
      ]).start();
      return;
    }

    if (!rendered) {
      return;
    }

    translateY.stopAnimation();
    opacity.stopAnimation();
    Animated.parallel([
      Animated.timing(translateY, {
        toValue: -24,
        duration: 180,
        easing: Easing.in(Easing.cubic),
        useNativeDriver: true,
      }),
      Animated.timing(opacity, {
        toValue: 0,
        duration: 140,
        easing: Easing.in(Easing.quad),
        useNativeDriver: true,
      }),
    ]).start(({ finished }) => {
      if (finished) {
        setRendered(false);
      }
    });
  }, [opacity, rendered, translateY, visible]);

  if (!rendered) {
    return null;
  }

  return (
    <View style={s.overlay} pointerEvents="box-none">
      <Pressable style={s.backdrop} onPress={onClose} />
      <Animated.View
        style={[
          s.panelWrap,
          {
            opacity,
            transform: [{ translateY }],
          },
        ]}
        pointerEvents="box-none"
      >
        <GlobalMenu
          onClose={onClose}
          onContinue={onContinue}
          topInset={insets.top}
        />
      </Animated.View>
    </View>
  );
}

const makeStyles = (theme: Theme, topInset: number) => StyleSheet.create({
  overlay: {
    position: 'absolute',
    top: -topInset,
    right: 0,
    bottom: 0,
    left: 0,
    zIndex: 40,
  },
  backdrop: {
    ...StyleSheet.absoluteFill,
    backgroundColor: withAlpha(theme.background, 0.72),
  },
  panelWrap: {
    paddingTop: 0,
  },
});
