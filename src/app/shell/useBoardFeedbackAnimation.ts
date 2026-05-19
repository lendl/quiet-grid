import { useEffect, useMemo, useRef, useState } from 'react';
import { Animated, Easing } from 'react-native';
import type { BoardFeedbackEffect } from './boardFeedback';
import { createBoardFeedbackLookup } from './boardFeedback';

const SPIN_DURATION_MS = 420;
const SHAKE_DURATION_MS = 320;

type UseBoardFeedbackAnimationArgs = {
  effects: readonly BoardFeedbackEffect[] | null | undefined;
  cellSize: number;
};

type ActiveBoardFeedbackState = {
  batchKey: string;
  spinKeys: string[];
  shakeKeys: string[];
};

export type BoardFeedbackAnimationState = {
  activeSpinKeys: Set<string>;
  activeShakeKeys: Set<string>;
  rotate: Animated.AnimatedInterpolation<string>;
  shakeTranslateX: Animated.AnimatedInterpolation<number | string>;
};

export function useBoardFeedbackAnimation({
  effects,
  cellSize,
}: UseBoardFeedbackAnimationArgs): BoardFeedbackAnimationState {
  const rotation = useRef(new Animated.Value(0)).current;
  const shake = useRef(new Animated.Value(0)).current;
  const handledEffectIdsRef = useRef<Set<string>>(new Set());
  const [activeFeedback, setActiveFeedback] = useState<ActiveBoardFeedbackState | null>(null);

  const rotate = useMemo(() => rotation.interpolate({
    inputRange: [0, 1],
    outputRange: ['0deg', '360deg'],
  }), [rotation]);

  const shakeTranslateX = useMemo(() => shake.interpolate({
    inputRange: [-1, 1],
    outputRange: [-cellSize * 0.12, cellSize * 0.12],
  }), [cellSize, shake]);

  const effectIds = useMemo(
    () => (effects ?? []).map((effect) => String(effect.id)).join('|'),
    [effects],
  );

  useEffect(() => {
    if (!effects || effects.length === 0) {
      rotation.stopAnimation();
      rotation.setValue(0);
      shake.stopAnimation();
      shake.setValue(0);
      setActiveFeedback(null);
      handledEffectIdsRef.current.clear();
      return undefined;
    }

    const nextEffects = effects.filter(
      (effect) => !handledEffectIdsRef.current.has(String(effect.id)),
    );

    if (nextEffects.length === 0) {
      return undefined;
    }

    nextEffects.forEach((effect) => {
      handledEffectIdsRef.current.add(String(effect.id));
    });

    const lookup = createBoardFeedbackLookup(nextEffects);
    const spinKeys = Array.from(lookup.entries())
      .filter(([, kind]) => kind === 'spin')
      .map(([key]) => key);
    const shakeKeys = Array.from(lookup.entries())
      .filter(([, kind]) => kind === 'shake')
      .map(([key]) => key);

    if (spinKeys.length === 0 && shakeKeys.length === 0) {
      return undefined;
    }

    const batchKey = nextEffects.map((effect) => String(effect.id)).join('|') || effectIds;

    setActiveFeedback({
      batchKey,
      spinKeys,
      shakeKeys,
    });
    rotation.stopAnimation();
    rotation.setValue(0);
    shake.stopAnimation();
    shake.setValue(0);

    const animations: Animated.CompositeAnimation[] = [];

    if (spinKeys.length > 0) {
      animations.push(Animated.timing(rotation, {
        toValue: 1,
        duration: SPIN_DURATION_MS,
        easing: Easing.inOut(Easing.ease),
        useNativeDriver: true,
      }));
    }

    if (shakeKeys.length > 0) {
      animations.push(Animated.sequence([
        Animated.timing(shake, { toValue: -1, duration: 45, useNativeDriver: true }),
        Animated.timing(shake, { toValue: 1, duration: 55, useNativeDriver: true }),
        Animated.timing(shake, { toValue: -0.85, duration: 55, useNativeDriver: true }),
        Animated.timing(shake, { toValue: 0.85, duration: 55, useNativeDriver: true }),
        Animated.timing(shake, {
          toValue: 0,
          duration: SHAKE_DURATION_MS - 210,
          useNativeDriver: true,
        }),
      ]));
    }

    const animation = animations.length === 1
      ? animations[0]
      : Animated.parallel(animations, { stopTogether: true });

    animation.start(({ finished }) => {
      rotation.setValue(0);
      shake.setValue(0);

      if (!finished) {
        return;
      }

      setActiveFeedback((current) => (current?.batchKey === batchKey ? null : current));
    });

    return () => {
      animation.stop();
      rotation.stopAnimation();
      rotation.setValue(0);
      shake.stopAnimation();
      shake.setValue(0);

      setActiveFeedback((current) => (current?.batchKey === batchKey ? null : current));
    };
  }, [effectIds, effects, rotation, shake]);

  const activeSpinKeys = useMemo(
    () => new Set(activeFeedback?.spinKeys ?? []),
    [activeFeedback],
  );
  const activeShakeKeys = useMemo(
    () => new Set(activeFeedback?.shakeKeys ?? []),
    [activeFeedback],
  );

  return {
    activeSpinKeys,
    activeShakeKeys,
    rotate,
    shakeTranslateX,
  };
}
