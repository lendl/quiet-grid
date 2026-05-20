import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  type GestureResponderEvent,
  type LayoutChangeEvent,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { StackActions } from '@react-navigation/native';
import type { StackScreenProps } from '@react-navigation/stack';
import { Gesture, GestureDetector } from 'react-native-gesture-handler';
import GamePageShell from '../components/GamePageShell';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import type { RootStackParamList } from '../navigation/types';
import type { Theme } from '../theme';
import { getGameAnalysisAdapter } from '../analysisRegistry';

type Props = StackScreenProps<RootStackParamList, 'Analysis'>;
const SWIPE_DISTANCE_THRESHOLD = 48;
const SWIPE_VELOCITY_THRESHOLD = 420;

function clampIndex(stepIndex: number, stepCount: number): number {
  if (stepCount <= 1) {
    return 0;
  }
  return Math.min(Math.max(stepIndex, 0), stepCount - 1);
}

export default function PuzzleAnalysisScreen({ navigation, route }: Props) {
  const { strings } = useLanguage();
  const { theme } = useTheme();
  const s = useMemo(() => makeStyles(theme), [theme]);
  const { analysis } = route.params;
  const adapter = getGameAnalysisAdapter(analysis.gameId);
  const [stepIndex, setStepIndex] = useState(0);
  const [boardWidth, setBoardWidth] = useState(0);
  const [scrubberWidth, setScrubberWidth] = useState(0);
  const stepCount = analysis.steps.length;
  const currentStep = analysis.steps[stepIndex];
  const scrubberProgress = stepCount <= 1 ? 1 : stepIndex / (stepCount - 1);

  const handleBoardLayout = useCallback((event: LayoutChangeEvent) => {
    setBoardWidth(event.nativeEvent.layout.width);
  }, []);

  const handleScrubberTrackLayout = useCallback((event: LayoutChangeEvent) => {
    setScrubberWidth(event.nativeEvent.layout.width);
  }, []);

  const goToPreviousStep = useCallback(() => {
    setStepIndex((current) => clampIndex(current - 1, stepCount));
  }, [stepCount]);

  const goToNextStep = useCallback(() => {
    setStepIndex((current) => clampIndex(current + 1, stepCount));
  }, [stepCount]);

  const returnToPuzzle = useCallback(() => {
    navigation.dispatch(StackActions.replace('Game', { gameId: analysis.gameId }));
  }, [analysis.gameId, navigation]);

  const updateStepFromLocation = useCallback((locationX: number, width: number) => {
    if (stepCount <= 1 || width <= 0) {
      return;
    }
    const ratio = Math.min(Math.max(locationX / width, 0), 1);
    setStepIndex(Math.round(ratio * (stepCount - 1)));
  }, [stepCount]);

  const handleScrubberLayout = useCallback((event: GestureResponderEvent) => {
    updateStepFromLocation(event.nativeEvent.locationX, scrubberWidth);
  }, [scrubberWidth, updateStepFromLocation]);
  const swipeGesture = useMemo(() => Gesture.Pan()
    .runOnJS(true)
    .activeOffsetX([-16, 16])
    .failOffsetY([-20, 20])
    .onEnd((event) => {
      const shouldGoNext = event.translationX <= -SWIPE_DISTANCE_THRESHOLD
        || event.velocityX <= -SWIPE_VELOCITY_THRESHOLD;
      const shouldGoPrevious = event.translationX >= SWIPE_DISTANCE_THRESHOLD
        || event.velocityX >= SWIPE_VELOCITY_THRESHOLD;

      if (shouldGoNext && stepIndex < stepCount - 1) {
        goToNextStep();
        return;
      }

      if (shouldGoPrevious && stepIndex > 0) {
        goToPreviousStep();
      }
    }), [goToNextStep, goToPreviousStep, stepCount, stepIndex]);

  const boardSize = boardWidth > 0 ? Math.min(boardWidth, 420) : 0;

  useEffect(() => {
    if (!adapter || !currentStep) {
      returnToPuzzle();
    }
  }, [adapter, currentStep, returnToPuzzle]);

  if (!adapter || !currentStep) {
    return null;
  }

  return (
    <GamePageShell activeTab="Games" headerMode="back" backToPuzzleTypeId={analysis.gameId}>
      <GestureDetector gesture={swipeGesture}>
        <ScrollView contentContainerStyle={s.scroll} showsVerticalScrollIndicator={false}>
          <View style={s.header}>
            <Text style={s.stepCounter}>{strings.analysis.step(stepIndex + 1, stepCount)}</Text>
          </View>

          <View style={s.boardSection} onLayout={handleBoardLayout}>
            {boardSize > 0 ? adapter.renderAnalysisStep({
              analysis,
              stepIndex,
              containerWidth: boardSize,
              containerHeight: boardSize,
            }) : null}
          </View>

          <View style={s.card}>
            <Text style={s.cardTitle}>{currentStep.title}</Text>
            <Text style={s.cardBody}>{currentStep.body}</Text>
          </View>

          <View style={s.scrubberSection}>
            <Text style={s.scrubberLabel}>{strings.analysis.fastJump}</Text>
            <View
              style={s.scrubberTrack}
              onLayout={handleScrubberTrackLayout}
              onStartShouldSetResponder={() => true}
              onResponderGrant={handleScrubberLayout}
              onResponderMove={handleScrubberLayout}
            >
              <View style={s.scrubberTrackBase} />
              <View style={[s.scrubberFill, { width: `${scrubberProgress * 100}%` }]} />
              <View style={[s.scrubberThumb, { left: `${scrubberProgress * 100}%` }]} />
            </View>
          </View>

          <View style={s.controls}>
            <TouchableOpacity
              style={[s.controlButton, stepIndex === 0 ? s.controlButtonDisabled : null]}
              onPress={goToPreviousStep}
              disabled={stepIndex === 0}
              activeOpacity={0.82}
            >
              <Text style={s.controlButtonText}>{strings.analysis.previous}</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[s.controlButton, stepIndex === stepCount - 1 ? s.controlButtonDisabled : null]}
              onPress={goToNextStep}
              disabled={stepIndex === stepCount - 1}
              activeOpacity={0.82}
            >
              <Text style={s.controlButtonText}>{strings.analysis.next}</Text>
            </TouchableOpacity>
          </View>
        </ScrollView>
      </GestureDetector>
    </GamePageShell>
  );
}

