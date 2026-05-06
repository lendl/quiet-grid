import type { MinesweeperBoard } from '../types';
import {
  buildGuessNextMove,
  buildPatternNextMove,
} from './content';
import type { MinesweeperLearningCenterPatternKey } from '../i18n';

export interface MinesweeperNextMoveCell {
  row: number;
  col: number;
}

export type MinesweeperNextMovePatternKey = MinesweeperLearningCenterPatternKey;

export interface MinesweeperNextMoveTeaching {
  patternKey: MinesweeperNextMovePatternKey;
  patternTitle: string;
  patternLabel: string;
  explanationTitle: string;
  explanation: string;
}

export interface MinesweeperNextMoveHint {
  title: string;
  body: string;
  evidenceCells: MinesweeperNextMoveCell[];
  targetCells: MinesweeperNextMoveCell[];
  teaching?: MinesweeperNextMoveTeaching;
}

interface MinesweeperClue {
  row: number;
  col: number;
  adjacentMines: number;
}

interface MinesweeperClueState {
  unknownNeighbors: MinesweeperNextMoveCell[];
  forcedMineNeighbors: MinesweeperNextMoveCell[];
  unresolvedNeighbors: MinesweeperNextMoveCell[];
  remainingMines: number;
}

type ForcedMineReason = 'direct-local' | 'subset-difference';

