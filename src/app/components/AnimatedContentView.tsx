import React, { useEffect, useRef } from 'react';
import { useIsFocused } from '@react-navigation/native';
import { Animated, Easing, type StyleProp, type ViewStyle } from 'react-native';
import type { TransitionDirection } from '../navigation/types';

type Props = {
  children?: React.ReactNode;
  style?: StyleProp<ViewStyle>;
  direction?: TransitionDirection;
};

export default function AnimatedContentView({ children, style, direction = 'forward' }: Props) {
  const isFocused = useIsFocused();
  const opacity = useRef(new Animated.Value(1)).current;
  const translateX = useRef(new Animated.Value(0)).current;
  const animationRef = useRef<Animated.CompositeAnimation | null>(null);

  useEffect(() => {
    if (!isFocused) {
      return;
    }

    animationRef.current?.stop();

    if (direction === 'none') {
      opacity.setValue(1);
      translateX.setValue(0);
      return;
    }

    const startOffset = direction === 'backward' ? -10 : 10;
    opacity.setValue(0.72);
    translateX.setValue(startOffset);

    const animation = Animated.parallel([
      Animated.timing(opacity, {
        toValue: 1,
        duration: 170,
        easing: Easing.out(Easing.quad),
        useNativeDriver: true,
      }),
      Animated.timing(translateX, {
        toValue: 0,
        duration: 190,
        easing: Easing.out(Easing.cubic),
        useNativeDriver: true,
      }),
    ]);

    animationRef.current = animation;
    animation.start(() => {
      animationRef.current = null;
    });

    return () => {
      animation.stop();
      if (animationRef.current === animation) {
        animationRef.current = null;
      }
    };
  }, [direction, isFocused, opacity, translateX]);

  return (
    <Animated.View
      style={[
        style,
        {
          opacity,
          transform: [{ translateX }],
        },
      ]}
    >
      {children}
    </Animated.View>
  );
}