const makeStyles = (theme: Theme) => StyleSheet.create({
  scroll: {
    padding: 20,
    gap: 20,
  },
  header: {
    gap: 6,
  },
  stepCounter: {
    fontSize: 13,
    fontWeight: '800',
    letterSpacing: 0.8,
    textTransform: 'uppercase',
    color: theme.textMuted,
  },
  boardSection: {
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 260,
  },
  card: {
    gap: 10,
    padding: 18,
    borderRadius: 22,
    backgroundColor: theme.surface,
    borderWidth: 1,
    borderColor: theme.border,
  },
  cardTitle: {
    fontSize: 19,
    fontWeight: '800',
    color: theme.text,
  },
  cardBody: {
    fontSize: 15,
    lineHeight: 23,
    color: theme.textSecondary,
  },
  scrubberSection: {
    gap: 10,
  },
  scrubberLabel: {
    fontSize: 13,
    fontWeight: '800',
    letterSpacing: 0.8,
    textTransform: 'uppercase',
    color: theme.textMuted,
  },
  scrubberTrack: {
    position: 'relative',
    height: 36,
    justifyContent: 'center',
  },
  scrubberTrackBase: {
    position: 'absolute',
    left: 0,
    right: 0,
    top: 16,
    height: 4,
    borderRadius: 999,
    backgroundColor: theme.border,
  },
  scrubberFill: {
    position: 'absolute',
    left: 0,
    top: 16,
    height: 4,
    borderRadius: 999,
    backgroundColor: theme.primary,
  },
  scrubberThumb: {
    position: 'absolute',
    top: 9,
    width: 18,
    height: 18,
    marginLeft: -9,
    borderRadius: 999,
    backgroundColor: theme.primary,
    borderWidth: 2,
    borderColor: theme.background,
  },
  controls: {
    flexDirection: 'row',
    gap: 12,
  },
  controlButton: {
    flex: 1,
    minHeight: 52,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 18,
    backgroundColor: theme.surface,
    borderWidth: 1,
    borderColor: theme.border,
  },
  controlButtonDisabled: {
    opacity: 0.45,
  },
  controlButtonText: {
    fontSize: 15,
    fontWeight: '800',
    color: theme.text,
  },
});