interface MinesweeperAnalyzedHint {
  patternKey: MinesweeperNextMovePatternKey;
  evidenceCells: MinesweeperNextMoveCell[];
  targetCells: MinesweeperNextMoveCell[];
  primaryClueCell?: MinesweeperNextMoveCell;
  secondaryClueCell?: MinesweeperNextMoveCell;
  mineCount?: number;
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

function containsCell(cells: MinesweeperNextMoveCell[], candidate: MinesweeperNextMoveCell): boolean {
  return cells.some(({ row, col }) => row === candidate.row && col === candidate.col);
}

function differenceCells(
  cells: MinesweeperNextMoveCell[],
  excludedCells: MinesweeperNextMoveCell[],
): MinesweeperNextMoveCell[] {
  const excludedKeys = new Set(excludedCells.map(({ row, col }) => toKey(row, col)));
  return cells.filter(({ row, col }) => !excludedKeys.has(toKey(row, col)));
}

function isSubset(
  subsetCells: MinesweeperNextMoveCell[],
  supersetCells: MinesweeperNextMoveCell[],
): boolean {
  const supersetKeys = new Set(supersetCells.map(({ row, col }) => toKey(row, col)));
  return subsetCells.every(({ row, col }) => supersetKeys.has(toKey(row, col)));
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

function getClueState(
  board: MinesweeperBoard,
  clue: MinesweeperClue,
  forcedMineKeys: Set<string>,
): MinesweeperClueState {
  const unknownNeighbors = getNeighborCells(board, clue.row, clue.col)
    .filter(({ row, col }) => isUnknownCell(board, row, col));
  const forcedMineNeighbors = unknownNeighbors
    .filter(({ row, col }) => forcedMineKeys.has(toKey(row, col)));
  const unresolvedNeighbors = unknownNeighbors
    .filter(({ row, col }) => !forcedMineKeys.has(toKey(row, col)));

  return {
    unknownNeighbors,
    forcedMineNeighbors,
    unresolvedNeighbors,
    remainingMines: clue.adjacentMines - forcedMineNeighbors.length,
  };
}

function tryAddForcedMines(
  forcedMineReasons: Map<string, ForcedMineReason>,
  cells: MinesweeperNextMoveCell[],
  reason: ForcedMineReason,
): boolean {
  let changed = false;

  cells.forEach(({ row, col }) => {
    const key = toKey(row, col);
    if (forcedMineReasons.has(key)) {
      return;
    }

    forcedMineReasons.set(key, reason);
    changed = true;
  });

  return changed;
}

function findForcedMineReasons(board: MinesweeperBoard, clues: MinesweeperClue[]): Map<string, ForcedMineReason> {
  const forcedMineReasons = new Map<string, ForcedMineReason>();
  let changed = true;

  while (changed) {
    changed = false;
    const forcedMineKeys = new Set(forcedMineReasons.keys());

    clues.forEach((clue) => {
      const state = getClueState(board, clue, forcedMineKeys);

      if (
        state.remainingMines <= 0
        || state.remainingMines !== state.unresolvedNeighbors.length
      ) {
        return;
      }

      if (tryAddForcedMines(forcedMineReasons, state.unresolvedNeighbors, 'direct-local')) {
        changed = true;
      }
    });

    for (let clueIndex = 0; clueIndex < clues.length; clueIndex++) {
      for (let otherIndex = clueIndex + 1; otherIndex < clues.length; otherIndex++) {
        const firstClue = clues[clueIndex];
        const secondClue = clues[otherIndex];
        const refreshedKeys = new Set(forcedMineReasons.keys());
        const firstState = getClueState(board, firstClue, refreshedKeys);
        const secondState = getClueState(board, secondClue, refreshedKeys);

        const orderedPairs: Array<
          [MinesweeperClue, MinesweeperClue, MinesweeperClueState, MinesweeperClueState]
        > = [
          [firstClue, secondClue, firstState, secondState],
          [secondClue, firstClue, secondState, firstState],
        ];

        orderedPairs.forEach(([, , subsetState, supersetState]) => {
          if (
            subsetState.remainingMines < 0
            || supersetState.remainingMines < 0
            || subsetState.unresolvedNeighbors.length === 0
            || supersetState.unresolvedNeighbors.length === 0
            || !isSubset(subsetState.unresolvedNeighbors, supersetState.unresolvedNeighbors)
          ) {
            return;
          }

          const extraCells = differenceCells(
            supersetState.unresolvedNeighbors,
            subsetState.unresolvedNeighbors,
          );
          const remainingMineDifference = supersetState.remainingMines - subsetState.remainingMines;

          if (
            remainingMineDifference <= 0
            || remainingMineDifference !== extraCells.length
          ) {
            return;
          }

          if (tryAddForcedMines(forcedMineReasons, extraCells, 'subset-difference')) {
            changed = true;
          }
        });
      }
    }
  }

  return forcedMineReasons;
}

function canCellBeMine(
  board: MinesweeperBoard,
  candidateCell: MinesweeperNextMoveCell,
  forcedMineKeys: Set<string>,
): boolean {
  const candidateKey = toKey(candidateCell.row, candidateCell.col);

  return getNeighborCells(board, candidateCell.row, candidateCell.col).every(({ row, col }) => {
    const clueCell = board.cells[row][col];
    if (clueCell.state !== 'revealed' || clueCell.adjacentMines <= 0) {
      return true;
    }

    const clue: MinesweeperClue = { row, col, adjacentMines: clueCell.adjacentMines };
    const clueState = getClueState(board, clue, forcedMineKeys);
    const touchesClue = clueState.unresolvedNeighbors.some(
      ({ row: neighborRow, col: neighborCol }) => toKey(neighborRow, neighborCol) === candidateKey,
    );

    if (!touchesClue) {
      return true;
    }

    const assumedMineCount = clueState.forcedMineNeighbors.length + 1;
    if (assumedMineCount > clue.adjacentMines) {
      return false;
    }

    const remainingMinesAfterAssumption = clue.adjacentMines - assumedMineCount;
    const remainingSlotsAfterAssumption = clueState.unresolvedNeighbors.length - 1;
    return remainingMinesAfterAssumption <= remainingSlotsAfterAssumption;
  });
}

function getContradictionClues(
  board: MinesweeperBoard,
  candidateCell: MinesweeperNextMoveCell,
  forcedMineKeys: Set<string>,
): MinesweeperNextMoveCell[] {
  const candidateKey = toKey(candidateCell.row, candidateCell.col);

  return getNeighborCells(board, candidateCell.row, candidateCell.col)
    .filter(({ row, col }) => {
      const clueCell = board.cells[row][col];
      if (clueCell.state !== 'revealed' || clueCell.adjacentMines <= 0) {
        return false;
      }

      const clue: MinesweeperClue = { row, col, adjacentMines: clueCell.adjacentMines };
      const clueState = getClueState(board, clue, forcedMineKeys);
      const touchesClue = clueState.unresolvedNeighbors.some(
        ({ row: neighborRow, col: neighborCol }) => toKey(neighborRow, neighborCol) === candidateKey,
      );

      if (!touchesClue) {
        return false;
      }

      const assumedMineCount = clueState.forcedMineNeighbors.length + 1;
      if (assumedMineCount > clue.adjacentMines) {
        return true;
      }

      const remainingMinesAfterAssumption = clue.adjacentMines - assumedMineCount;
      const remainingSlotsAfterAssumption = clueState.unresolvedNeighbors.length - 1;
      return remainingMinesAfterAssumption > remainingSlotsAfterAssumption;
    })
    .sort(compareCells);
}

function findOnlyOnePossibleMineHint(
  board: MinesweeperBoard,
  clues: MinesweeperClue[],
  forcedMineKeys: Set<string>,
): MinesweeperAnalyzedHint | null {
  const hints: MinesweeperAnalyzedHint[] = [];

  for (let clueIndex = 0; clueIndex < clues.length; clueIndex++) {
    for (let otherIndex = clueIndex + 1; otherIndex < clues.length; otherIndex++) {
      const firstClue = clues[clueIndex];
      const secondClue = clues[otherIndex];
      const firstState = getClueState(board, firstClue, forcedMineKeys);
      const secondState = getClueState(board, secondClue, forcedMineKeys);

      const orderedPairs: Array<
        [MinesweeperClue, MinesweeperClue, MinesweeperClueState, MinesweeperClueState]
      > = [
        [firstClue, secondClue, firstState, secondState],
        [secondClue, firstClue, secondState, firstState],
      ];

      orderedPairs.forEach(([subsetClue, supersetClue, subsetState, supersetState]) => {
        if (
          subsetState.remainingMines !== 1
          || supersetState.remainingMines !== 1
          || subsetState.unresolvedNeighbors.length === 0
          || supersetState.unresolvedNeighbors.length === 0
          || !isSubset(subsetState.unresolvedNeighbors, supersetState.unresolvedNeighbors)
        ) {
          return;
        }

        const extraCells = differenceCells(
          supersetState.unresolvedNeighbors,
          subsetState.unresolvedNeighbors,
        ).sort(compareCells);

        if (extraCells.length === 0) {
          return;
        }

        hints.push({
          patternKey: 'only-one-possible-mine',
          primaryClueCell: { row: supersetClue.row, col: supersetClue.col },
          secondaryClueCell: { row: subsetClue.row, col: subsetClue.col },
          evidenceCells: dedupeCells([
            { row: subsetClue.row, col: subsetClue.col },
            { row: supersetClue.row, col: supersetClue.col },
            ...subsetState.unresolvedNeighbors,
            ...supersetState.unresolvedNeighbors,
          ]),
          targetCells: extraCells,
          mineCount: 1,
        });
      });
    }
  }

  hints.sort((a, b) => {
    if (a.targetCells.length !== b.targetCells.length) {
      return a.targetCells.length - b.targetCells.length;
    }

    if (!a.primaryClueCell || !b.primaryClueCell) {
      return 0;
    }

    return compareCells(a.primaryClueCell, b.primaryClueCell);
  });

  return hints[0] ?? null;
}

function findGuaranteedSafeTileHint(
  board: MinesweeperBoard,
  clues: MinesweeperClue[],
  forcedMineKeys: Set<string>,
): MinesweeperAnalyzedHint | null {
  const candidates = dedupeCells(clues.flatMap((clue) => (
    getClueState(board, clue, forcedMineKeys).unresolvedNeighbors
  )));

  for (const candidateCell of candidates) {
    const contradictionClues = getContradictionClues(board, candidateCell, forcedMineKeys);
    if (contradictionClues.length === 0) {
      continue;
    }

    const primaryClueCell = contradictionClues[0];
    const primaryClue = board.cells[primaryClueCell.row][primaryClueCell.col];

    return {
      patternKey: 'guaranteed-safe-tile',
      primaryClueCell,
      secondaryClueCell: contradictionClues[1],
      evidenceCells: dedupeCells([
        candidateCell,
        ...contradictionClues,
      ]),
      targetCells: [candidateCell],
      mineCount: primaryClue.adjacentMines,
    };
  }

  return null;
}

function findSingleMineLogicHint(
  board: MinesweeperBoard,
  clues: MinesweeperClue[],
  forcedMineKeys: Set<string>,
): MinesweeperAnalyzedHint | null {
  const hints: MinesweeperAnalyzedHint[] = [];

  clues.forEach((clue) => {
    const clueState = getClueState(board, clue, forcedMineKeys);
    if (clueState.remainingMines !== 1 || clueState.unresolvedNeighbors.length < 2) {
      return;
    }

    const legalMineCandidates = clueState.unresolvedNeighbors
      .filter((candidateCell) => canCellBeMine(board, candidateCell, forcedMineKeys))
      .sort(compareCells);

    if (legalMineCandidates.length !== 1) {
      return;
    }

    const targetCells = differenceCells(
      clueState.unresolvedNeighbors,
      legalMineCandidates,
    ).sort(compareCells);

    if (targetCells.length === 0) {
      return;
    }

    hints.push({
      patternKey: 'single-mine-logic',
      primaryClueCell: { row: clue.row, col: clue.col },
      evidenceCells: dedupeCells([
        { row: clue.row, col: clue.col },
        ...clueState.unresolvedNeighbors,
      ]),
      targetCells,
      mineCount: 1,
    });
  });

  hints.sort((a, b) => {
    if (a.targetCells.length !== b.targetCells.length) {
      return a.targetCells.length - b.targetCells.length;
    }

    if (!a.primaryClueCell || !b.primaryClueCell) {
      return 0;
    }

    return compareCells(a.primaryClueCell, b.primaryClueCell);
  });

  return hints[0] ?? null;
}

function findSatisfiedClueHint(
  board: MinesweeperBoard,
  clues: MinesweeperClue[],
  forcedMineReasons: Map<string, ForcedMineReason>,
  patternKey: MinesweeperNextMovePatternKey,
): MinesweeperAnalyzedHint | null {
  const forcedMineKeys = new Set(forcedMineReasons.keys());
  const hints: MinesweeperAnalyzedHint[] = [];

  clues.forEach((clue) => {
    const clueState = getClueState(board, clue, forcedMineKeys);
    if (clueState.remainingMines !== 0 || clueState.unresolvedNeighbors.length === 0) {
      return;
    }

    const usesSubsetResolution = clueState.forcedMineNeighbors.some(({ row, col }) => (
      forcedMineReasons.get(toKey(row, col)) === 'subset-difference'
    ));
    const resolvedPatternKey = usesSubsetResolution
      ? 'full-clue-resolution'
      : 'all-mines-accounted-for';

    if (resolvedPatternKey !== patternKey) {
      return;
    }

    hints.push({
      patternKey,
      primaryClueCell: { row: clue.row, col: clue.col },
      evidenceCells: dedupeCells([
        { row: clue.row, col: clue.col },
        ...clueState.forcedMineNeighbors,
      ]),
      targetCells: clueState.unresolvedNeighbors.sort(compareCells),
      mineCount: clue.adjacentMines,
    });
  });

  hints.sort((a, b) => {
    if (a.targetCells.length !== b.targetCells.length) {
      return a.targetCells.length - b.targetCells.length;
    }

    if (!a.primaryClueCell || !b.primaryClueCell) {
      return 0;
    }

    return compareCells(a.primaryClueCell, b.primaryClueCell);
  });

  return hints[0] ?? null;
}

function findFullClueResolutionHint(
  board: MinesweeperBoard,
  clues: MinesweeperClue[],
  forcedMineReasons: Map<string, ForcedMineReason>,
): MinesweeperAnalyzedHint | null {
  return findSatisfiedClueHint(board, clues, forcedMineReasons, 'full-clue-resolution');
}

function findAllMinesAccountedForHint(
  board: MinesweeperBoard,
  clues: MinesweeperClue[],
  forcedMineReasons: Map<string, ForcedMineReason>,
): MinesweeperAnalyzedHint | null {
  return findSatisfiedClueHint(board, clues, forcedMineReasons, 'all-mines-accounted-for');
}

function createGuessHint(): MinesweeperNextMoveHint {
  const guessHint = buildGuessNextMove();
  return {
    title: guessHint.title,
    body: guessHint.body,
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

  const forcedMineReasons = findForcedMineReasons(board, clues);
  const forcedMineKeys = new Set(forcedMineReasons.keys());

  const analyzedHint = findOnlyOnePossibleMineHint(board, clues, forcedMineKeys)
    ?? findGuaranteedSafeTileHint(board, clues, forcedMineKeys)
    ?? findSingleMineLogicHint(board, clues, forcedMineKeys)
    ?? findFullClueResolutionHint(board, clues, forcedMineReasons)
    ?? findAllMinesAccountedForHint(board, clues, forcedMineReasons);

  if (!analyzedHint) {
    return createGuessHint();
  }

  const nextMoveCopy = buildPatternNextMove({
    patternKey: analyzedHint.patternKey,
    clueCell: analyzedHint.primaryClueCell,
    secondaryClueCell: analyzedHint.secondaryClueCell,
    targetCount: analyzedHint.targetCells.length,
    mineCount: analyzedHint.mineCount,
  });

  return {
    ...nextMoveCopy,
    teaching: nextMoveCopy.teaching ? {
      patternKey: analyzedHint.patternKey,
      ...nextMoveCopy.teaching,
    } : undefined,
    evidenceCells: analyzedHint.evidenceCells,
    targetCells: analyzedHint.targetCells,
  };
}
