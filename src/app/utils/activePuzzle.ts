import type { ActiveSession } from '../shell/activeSessionTypes';
import type { Difficulty } from '../types';
import { getGameDefinition } from '../shell/games/gameRegistry';
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

export function getActivePuzzleDifficulty(activeSession: ActiveSession): Difficulty {
  return activeSession.puzzle.difficulty;
}

export function getActivePuzzleDimensions(activeSession: ActiveSession): PuzzleDimensions {
  return {
    rows: activeSession.puzzle.rows,
    cols: activeSession.puzzle.cols,
  };
}

export function getActivePuzzleDisplay(activeSession: ActiveSession): ActivePuzzleDisplay {
  const difficulty = formatDifficultyLabel(
    activeSession.gameId,
    getActivePuzzleDifficulty(activeSession),
  );
  const elapsed = formatElapsed(activeSession.elapsedSeconds);
  const { rows, cols } = getActivePuzzleDimensions(activeSession);
  const definition = getGameDefinition(activeSession.gameId);

  return {
    label: definition.shortTitle,
    meta: [
      difficulty,
      `${rows}×${cols}`,
      elapsed,
    ],
  };
}
