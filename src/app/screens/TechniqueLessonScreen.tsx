import React, { useMemo, useState } from 'react';
import type { LayoutChangeEvent } from 'react-native';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import Ionicons from '@react-native-vector-icons/ionicons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import type { StackScreenProps } from '@react-navigation/stack';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import type { RootStackParamList } from '../navigation/types';
import type { Theme } from '../theme';
import { withAlpha } from '../utils/color';
import SudokuPuzzleGrid from '../../games/sudoku/ui/play/components/SudokuPuzzleGrid';
import { buildSudokuCandidateGrid } from '../../games/sudoku/gameplay/analysis/candidates';
import { getSudokuStrings } from '../../games/sudoku/content/strings';
import type { SudokuBoard, SudokuNotes } from '../../games/sudoku/types';
import { createEmptySudokuNotes } from '../../games/sudoku/types';
import type { SudokuHintTargetCell } from '../../games/sudoku/gameplay/analysis/nextMove';

type Props = StackScreenProps<RootStackParamList, 'TechniqueLesson'>;

function buildNotesFromBoard(board: SudokuBoard, givens: SudokuBoard): SudokuNotes {
  const notes = createEmptySudokuNotes(9, 9);
  const candidateGrid = buildSudokuCandidateGrid({ board, givens });

  candidateGrid.forEach((row) => {
    row.forEach((cell) => {
      if (cell.value !== null) {
        return;
      }
      cell.logicalCandidates.forEach((digit) => {
        notes[cell.row][cell.col][digit - 1] = true;
      });
    });
  });

  return notes;
}

export default function TechniqueLessonScreen({ navigation, route }: Props) {
  const { resolvedLanguage } = useLanguage();
  const { theme, isDark } = useTheme();
  const insets = useSafeAreaInsets();
  const styles = useMemo(() => makeStyles(theme, isDark, insets.top, insets.bottom), [theme, isDark, insets.top, insets.bottom]);
  const strings = useMemo(() => getSudokuStrings(), [resolvedLanguage]);
  const { ruleKey, title, board, givens, finishedCells, evidenceCells, targetCells, highlightRows, highlightCols, highlightBoxes } = route.params;
  const [boardBounds, setBoardBounds] = useState({ width: 0, height: 0 });

  const typedBoard = board as SudokuBoard;
  const typedGivens = givens as SudokuBoard;
  const typedFinishedCells = finishedCells as boolean[][];
  const typedTargetCells = targetCells as SudokuHintTargetCell[];

  const notes = useMemo(() => buildNotesFromBoard(typedBoard, typedGivens), [typedBoard, typedGivens]);

  const explanation = (strings.play.techniqueLesson.explanations as Record<string, string>)[ruleKey] ?? null;
  const paragraphs = explanation ? explanation.split('\n\n') : [];

  const handleBoardLayout = (event: LayoutChangeEvent) => {
    const { width, height } = event.nativeEvent.layout;
    setBoardBounds({
      width: Math.max(0, width - 12),
      height: Math.max(0, height - 10),
    });
  };

  return (
    <View style={[styles.root, { paddingTop: insets.top }]}>
      <View style={styles.topBar}>
        <TouchableOpacity
          accessibilityRole="button"
          accessibilityLabel={strings.play.techniqueLesson.backButton}
          onPress={() => navigation.goBack()}
          style={styles.backButton}
          activeOpacity={0.82}
        >
          <Ionicons name="arrow-back" size={20} color={theme.text} />
        </TouchableOpacity>
        <Text style={styles.topBarTitle} numberOfLines={1}>
          {title}
        </Text>
      </View>

      <ScrollView
        style={styles.scroll}
        contentContainerStyle={[styles.scrollContent, { paddingBottom: insets.bottom + 24 }]}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.boardHost} onLayout={handleBoardLayout}>
          {boardBounds.width > 0 ? (
            <SudokuPuzzleGrid
              board={typedBoard}
              givens={typedGivens}
              notes={notes}
              finishedCells={typedFinishedCells}
              selectedCell={null}
              validatedUnitKeys={[]}
              penalizedUnitKeys={[]}
              boardFeedbackEffects={null}
              interactive={false}
              nextMoveEvidenceCells={evidenceCells}
              nextMoveTargetCells={typedTargetCells}
              nextMoveHighlightRows={highlightRows}
              nextMoveHighlightCols={highlightCols}
              nextMoveHighlightBoxes={highlightBoxes}
              containerWidth={boardBounds.width}
              containerHeight={boardBounds.height}
              onCellPress={() => {}}
            />
          ) : null}
        </View>

        <View style={styles.legend}>
          <View style={[styles.legendPill, { backgroundColor: withAlpha(theme.primary, isDark ? 0.16 : 0.08), borderColor: withAlpha(theme.primary, isDark ? 0.5 : 0.36) }]}>
            <View style={[styles.legendDot, { backgroundColor: theme.primary }]} />
            <Text style={[styles.legendText, { color: theme.textSecondary }]}>Evidence</Text>
          </View>
          <View style={[styles.legendPill, { backgroundColor: withAlpha(theme.success, isDark ? 0.22 : 0.12), borderColor: withAlpha(theme.success, isDark ? 0.58 : 0.4) }]}>
            <View style={[styles.legendDot, { backgroundColor: theme.success }]} />
            <Text style={[styles.legendText, { color: theme.textSecondary }]}>Place digit</Text>
          </View>
          <View style={[styles.legendPill, { backgroundColor: withAlpha(theme.difficultyHard, isDark ? 0.22 : 0.12), borderColor: withAlpha(theme.difficultyHard, isDark ? 0.58 : 0.4) }]}>
            <View style={[styles.legendDot, { backgroundColor: theme.difficultyHard }]} />
            <Text style={[styles.legendText, { color: theme.textSecondary }]}>Clear note</Text>
          </View>
        </View>

        {paragraphs.length > 0 ? (
          <View style={styles.explanationCard}>
            {paragraphs.map((paragraph, index) => (
              <Text
                key={index}
                style={[styles.paragraph, index > 0 && styles.paragraphSpaced, { color: theme.text }]}
              >
                {paragraph}
              </Text>
            ))}
          </View>
        ) : null}

        <TouchableOpacity
          accessibilityRole="button"
          activeOpacity={0.82}
          onPress={() => navigation.goBack()}
          style={[styles.backCta, { backgroundColor: theme.primary }]}
        >
          <Text style={[styles.backCtaText, { color: theme.onPrimary }]}>
            {strings.play.techniqueLesson.backButton}
          </Text>
        </TouchableOpacity>
      </ScrollView>
    </View>
  );
}

