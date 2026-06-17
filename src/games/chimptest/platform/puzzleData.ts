import type { PuzzleDifficulty } from '../../shared/types';
import type { ChimpTestCell, ChimpTestPuzzle, ChimpTestSession } from '../types';

const DIFFICULTY_CONFIG: Record<PuzzleDifficulty, { gridSize: number; startCount: number; maxCount: number }> = {
  easy:   { gridSize: 4, startCount: 3, maxCount: 7 },
  medium: { gridSize: 5, startCount: 4, maxCount: 9 },
  hard:   { gridSize: 6, startCount: 5, maxCount: 11 },
  expert: { gridSize: 7, startCount: 6, maxCount: 13 },
};

export function generateChimpTestCells(count: number, gridSize: number): ChimpTestCell[] {
  const positions: { row: number; col: number }[] = [];
  for (let r = 0; r < gridSize; r++) {
    for (let c = 0; c < gridSize; c++) {
      positions.push({ row: r, col: c });
    }
  }

  for (let i = positions.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    const tmp = positions[i]!;
    positions[i] = positions[j]!;
    positions[j] = tmp;
  }

  return positions.slice(0, count).map((pos, index) => ({
    number: index + 1,
    row: pos.row,
    col: pos.col,
    hidden: false,
  }));
}

export function createChimpTestSession(difficulty: PuzzleDifficulty): ChimpTestSession {
  const config = DIFFICULTY_CONFIG[difficulty];
  const puzzle: ChimpTestPuzzle = {
    id: `${difficulty}-${Date.now()}`,
    difficulty,
    gridSize: config.gridSize,
    startCount: config.startCount,
    maxCount: config.maxCount,
  };

  return {
    puzzle,
    currentCount: config.startCount,
    cells: generateChimpTestCells(config.startCount, config.gridSize),
    nextExpected: 1,
    revealAll: false,
    wrongTapCell: null,
    roundTimes: [],
    roundStartElapsed: 0,
    status: 'playing',
  };
}
