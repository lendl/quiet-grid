import React, { useMemo } from 'react';
import { ScrollView, StyleSheet, View, useWindowDimensions, type StyleProp, type ViewStyle } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

const OUTCOME_FOOTER_CLEARANCE = 72;

export type OutcomeScreenMetrics = {
  accentScale: number;
  contentMaxWidth: number;
  contentPaddingHorizontal: number;
  contentPaddingVertical: number;
  contentGap: number;
  heroStageHeight: number;
  heroClusterWidth: number;
  heroClusterHeight: number;
  heroIconMarginTop: number;
  heroIconSize: number;
  heroIconRadius: number;
  heroSymbolSize: number;
  copyMarginTop: number;
  titleFontSize: number;
  titleLineHeight: number;
  bodyMarginTop: number;
  bodyFontSize: number;
  bodyLineHeight: number;
  difficultyMarginTop: number;
  scoreSectionMarginTop: number;
  scoreValueMarginTop: number;
  scoreFontSize: number;
  scoreLineHeight: number;
  metaRowMarginTop: number;
  metaValueFontSize: number;
  metaDividerHeight: number;
  actionMarginTop: number;
  primaryActionMarginTop: number;
};

const LARGE_LAYOUT: OutcomeScreenMetrics = {
  accentScale: 1,
  contentMaxWidth: 440,
  contentPaddingHorizontal: 20,
  contentPaddingVertical: 24,
  contentGap: 20,
  heroStageHeight: 318,
  heroClusterWidth: 280,
  heroClusterHeight: 240,
  heroIconMarginTop: 78,
  heroIconSize: 98,
  heroIconRadius: 30,
  heroSymbolSize: 46,
  copyMarginTop: -10,
  titleFontSize: 31,
  titleLineHeight: 36,
  bodyMarginTop: 10,
  bodyFontSize: 15,
  bodyLineHeight: 22,
  difficultyMarginTop: 8,
  scoreSectionMarginTop: 20,
  scoreValueMarginTop: 8,
  scoreFontSize: 70,
  scoreLineHeight: 74,
  metaRowMarginTop: 14,
  metaValueFontSize: 22,
  metaDividerHeight: 30,
  actionMarginTop: 28,
  primaryActionMarginTop: 28,
};

const MEDIUM_LAYOUT: OutcomeScreenMetrics = {
  accentScale: 0.9,
  contentMaxWidth: 420,
  contentPaddingHorizontal: 20,
  contentPaddingVertical: 20,
  contentGap: 18,
  heroStageHeight: 280,
  heroClusterWidth: 252,
  heroClusterHeight: 216,
  heroIconMarginTop: 68,
  heroIconSize: 88,
  heroIconRadius: 28,
  heroSymbolSize: 42,
  copyMarginTop: -6,
  titleFontSize: 28,
  titleLineHeight: 33,
  bodyMarginTop: 8,
  bodyFontSize: 15,
  bodyLineHeight: 21,
  difficultyMarginTop: 7,
  scoreSectionMarginTop: 12,
  scoreValueMarginTop: 6,
  scoreFontSize: 58,
  scoreLineHeight: 62,
  metaRowMarginTop: 12,
  metaValueFontSize: 20,
  metaDividerHeight: 28,
  actionMarginTop: 22,
  primaryActionMarginTop: 22,
};

const SMALL_LAYOUT: OutcomeScreenMetrics = {
  accentScale: 0.8,
  contentMaxWidth: 400,
  contentPaddingHorizontal: 18,
  contentPaddingVertical: 16,
  contentGap: 14,
  heroStageHeight: 236,
  heroClusterWidth: 224,
  heroClusterHeight: 184,
  heroIconMarginTop: 54,
  heroIconSize: 76,
  heroIconRadius: 24,
  heroSymbolSize: 36,
  copyMarginTop: 0,
  titleFontSize: 24,
  titleLineHeight: 29,
  bodyMarginTop: 6,
  bodyFontSize: 14,
  bodyLineHeight: 20,
  difficultyMarginTop: 6,
  scoreSectionMarginTop: 8,
  scoreValueMarginTop: 4,
  scoreFontSize: 46,
  scoreLineHeight: 50,
  metaRowMarginTop: 10,
  metaValueFontSize: 18,
  metaDividerHeight: 24,
  actionMarginTop: 18,
  primaryActionMarginTop: 18,
};

function getOutcomeScreenMetrics(windowHeight: number): OutcomeScreenMetrics {
  if (windowHeight <= 700) {
    return SMALL_LAYOUT;
  }

  if (windowHeight <= 820) {
    return MEDIUM_LAYOUT;
  }

  return LARGE_LAYOUT;
}

type Props = {
  children: (metrics: OutcomeScreenMetrics) => React.ReactNode;
  contentStyle?: StyleProp<ViewStyle>;
};

export default function OutcomeScreenLayout({ children, contentStyle }: Props) {
  const { height: windowHeight } = useWindowDimensions();
  const insets = useSafeAreaInsets();
  const metrics = useMemo(() => getOutcomeScreenMetrics(windowHeight), [windowHeight]);

  return (
    <ScrollView
      style={styles.scrollView}
      contentContainerStyle={[
        styles.scrollContent,
        {
          paddingBottom: OUTCOME_FOOTER_CLEARANCE + insets.bottom,
        },
      ]}
      showsVerticalScrollIndicator={false}
    >
      <View
        style={[
          styles.content,
          {
            maxWidth: metrics.contentMaxWidth,
            paddingHorizontal: metrics.contentPaddingHorizontal,
            paddingVertical: metrics.contentPaddingVertical,
          },
          contentStyle,
        ]}
      >
        {children(metrics)}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
  },
  content: {
    position: 'relative',
    width: '100%',
    alignSelf: 'center',
  },
});
