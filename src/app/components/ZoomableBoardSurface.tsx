/* eslint-disable react-hooks/immutability */
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
  autoFocus?: {
    key: string;
    xRatio: number;
    yRatio: number;
    scale: number;
  };
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
  autoFocus,
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
  const lastViewportSizeRef = useRef({ width: 0, height: 0 });
  const viewportSizeRef = useRef({ width: 0, height: 0 });
  const contentSizeRef = useRef({ width: 0, height: 0 });
  const lastAutoFocusKeyRef = useRef<string | null>(null);

  useEffect(() => {
    onZoomStateChangeRef.current = onZoomStateChange;
  }, [onZoomStateChange]);

  const notifyZoomStateChange = useCallback((nextZoomed: boolean) => {
    onZoomStateChangeRef.current?.(nextZoomed);
  }, []);

  const resetFromJs = useCallback(() => {
    resetTransformFromJsRef.current();
  }, []);

  const applyAutoFocusFromJs = useCallback(() => {
    if (!autoFocus) {
      return;
    }

    const viewport = viewportSizeRef.current;
    const content = contentSizeRef.current;
    if (viewport.width <= 0 || viewport.height <= 0 || content.width <= 0 || content.height <= 0) {
      return;
    }

    const nextScale = clamp(autoFocus.scale, minScale, maxScale);
    const focusX = clamp(autoFocus.xRatio, 0, 1) * content.width;
    const focusY = clamp(autoFocus.yRatio, 0, 1) * content.height;
    const scaledWidth = content.width * nextScale;
    const scaledHeight = content.height * nextScale;
    const maxOffsetX = Math.max(0, (scaledWidth - viewport.width) / 2);
    const maxOffsetY = Math.max(0, (scaledHeight - viewport.height) / 2);
    const nextTranslateX = clamp((content.width / 2 - focusX) * nextScale, -maxOffsetX, maxOffsetX);
    const nextTranslateY = clamp((content.height / 2 - focusY) * nextScale, -maxOffsetY, maxOffsetY);

    scale.value = withTiming(nextScale, { duration: 180 });
    translateX.value = withTiming(nextTranslateX, { duration: 180 });
    translateY.value = withTiming(nextTranslateY, { duration: 180 });

    const nextZoomed = nextScale > resetThreshold;
    if (onZoomStateChangeRef.current) {
      onZoomStateChangeRef.current(nextZoomed);
    }
    lastReportedZoomed.value = nextZoomed;
    lastAutoFocusKeyRef.current = autoFocus.key;
  }, [autoFocus, maxScale, minScale, resetThreshold, scale, translateX, translateY, lastReportedZoomed]);

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

  useEffect(() => {
    if (!autoFocus) {
      lastAutoFocusKeyRef.current = null;
      return;
    }

    if (lastAutoFocusKeyRef.current === autoFocus.key) {
      return;
    }

    applyAutoFocusFromJs();
  }, [applyAutoFocusFromJs, autoFocus]);

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
          viewportSizeRef.current = { width, height };

          const previousViewport = lastViewportSizeRef.current;
          const viewportChanged = previousViewport.width > 0
            && previousViewport.height > 0
            && (previousViewport.width !== width || previousViewport.height !== height);
          lastViewportSizeRef.current = { width, height };

          if (viewportChanged) {
            if (autoFocus) {
              lastAutoFocusKeyRef.current = null;
              applyAutoFocusFromJs();
            } else {
              resetFromJs();
            }
            return;
          }

          applyAutoFocusFromJs();
        }}
      >
        <Animated.View style={animatedStyle}>
          <View
            onLayout={(event) => {
              const { width, height } = event.nativeEvent.layout;
              contentWidth.value = width;
              contentHeight.value = height;
              contentSizeRef.current = { width, height };
              applyAutoFocusFromJs();
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
