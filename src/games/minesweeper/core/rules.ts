import type {
  MinesweeperBoard,
  MinesweeperCell,
  MinesweeperPuzzle,
} from './types';
import type { PuzzleDifficulty } from '../../shared/types';
import {
  getMinesweeperSizeProfile,
  resolveMinesweeperSizeProfile,
} from './sizeProfiles';

export type MinesweeperDistributionMode =
  | 'cluster-limited'
  | 'mostly-uniform'
  | 'random'
  | 'clustered';

export interface MinesweeperConfig {
  rows: number;
  cols: number;
  densityMin: number;
  densityMax: number;
  targetDensity: number;
  distribution: MinesweeperDistributionMode;
  openingMinRatio: number | null;
  openingMaxRatio: number | null;
  protectNeighbors: boolean;
  retryLimit: number;
}

type MinesweeperDifficultyRules = Omit<MinesweeperConfig, 'rows' | 'cols'>;

const MINESWEEPER_DIFFICULTY_RULES: Record<PuzzleDifficulty, MinesweeperDifficultyRules> = {
  easy: {
    densityMin: 0.10,
    densityMax: 0.13,
    targetDensity: 0.12,
    distribution: 'cluster-limited',
    openingMinRatio: 0.18,
    openingMaxRatio: null,
    protectNeighbors: true,
    retryLimit: 500,
  },
  medium: {
    densityMin: 0.15,
    densityMax: 0.18,
    targetDensity: 0.165,
    distribution: 'mostly-uniform',
    openingMinRatio: 0.10,
    openingMaxRatio: 0.18,
    protectNeighbors: false,
    retryLimit: 500,
  },
  hard: {
    densityMin: 0.18,
    densityMax: 0.22,
    targetDensity: 0.20,
    distribution: 'random',
    openingMinRatio: 0.04,
    openingMaxRatio: 0.10,
    protectNeighbors: false,
    retryLimit: 700,
  },
  expert: {
    densityMin: 0.20,
    densityMax: 0.25,
    targetDensity: 0.21,
    distribution: 'clustered',
    openingMinRatio: 0.03,
    openingMaxRatio: null,
    protectNeighbors: true,
    retryLimit: 120,
  },
};

function buildMinesweeperConfig(
  difficulty: PuzzleDifficulty,
  rows: number,
  cols: number,
): MinesweeperConfig {
  const rules = MINESWEEPER_DIFFICULTY_RULES[difficulty];
  if (!rules) {
    throw new Error(`Unsupported Minesweeper difficulty: ${difficulty}`);
  }

  return {
    ...rules,
    rows,
    cols,
  };
}

function createCell(isMine: boolean): MinesweeperCell {
  return {
    isMine,
    adjacentMines: 0,
    state: 'hidden',
  };
}

function cloneBoard(board: MinesweeperBoard): MinesweeperBoard {
  return {
    ...board,
    cells: board.cells.map((row) => row.map((cell) => ({ ...cell }))),
  };
}

function isInBounds(board: MinesweeperBoard, row: number, col: number): boolean {
  return row >= 0 && row < board.rows && col >= 0 && col < board.cols;
}

function getNeighborCoords(board: MinesweeperBoard, row: number, col: number): Array<[number, number]> {
  const neighbors: Array<[number, number]> = [];

  for (let rowOffset = -1; rowOffset <= 1; rowOffset++) {
    for (let colOffset = -1; colOffset <= 1; colOffset++) {
      if (rowOffset === 0 && colOffset === 0) continue;
      const nextRow = row + rowOffset;
      const nextCol = col + colOffset;
      if (isInBounds(board, nextRow, nextCol)) {
        neighbors.push([nextRow, nextCol]);
      }
    }
  }

  return neighbors;
}

function countAdjacentMines(board: MinesweeperBoard, row: number, col: number): number {
  return getNeighborCoords(board, row, col)
    .filter(([nextRow, nextCol]) => board.cells[nextRow][nextCol].isMine)
    .length;
}

function revealAllMines(board: MinesweeperBoard): void {
  for (const row of board.cells) {
    for (const cell of row) {
      if (cell.isMine) {
        cell.state = 'revealed';
      }
    }
  }
}

