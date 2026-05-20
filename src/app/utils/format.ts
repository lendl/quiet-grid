import { getGameDefinition } from '../shell/games/gameRegistry';
import type { Difficulty, GameId } from '../types';
import type { Theme } from '../theme';

export function formatDifficultyLabel(puzzleTypeId: GameId, difficulty: Difficulty): string {
  return getGameDefinition(puzzleTypeId).content.difficultyLabels[difficulty];
}

export function getDifficultyColor(theme: Theme, difficulty: Difficulty): string {
  if (difficulty === 'easy') return theme.difficultyEasy;
  if (difficulty === 'medium') return theme.difficultyMedium;
  if (difficulty === 'hard') return theme.difficultyHard;
  return theme.difficultyExpert;
}
