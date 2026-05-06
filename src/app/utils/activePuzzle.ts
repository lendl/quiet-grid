import type { ActivePuzzle } from '../shell/activePuzzleTypes';
import type { Difficulty } from '../types';
import { getPuzzleDefinition } from '../shell/games/gameRegistry';
import { formatDifficultyLabel } from './format';
import { formatElapsed } from './formatElapsed';

export type ActivePuzzleDisplay = {
  label: string;
  meta: string[];
};

export type PuzzleDimensions = {
  rows: number;
  cols: number;
};

export function getActivePuzzleDifficulty(activePuzzle: ActivePuzzle): Difficulty {
  return activePuzzle.puzzle.difficulty;
}

export function getActivePuzzleDimensions(activePuzzle: ActivePuzzle): PuzzleDimensions {
  return {
    rows: activePuzzle.puzzle.rows,
    cols: activePuzzle.puzzle.cols,
  };
}

export function getActivePuzzleDisplay(activePuzzle: ActivePuzzle): ActivePuzzleDisplay {
  const difficulty = formatDifficultyLabel(
    activePuzzle.puzzleTypeId,
    getActivePuzzleDifficulty(activePuzzle),
  );
  const elapsed = formatElapsed(activePuzzle.elapsedSeconds);
  const { rows, cols } = getActivePuzzleDimensions(activePuzzle);
  const definition = getPuzzleDefinition(activePuzzle.puzzleTypeId);

  return {
    label: definition.shortTitle,
    meta: [
      difficulty,
      `${rows}×${cols}`,
      elapsed,
    ],
  };
}