function computeStatus(board: MinesweeperBoard): MinesweeperBoard['status'] {
  for (const row of board.cells) {
    for (const cell of row) {
      if (!cell.isMine && cell.state !== 'revealed') {
        return 'playing';
      }
    }
  }

  return 'won';
}

function revealSafeArea(board: MinesweeperBoard, startRow: number, startCol: number): void {
  const queue: Array<[number, number]> = [[startRow, startCol]];
  const seen = new Set<string>();

  while (queue.length > 0) {
    const [row, col] = queue.shift()!;
    const key = `${row},${col}`;
    if (seen.has(key)) continue;
    seen.add(key);

    const cell = board.cells[row][col];
    if (cell.state === 'flagged' || cell.state === 'revealed' || cell.isMine) {
      continue;
    }

    cell.state = 'revealed';

    if (cell.adjacentMines !== 0) {
      continue;
    }

    for (const [nextRow, nextCol] of getNeighborCoords(board, row, col)) {
      const neighbor = board.cells[nextRow][nextCol];
      if (!neighbor.isMine && neighbor.state !== 'revealed') {
        queue.push([nextRow, nextCol]);
      }
    }
  }
}

function countRevealedCells(board: MinesweeperBoard): number {
  return board.cells.flat().filter((cell) => cell.state === 'revealed').length;
}

function countAdjacentMinePairs(board: MinesweeperBoard): number {
  let pairs = 0;

  for (let row = 0; row < board.rows; row++) {
    for (let col = 0; col < board.cols; col++) {
      if (!board.cells[row][col].isMine) continue;
      for (const [nextRow, nextCol] of getNeighborCoords(board, row, col)) {
        if (nextRow < row || (nextRow === row && nextCol <= col)) continue;
        if (board.cells[nextRow][nextCol].isMine) {
          pairs++;
        }
      }
    }
  }

  return pairs;
}

function getMaxWindowMineCount(board: MinesweeperBoard, windowRows: number, windowCols: number): number {
  let maxCount = 0;

  for (let row = 0; row <= board.rows - windowRows; row++) {
    for (let col = 0; col <= board.cols - windowCols; col++) {
      let count = 0;
      for (let rowOffset = 0; rowOffset < windowRows; rowOffset++) {
        for (let colOffset = 0; colOffset < windowCols; colOffset++) {
          if (board.cells[row + rowOffset][col + colOffset].isMine) {
            count++;
          }
        }
      }
      maxCount = Math.max(maxCount, count);
    }
  }

  return maxCount;
}

function getMineCount(config: MinesweeperConfig): number {
  const cellCount = config.rows * config.cols;
  const minCount = Math.ceil(cellCount * config.densityMin);
  const maxCount = Math.floor(cellCount * config.densityMax);
  const targetCount = Math.round(cellCount * config.targetDensity);
  return Math.min(maxCount, Math.max(minCount, targetCount));
}

function getProtectedCoords(
  rows: number,
  cols: number,
  targetRow: number,
  targetCol: number,
  protectNeighbors: boolean,
): Set<number> {
  const protectedIndexes = new Set<number>();

  for (let rowOffset = -1; rowOffset <= 1; rowOffset++) {
    for (let colOffset = -1; colOffset <= 1; colOffset++) {
      if (!protectNeighbors && (rowOffset !== 0 || colOffset !== 0)) {
        continue;
      }

      const row = targetRow + rowOffset;
      const col = targetCol + colOffset;
      if (row >= 0 && row < rows && col >= 0 && col < cols) {
        protectedIndexes.add((row * cols) + col);
      }
    }
  }

  return protectedIndexes;
}

function getRandomMineIndexes(
  rows: number,
  cols: number,
  mineCount: number,
  protectedIndexes: Set<number>,
): Set<number> {
  const positions = Array.from({ length: rows * cols }, (_, index) => index)
    .filter((index) => !protectedIndexes.has(index));

  for (let index = positions.length - 1; index > 0; index--) {
    const swapIndex = Math.floor(Math.random() * (index + 1));
    [positions[index], positions[swapIndex]] = [positions[swapIndex], positions[index]];
  }

  return new Set(positions.slice(0, mineCount));
}

