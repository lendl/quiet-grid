import type { MinesweeperBoard } from '../types';
import {
  buildGuessNextMove,
  buildSafeRevealNextMove,
} from './content';

export interface MinesweeperNextMoveCell {
  row: number;
  col: number;
}

export interface MinesweeperNextMoveHint {
  title: string;
  body: string;
  evidenceCells: MinesweeperNextMoveCell[];
  targetCells: MinesweeperNextMoveCell[];
}

interface MinesweeperClue {
  row: number;
  col: number;
  adjacentMines: number;
}

function toKey(row: number, col: number): string {
  return `${row}:${col}`;
}

function compareCells(a: MinesweeperNextMoveCell, b: MinesweeperNextMoveCell): number {
  if (a.row !== b.row) {
    return a.row - b.row;
  }

  return a.col - b.col;
}

function dedupeCells(cells: MinesweeperNextMoveCell[]): MinesweeperNextMoveCell[] {
  const seen = new Set<string>();
  const unique: MinesweeperNextMoveCell[] = [];

  cells.forEach((cell) => {
    const key = toKey(cell.row, cell.col);
    if (seen.has(key)) {
      return;
    }

    seen.add(key);
    unique.push(cell);
  });

  return unique.sort(compareCells);
}

function getNeighborCells(board: MinesweeperBoard, row: number, col: number): MinesweeperNextMoveCell[] {
  const neighbors: MinesweeperNextMoveCell[] = [];

  for (let rowOffset = -1; rowOffset <= 1; rowOffset++) {
    for (let colOffset = -1; colOffset <= 1; colOffset++) {
      if (rowOffset === 0 && colOffset === 0) {
        continue;
      }

      const nextRow = row + rowOffset;
      const nextCol = col + colOffset;

      if (
        nextRow >= 0
        && nextRow < board.rows
        && nextCol >= 0
        && nextCol < board.cols
      ) {
        neighbors.push({ row: nextRow, col: nextCol });
      }
    }
  }

  return neighbors;
}

function isUnknownCell(board: MinesweeperBoard, row: number, col: number): boolean {
  return board.cells[row][col].state !== 'revealed';
}

function getClues(board: MinesweeperBoard): MinesweeperClue[] {
  const clues: MinesweeperClue[] = [];

  for (let row = 0; row < board.rows; row++) {
    for (let col = 0; col < board.cols; col++) {
      const cell = board.cells[row][col];
      if (cell.state !== 'revealed' || cell.adjacentMines <= 0) {
        continue;
      }

      clues.push({
        row,
        col,
        adjacentMines: cell.adjacentMines,
      });
    }
  }

  return clues;
}

function findForcedMineKeys(board: MinesweeperBoard, clues: MinesweeperClue[]): Set<string> {
  const forcedMineKeys = new Set<string>();
  let changed = true;

  while (changed) {
    changed = false;

    clues.forEach((clue) => {
      const unknownNeighbors = getNeighborCells(board, clue.row, clue.col)
        .filter(({ row, col }) => isUnknownCell(board, row, col));
      const forcedAround = unknownNeighbors.filter(({ row, col }) => forcedMineKeys.has(toKey(row, col)));
      const unresolvedNeighbors = unknownNeighbors.filter(({ row, col }) => !forcedMineKeys.has(toKey(row, col)));
      const remainingMines = clue.adjacentMines - forcedAround.length;

      if (remainingMines <= 0 || remainingMines !== unresolvedNeighbors.length) {
        return;
      }

      unresolvedNeighbors.forEach(({ row, col }) => {
        const key = toKey(row, col);
        if (forcedMineKeys.has(key)) {
          return;
        }

        forcedMineKeys.add(key);
        changed = true;
      });
    });
  }

  return forcedMineKeys;
}

function createGuessHint(): MinesweeperNextMoveHint {
  return {
    ...buildGuessNextMove(),
    evidenceCells: [],
    targetCells: [],
  };
}

export function getMinesweeperNextMoveHint(board: MinesweeperBoard): MinesweeperNextMoveHint {
  if (board.status !== 'playing') {
    return createGuessHint();
  }

  const clues = getClues(board);
  if (clues.length === 0) {
    return createGuessHint();
  }

  const forcedMineKeys = findForcedMineKeys(board, clues);
  const safeHints = clues.flatMap((clue) => {
    const unknownNeighbors = getNeighborCells(board, clue.row, clue.col)
      .filter(({ row, col }) => isUnknownCell(board, row, col));
    const forcedMineNeighbors = unknownNeighbors
      .filter(({ row, col }) => forcedMineKeys.has(toKey(row, col)));
    const remainingMines = clue.adjacentMines - forcedMineNeighbors.length;

    if (remainingMines !== 0) {
      return [];
    }

    const targetCells = unknownNeighbors
      .filter(({ row, col }) => !forcedMineKeys.has(toKey(row, col)))
      .sort(compareCells);

    if (targetCells.length === 0) {
      return [];
    }

    return [{
      clueCell: { row: clue.row, col: clue.col },
      adjacentMines: clue.adjacentMines,
      evidenceCells: dedupeCells([
        { row: clue.row, col: clue.col },
        ...forcedMineNeighbors,
      ]),
      targetCells,
    }];
  });

  safeHints.sort((a, b) => {
    if (a.targetCells.length !== b.targetCells.length) {
      return a.targetCells.length - b.targetCells.length;
    }

    return compareCells(a.clueCell, b.clueCell);
  });

  const nextHint = safeHints[0];
  if (!nextHint) {
    return createGuessHint();
  }

  return {
    ...buildSafeRevealNextMove({
      clueCell: nextHint.clueCell,
      targetCount: nextHint.targetCells.length,
      mineCount: nextHint.adjacentMines,
    }),
    evidenceCells: nextHint.evidenceCells,
    targetCells: nextHint.targetCells,
  };
}