const makeStyles = (theme: Theme, isDark: boolean, topInset: number, _bottomInset: number) => StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: theme.background,
  },
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 10,
    gap: 10,
    borderBottomWidth: 1,
    borderBottomColor: withAlpha(theme.border, isDark ? 0.44 : 0.3),
  },
  backButton: {
    width: 36,
    height: 36,
    alignItems: 'center',
    justifyContent: 'center',
  },
  topBarTitle: {
    flex: 1,
    fontSize: 16,
    fontWeight: '800',
    color: theme.text,
  },
  scroll: {
    flex: 1,
  },
  scrollContent: {
    gap: 16,
    paddingHorizontal: 16,
    paddingTop: 16,
  },
  boardHost: {
    width: '100%',
    aspectRatio: 1,
  },
  legend: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    justifyContent: 'center',
  },
  legendPill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
  },
  legendDot: {
    width: 10,
    height: 10,
    borderRadius: 999,
  },
  legendText: {
    fontSize: 12,
    fontWeight: '700',
  },
  explanationCard: {
    backgroundColor: withAlpha(theme.surfaceElevated, 0.96),
    borderRadius: 16,
    borderWidth: 1,
    borderColor: withAlpha(theme.border, isDark ? 0.44 : 0.3),
    paddingHorizontal: 16,
    paddingVertical: 16,
  },
  paragraph: {
    fontSize: 15,
    lineHeight: 23,
  },
  paragraphSpaced: {
    marginTop: 14,
  },
  backCta: {
    minHeight: 52,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
  },
  backCtaText: {
    fontSize: 16,
    fontWeight: '800',
  },
});
