import { getCurrentLanguage } from '../../../../app/i18n';
import { resolveGameContent, type LocalizedGameContent } from '../../../../app/i18n/gameContent';
import type { HowToPlayContent } from '../../../../app/shell/games/howToPlayContent';
import type { LossContent } from '../../../../app/shell/games/lossContent';
import type { Difficulty } from '../../../../app/types';
import type { SudokuTechnique } from '../../gameplay/analysis';
import en from './en';
import nl from './nl';
import de from './de';
import fr from './fr';
import es from './es';

export type SudokuTutorialLessonKey =
  | 'goal'
  | 'naked-single'
  | 'notes-mode'
  | 'hidden-single';

export interface SudokuTutorialLessonCopy {
  title: string;
  body: string;
  summary: string;
  controlHint: string;
  continueLabel?: string;
  prompt?: string;
  options?: Record<string, string>;
  correctOptionKey?: string;
  correctFeedback?: string;
  wrongFeedback?: string;
}

export interface SudokuStrings {
  title: string;
  shortTitle: string;
  tagline: string;
  difficultyLabels: Record<Difficulty, string>;
  difficultyDescriptions: Record<Difficulty, string>;
  play: {
    metadataLabels: {
      size: string;
      difficulty: string;
      filled: string;
    };
    noPuzzlesDialog: {
      title: string;
      message(difficultyLabel: string): string;
    };
    cellLabel: string;
    resetZoom: string;
    helperToggle: {
      show: string;
      hide: string;
    };
    controls: {
      noteModeEnabled: string;
      noteModeDisabled: string;
      selectedCellPrompt: string;
      selectedCellLabel(cellLabel: string): string;
      digitButtonLabel(digit: number): string;
      noteDigitLabel(digit: number): string;
    };
    nextMove: {
      invalidConflictTitle: string;
      invalidConflictBody(houseLabel: string, digit: number): string;
      invalidDeadCellTitle: string;
      invalidDeadCellBody(cellLabel: string): string;
      placementTitle(techniqueLabel: string, digit: number): string;
      nakedSingleBody(digit: number, cellLabel: string): string;
      hiddenSingleBody(digit: number, houseLabel: string, cellLabel: string): string;
      placementBody(techniqueLabel: string, digit: number, cellLabel: string, houseLabels: string): string;
      eliminationTitle(techniqueLabel: string, digitsLabel: string): string;
      lockedCandidatesBody(digitsLabel: string, sourceHouseLabel: string, targetHouseLabel: string): string;
      eliminationBody(techniqueLabel: string, digitsLabel: string, targetLabels: string, houseLabels: string): string;
      unsupportedTitle: string;
      unsupportedBody: string;
    };
  };
  tutorial: {
    exitLabel: {
      skip: string;
      end: string;
    };
    controlLabel: string;
    progressLabel(current: number, total: number): string;
    status: {
      nextLesson: string;
      finishing: string;
    };
    lessons: Record<SudokuTutorialLessonKey, SudokuTutorialLessonCopy>;
  };
  learning: {
    labels: {
      cell(row: number, col: number): string;
      row(index: number): string;
      column(index: number): string;
      box(index: number): string;
      joinList(items: string[]): string;
    };
    techniqueLabels: Record<SudokuTechnique, string>;
    analyzer: {
      legend: {
        evidence: string;
        place: string;
        eliminate: string;
      };
    };
  };
}

export interface SudokuI18n {
  strings: SudokuStrings;
  howToPlay: HowToPlayContent;
  loss: LossContent;
}

const CONTENT: LocalizedGameContent<SudokuI18n> = {
  en,
  nl,
  de,
  fr,
  es,
};

export function getSudokuI18n(): SudokuI18n {
  return resolveGameContent(getCurrentLanguage(), CONTENT);
}

export function getSudokuStrings(): SudokuStrings {
  return getSudokuI18n().strings;
}

export function getSudokuHowToPlay(): HowToPlayContent {
  return getSudokuI18n().howToPlay;
}

export function getSudokuLossContent(): LossContent {
  return getSudokuI18n().loss;
}

export function getSudokuTutorialLessonCopies(): Record<SudokuTutorialLessonKey, SudokuTutorialLessonCopy> {
  return getSudokuStrings().tutorial.lessons;
}