function createResolvedBoard(
  config: MinesweeperConfig,
  mineIndexes: Set<number>,
  mineCount: number,
): MinesweeperBoard {
  const board: MinesweeperBoard = {
    rows: config.rows,
    cols: config.cols,
    mines: mineCount,
    generated: true,
    status: 'playing',
    cells: Array.from({ length: config.rows }, (_, row) =>
      Array.from({ length: config.cols }, (_, col) => {
        const flatIndex = row * config.cols + col;
        return createCell(mineIndexes.has(flatIndex));
      }),
    ),
  };

  for (let row = 0; row < board.rows; row++) {
    for (let col = 0; col < board.cols; col++) {
      const cell = board.cells[row][col];
      if (!cell.isMine) {
        cell.adjacentMines = countAdjacentMines(board, row, col);
      }
    }
  }

  return board;
}

function meetsDistributionRules(
  board: MinesweeperBoard,
  config: MinesweeperConfig,
  mineCount: number,
): boolean {
  if (config.distribution === 'random' || config.distribution === 'clustered') {
    return true;
  }

  const adjacentPairs = countAdjacentMinePairs(board);
  const maxWindowCount = getMaxWindowMineCount(board, 3, 3);

  if (config.distribution === 'cluster-limited') {
    return adjacentPairs <= Math.floor(mineCount * 0.7) && maxWindowCount <= 3;
  }

  return adjacentPairs <= Math.floor(mineCount * 1.2) && maxWindowCount <= 4;
}

function getOpeningThresholds(board: MinesweeperBoard, config: MinesweeperConfig): {
  min: number | null;
  max: number | null;
} {
  const cellCount = board.rows * board.cols;
  return {
    min: config.openingMinRatio === null ? null : Math.floor(cellCount * config.openingMinRatio),
    max: config.openingMaxRatio === null ? null : Math.ceil(cellCount * config.openingMaxRatio),
  };
}

function meetsOpeningRules(board: MinesweeperBoard, row: number, col: number, config: MinesweeperConfig): boolean {
  if (config.openingMinRatio === null && config.openingMaxRatio === null) {
    return true;
  }

  const simulatedBoard = cloneBoard(board);
  revealSafeArea(simulatedBoard, row, col);
  const revealedCount = countRevealedCells(simulatedBoard);
  const thresholds = getOpeningThresholds(board, config);

  if (thresholds.min !== null && revealedCount < thresholds.min) {
    return false;
  }
  if (thresholds.max !== null && revealedCount > thresholds.max) {
    return false;
  }

  return true;
}

function countOpeningFrontier(board: MinesweeperBoard): number {
  let frontier = 0;

  for (let row = 0; row < board.rows; row++) {
    for (let col = 0; col < board.cols; col++) {
      const cell = board.cells[row][col];
      if (cell.state !== 'revealed' || cell.adjacentMines === 0) {
        continue;
      }

      const touchesHiddenNeighbor = getNeighborCoords(board, row, col)
        .some(([nextRow, nextCol]) => board.cells[nextRow][nextCol].state === 'hidden');

      if (touchesHiddenNeighbor) {
        frontier += 1;
      }
    }
  }

  return frontier;
}

function meetsOpeningQualityRules(
  board: MinesweeperBoard,
  row: number,
  col: number,
  config: MinesweeperConfig,
): boolean {
  if (config.distribution !== 'clustered') {
    return true;
  }

  const simulatedBoard = cloneBoard(board);
  revealSafeArea(simulatedBoard, row, col);

  const frontier = countOpeningFrontier(simulatedBoard);
  return frontier >= 3;
}

function finalizeGeneratedBoard(
  board: MinesweeperBoard,
  puzzle: MinesweeperPuzzle,
  row: number,
  col: number,
): MinesweeperBoard {
  if (board.generated) {
    return board;
  }

  const config = getMinesweeperConfig(puzzle.difficulty, {
    profileId: puzzle.profileId,
    rows: board.rows,
    cols: board.cols,
  });
  const mineCount = board.mines;
  const protectedIndexes = getProtectedCoords(board.rows, board.cols, row, col, config.protectNeighbors);

  for (let attempt = 0; attempt < config.retryLimit; attempt++) {
    const mineIndexes = getRandomMineIndexes(board.rows, board.cols, mineCount, protectedIndexes);
    const candidateBoard = createResolvedBoard(config, mineIndexes, mineCount);

    if (!meetsDistributionRules(candidateBoard, config, mineCount)) {
      continue;
    }
    if (!meetsOpeningRules(candidateBoard, row, col, config)) {
      continue;
    }
    if (!meetsOpeningQualityRules(candidateBoard, row, col, config)) {
      continue;
    }

    return candidateBoard;
  }

  throw new Error(`Unable to generate ${puzzle.difficulty} Minesweeper board within retry limit.`);
}

