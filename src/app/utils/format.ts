import { getPuzzleDefinition } from '../shell/games/gameRegistry';
import type { Difficulty, PuzzleTypeId } from '../types';
import type { Theme } from '../theme';

export function formatElapsed(seconds: number): string {
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
}

export function formatDifficultyLabel(puzzleTypeId: PuzzleTypeId, difficulty: Difficulty): string {
  return getPuzzleDefinition(puzzleTypeId).content.difficultyLabels[difficulty];
}

export function getDifficultyColor(theme: Theme, difficulty: Difficulty): string {
  if (difficulty === 'easy') return theme.difficultyEasy;
  if (difficulty === 'medium') return theme.difficultyMedium;
  if (difficulty === 'hard') return theme.difficultyHard;
  return theme.difficultyExpert;
}
