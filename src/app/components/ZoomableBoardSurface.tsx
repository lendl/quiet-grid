import React, { useCallback, useEffect, useRef } from 'react';
import { StyleSheet, View } from 'react-native';
import {
  Gesture,
  GestureDetector,
} from 'react-native-gesture-handler';
import Animated, {
  runOnJS,
  useAnimatedStyle,
  useDerivedValue,
  useSharedValue,
  withTiming,
} from 'react-native-reanimated';

type Props = {
  children: React.ReactNode;
  minScale?: number;
  maxScale?: number;
  resetThreshold?: number;
  onZoomStateChange?: (isZoomed: boolean) => void;
  onRegisterReset?: (reset: (() => void) | null) => void;
};

type Bounds = {
  maxOffsetX: number;
  maxOffsetY: number;
};

function clamp(value: number, min: number, max: number): number {
  'worklet';
  return Math.min(max, Math.max(min, value));
}

export default function ZoomableBoardSurface({
  children,
  minScale = 1,
  maxScale = 2.5,
  resetThreshold = 1.02,
  onZoomStateChange,
  onRegisterReset,
}: Props) {
  const viewportWidth = useSharedValue(0);
  const viewportHeight = useSharedValue(0);
  const contentWidth = useSharedValue(0);
  const contentHeight = useSharedValue(0);
  const scale = useSharedValue(1);
  const translateX = useSharedValue(0);
  const translateY = useSharedValue(0);
  const startScale = useSharedValue(1);
  const startX = useSharedValue(0);
  const startY = useSharedValue(0);
  const lastReportedZoomed = useSharedValue(false);
  const onZoomStateChangeRef = useRef(onZoomStateChange);
  const resetTransformFromJsRef = useRef<() => void>(() => undefined);

  useEffect(() => {
    onZoomStateChangeRef.current = onZoomStateChange;
  }, [onZoomStateChange]);

  const notifyZoomStateChange = useCallback((nextZoomed: boolean) => {
    onZoomStateChangeRef.current?.(nextZoomed);
  }, []);

  const bounds = useDerivedValue<Bounds>(() => {
    const scaledWidth = contentWidth.value * scale.value;
    const scaledHeight = contentHeight.value * scale.value;

    return {
      maxOffsetX: Math.max(0, (scaledWidth - viewportWidth.value) / 2),
      maxOffsetY: Math.max(0, (scaledHeight - viewportHeight.value) / 2),
    };
  });

  const reportZoomState = (nextZoomed: boolean) => {
    'worklet';
    if (lastReportedZoomed.value === nextZoomed) {
      return;
    }

    lastReportedZoomed.value = nextZoomed;
    runOnJS(notifyZoomStateChange)(nextZoomed);
  };

  const resetTransform = () => {
    'worklet';
    scale.value = withTiming(1, { duration: 180 });
    translateX.value = withTiming(0, { duration: 180 });
    translateY.value = withTiming(0, { duration: 180 });
    reportZoomState(false);
  };

  resetTransformFromJsRef.current = () => {
    scale.value = withTiming(1, { duration: 180 });
    translateX.value = withTiming(0, { duration: 180 });
    translateY.value = withTiming(0, { duration: 180 });
    if (onZoomStateChangeRef.current) {
      onZoomStateChangeRef.current(false);
    }
    lastReportedZoomed.value = false;
  };

  useEffect(() => {
    const handleReset = () => {
      resetTransformFromJsRef.current();
    };

    onRegisterReset?.(handleReset);
    return () => {
      onRegisterReset?.(null);
    };
  }, [onRegisterReset]);

  const pinch = Gesture.Pinch()
    .onStart(() => {
      startScale.value = scale.value;
    })
    .onUpdate((event) => {
      scale.value = clamp(startScale.value * event.scale, minScale, maxScale);
      reportZoomState(scale.value > resetThreshold);
    })
    .onEnd(() => {
      if (scale.value <= resetThreshold) {
        resetTransform();
        return;
      }

      translateX.value = withTiming(
        clamp(translateX.value, -bounds.value.maxOffsetX, bounds.value.maxOffsetX),
        { duration: 180 },
      );
      translateY.value = withTiming(
        clamp(translateY.value, -bounds.value.maxOffsetY, bounds.value.maxOffsetY),
        { duration: 180 },
      );
      reportZoomState(true);
    });

  const pan = Gesture.Pan()
    .minDistance(6)
    .maxPointers(1)
    .onStart(() => {
      startX.value = translateX.value;
      startY.value = translateY.value;
    })
    .onUpdate((event) => {
      if (scale.value <= 1.01) {
        translateX.value = 0;
        translateY.value = 0;
        return;
      }

      translateX.value = clamp(
        startX.value + event.translationX,
        -bounds.value.maxOffsetX,
        bounds.value.maxOffsetX,
      );
      translateY.value = clamp(
        startY.value + event.translationY,
        -bounds.value.maxOffsetY,
        bounds.value.maxOffsetY,
      );
    })
    .onEnd(() => {
      if (scale.value <= resetThreshold) {
        resetTransform();
      }
    });

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [
      { translateX: translateX.value },
      { translateY: translateY.value },
      { scale: scale.value },
    ],
  }));

  const gesture = Gesture.Simultaneous(pinch, pan);

  return (
    <GestureDetector gesture={gesture}>
      <View
        style={styles.viewport}
        onLayout={(event) => {
          const { width, height } = event.nativeEvent.layout;
          viewportWidth.value = width;
          viewportHeight.value = height;
        }}
      >
        <Animated.View style={animatedStyle}>
          <View
            onLayout={(event) => {
              const { width, height } = event.nativeEvent.layout;
              contentWidth.value = width;
              contentHeight.value = height;
            }}
          >
            {children}
          </View>
        </Animated.View>
      </View>
    </GestureDetector>
  );
}

const styles = StyleSheet.create({
  viewport: {
    flex: 1,
    minWidth: 0,
    minHeight: 0,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
});