export function getMinesweeperConfig(
  difficulty: PuzzleDifficulty,
  options: {
    profileId?: string;
    rows?: number;
    cols?: number;
  } = {},
): MinesweeperConfig {
  const matchedProfile = resolveMinesweeperSizeProfile(difficulty, {
    profileId: options.profileId,
    rows: options.rows,
    cols: options.cols,
  });
  if (matchedProfile) {
    return buildMinesweeperConfig(difficulty, matchedProfile.rows, matchedProfile.cols);
  }

  throw new Error(`Unsupported Minesweeper board profile for ${difficulty}: ${options.rows}x${options.cols}`);
}

export function createMinesweeperPuzzle(
  difficulty: PuzzleDifficulty,
): MinesweeperPuzzle {
  const selectedProfile = getMinesweeperSizeProfile(difficulty);
  const config = buildMinesweeperConfig(difficulty, selectedProfile.rows, selectedProfile.cols);
  return {
    difficulty,
    profileId: selectedProfile.id,
    rows: config.rows,
    cols: config.cols,
    mines: getMineCount(config),
  };
}

export function createMinesweeperBoard(puzzle: MinesweeperPuzzle): MinesweeperBoard {
  return {
    rows: puzzle.rows,
    cols: puzzle.cols,
    mines: puzzle.mines,
    generated: false,
    status: 'playing',
    cells: Array.from({ length: puzzle.rows }, () =>
      Array.from({ length: puzzle.cols }, () => createCell(false)),
    ),
  };
}

export function finalizeMinesweeperBoard(
  board: MinesweeperBoard,
  puzzle: MinesweeperPuzzle,
  row: number,
  col: number,
): MinesweeperBoard {
  if (!isInBounds(board, row, col) || board.status !== 'playing') {
    return board;
  }

  return finalizeGeneratedBoard(board, puzzle, row, col);
}

export function revealMinesweeperCell(
  board: MinesweeperBoard,
  puzzle: MinesweeperPuzzle,
  row: number,
  col: number,
): MinesweeperBoard {
  if (!isInBounds(board, row, col) || board.status !== 'playing') {
    return board;
  }

  const preparedBoard = board.generated ? board : finalizeGeneratedBoard(board, puzzle, row, col);
  const currentCell = preparedBoard.cells[row][col];
  if (currentCell.state === 'flagged' || currentCell.state === 'revealed') {
    return preparedBoard;
  }

  const nextBoard = cloneBoard(preparedBoard);
  const cell = nextBoard.cells[row][col];

  if (cell.isMine) {
    cell.state = 'revealed';
    revealAllMines(nextBoard);
    nextBoard.status = 'lost';
    return nextBoard;
  }

  revealSafeArea(nextBoard, row, col);
  nextBoard.status = computeStatus(nextBoard);
  return nextBoard;
}

export function toggleMinesweeperFlag(board: MinesweeperBoard, row: number, col: number): MinesweeperBoard {
  if (!isInBounds(board, row, col) || board.status !== 'playing' || !board.generated) {
    return board;
  }

  const cell = board.cells[row][col];
  if (cell.state === 'revealed') {
    return board;
  }

  const nextBoard = cloneBoard(board);
  const nextCell = nextBoard.cells[row][col];
  nextCell.state = nextCell.state === 'flagged' ? 'hidden' : 'flagged';
  return nextBoard;
}

export function countFlaggedCells(board: MinesweeperBoard): number {
  return board.cells.flat().filter((cell) => cell.state === 'flagged').length;
}

export function getOpeningCellCount(
  board: MinesweeperBoard,
  puzzle: MinesweeperPuzzle,
  row: number,
  col: number,
): number {
  const simulatedBoard = revealMinesweeperCell(board, puzzle, row, col);
  return countRevealedCells(simulatedBoard);
}

export function getMineDensity(board: MinesweeperBoard): number {
  return board.mines / (board.rows * board.cols);
}

export function getClusterScore(board: MinesweeperBoard): number {
  return countAdjacentMinePairs(board);
}
