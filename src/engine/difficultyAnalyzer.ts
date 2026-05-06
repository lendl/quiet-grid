import type { Cell} from './validator';
import {
  countEmpties,
  countSolutionsForCandidate,
  countValidLineCompletions,
  decodePuzzle,
  decodeSolution,
  findAvoidTrioMoveInLine,
  findPairMoveInLine,
  getColumn,
  hasUniqueLines,
  isCandidateLegal,
  otherValue,
} from '../games/takuzu/core';
import type { EngineTipKey } from './tipCatalog';
import { TIP_LEVELS } from './tipCatalog';

export interface TipUsageCounts {
  findPairs: number;
  avoidTrios: number;
  completeLines: number;
  eliminateFilledLines: number;
  eliminateImpossibleCombinations: number;
}

export interface DifficultyMetrics {
  givenCount: number;
  sparseMoveCount: number;
  tipSequencePressure: number;
  highestTipLevel: number;
  openingHighestTipLevel: number;
  totalMoves: number;
  openingMoves: number;
  tipUsageCounts: TipUsageCounts;
  openingTipUsageCounts: TipUsageCounts;
  impossibleCombinationMaxLineCompletions: number;
}

interface CandidateMove {
  row: number;
  col: number;
  value: 0 | 1;
  tip: EngineTipKey;
  lineCompletions?: number;
}

function toUsageCountKey(tip: EngineTipKey): keyof TipUsageCounts {
  switch (tip) {
    case 'find-pairs':
      return 'findPairs';
    case 'avoid-trios':
      return 'avoidTrios';
    case 'complete-lines':
      return 'completeLines';
    case 'eliminate-filled-lines':
      return 'eliminateFilledLines';
    case 'eliminate-impossible-combinations':
      return 'eliminateImpossibleCombinations';
  }
}

function createEmptyTipUsageCounts(): TipUsageCounts {
  return {
    findPairs: 0,
    avoidTrios: 0,
    completeLines: 0,
    eliminateFilledLines: 0,
    eliminateImpossibleCombinations: 0,
  };
}

function recordMove(metrics: DifficultyMetrics, board: Cell[][], move: CandidateMove, previousLevel: number | null): number {
  const row = board[move.row];
  const col = getColumn(board, move.col);
  const rowEmpties = countEmpties(row);
  const colEmpties = countEmpties(col);
  const level = TIP_LEVELS[move.tip];
  const countKey = toUsageCountKey(move.tip);

  metrics.tipUsageCounts[countKey]++;
  metrics.highestTipLevel = Math.max(metrics.highestTipLevel, level);
  metrics.totalMoves++;

  if (move.tip === 'eliminate-impossible-combinations' && move.lineCompletions !== undefined) {
    metrics.impossibleCombinationMaxLineCompletions = Math.max(
      metrics.impossibleCombinationMaxLineCompletions,
      move.lineCompletions,
    );
  }

  if (Math.max(rowEmpties, colEmpties) >= board.length / 2) {
    metrics.sparseMoveCount++;
  }

  if (previousLevel !== null && previousLevel >= 3 && level >= previousLevel) {
    metrics.tipSequencePressure++;
  }

  board[move.row][move.col] = move.value;
  return level;
}

function findPairMove(board: Cell[][]): CandidateMove | null {
  const size = board.length;

  for (let row = 0; row < size; row++) {
    const move = findPairMoveInLine(board[row]);
    if (move) return { row, col: move.index, value: move.value, tip: 'find-pairs' };
  }

  for (let col = 0; col < size; col++) {
    const move = findPairMoveInLine(getColumn(board, col));
    if (move) return { row: move.index, col, value: move.value, tip: 'find-pairs' };
  }

  return null;
}

function findAvoidTrioMove(board: Cell[][]): CandidateMove | null {
  const size = board.length;

  for (let row = 0; row < size; row++) {
    const move = findAvoidTrioMoveInLine(board[row]);
    if (move) return { row, col: move.index, value: move.value, tip: 'avoid-trios' };
  }

  for (let col = 0; col < size; col++) {
    const move = findAvoidTrioMoveInLine(getColumn(board, col));
    if (move) return { row: move.index, col, value: move.value, tip: 'avoid-trios' };
  }

  return null;
}

function findCompleteLineMove(board: Cell[][]): CandidateMove | null {
  const size = board.length;
  const half = size / 2;

  for (let row = 0; row < size; row++) {
    const line = board[row];
    const zeroes = line.filter(cell => cell === 0).length;
    const ones = line.filter(cell => cell === 1).length;
    if (zeroes === half || ones === half) {
      const fillValue = zeroes === half ? 1 : 0;
      const col = line.findIndex(cell => cell === null);
      if (col !== -1) return { row, col, value: fillValue, tip: 'complete-lines' };
    }
  }

  for (let col = 0; col < size; col++) {
    const line = getColumn(board, col);
    const zeroes = line.filter(cell => cell === 0).length;
    const ones = line.filter(cell => cell === 1).length;
    if (zeroes === half || ones === half) {
      const fillValue = zeroes === half ? 1 : 0;
      const row = line.findIndex(cell => cell === null);
      if (row !== -1) return { row, col, value: fillValue, tip: 'complete-lines' };
    }
  }

  return null;
}

function findEliminateFilledLinesRow(board: Cell[][]): CandidateMove | null {
  const size = board.length;
  const completeRows = board
    .map((line, index) => ({ line, index }))
    .filter(({ line }) => line.every(cell => cell !== null));

  for (let row = 0; row < size; row++) {
    const line = board[row];
    const emptyCols = line.flatMap((cell, col) => (cell === null ? [col] : []));
    if (emptyCols.length !== 2) continue;

    for (const complete of completeRows) {
      if (complete.index === row) continue;
      const matches = line.every((cell, col) => cell === null || cell === complete.line[col]);
      if (!matches) continue;

      const col = emptyCols[0];
      return {
        row,
        col,
        value: otherValue(complete.line[col] as 0 | 1),
        tip: 'eliminate-filled-lines',
      };
    }
  }

  return null;
}

function findEliminateFilledLinesColumn(board: Cell[][]): CandidateMove | null {
  const size = board.length;
  const completeCols = Array.from({ length: size }, (_, index) => ({
    line: getColumn(board, index),
    index,
  })).filter(({ line }) => line.every(cell => cell !== null));

  for (let col = 0; col < size; col++) {
    const line = getColumn(board, col);
    const emptyRows = line.flatMap((cell, row) => (cell === null ? [row] : []));
    if (emptyRows.length !== 2) continue;

    for (const complete of completeCols) {
      if (complete.index === col) continue;
      const matches = line.every((cell, row) => cell === null || cell === complete.line[row]);
      if (!matches) continue;

      const row = emptyRows[0];
      return {
        row,
        col,
        value: otherValue(complete.line[row] as 0 | 1),
        tip: 'eliminate-filled-lines',
      };
    }
  }

  return null;
}

function findImpossibleCombinationMove(board: Cell[][]): CandidateMove | null {
  const size = board.length;

  for (let row = 0; row < size; row++) {
    for (let col = 0; col < size; col++) {
      if (board[row][col] !== null) continue;

      const solvableValues = ([0, 1] as const).filter(value => {
        if (!isCandidateLegal(board, row, col, value)) {
          return false;
        }

        return countSolutionsForCandidate(board, row, col, value) > 0;
      });

      if (solvableValues.length === 1) {
        const rowCompletions = countValidLineCompletions(board[row]);
        const columnCompletions = countValidLineCompletions(getColumn(board, col));
        return {
          row,
          col,
          value: solvableValues[0],
          tip: 'eliminate-impossible-combinations',
          lineCompletions: Math.min(rowCompletions, columnCompletions),
        };
      }
    }
  }

  return null;
}

function isSolved(board: Cell[][]): board is (0 | 1)[][] {
  return board.every(row => row.every(cell => cell !== null));
}

function formatBoard(board: Cell[][]): string {
  return board
    .map(row => row.map(cell => (cell === null ? '.' : String(cell))).join(' '))
    .join('\n');
}

function buildOpeningMetrics(metrics: DifficultyMetrics, moveHistory: EngineTipKey[]): void {
  if (moveHistory.length === 0) {
    metrics.openingMoves = 0;
    metrics.openingHighestTipLevel = 0;
    metrics.openingTipUsageCounts = createEmptyTipUsageCounts();
    return;
  }

  const openingCount = Math.min(
    moveHistory.length,
    Math.max(10, Math.ceil(moveHistory.length * 0.3)),
  );

  const openingUsage = createEmptyTipUsageCounts();
  let openingHighestTipLevel = 0;

  for (const tip of moveHistory.slice(0, openingCount)) {
    openingUsage[toUsageCountKey(tip)]++;
    openingHighestTipLevel = Math.max(openingHighestTipLevel, TIP_LEVELS[tip]);
  }

  metrics.openingMoves = openingCount;
  metrics.openingHighestTipLevel = openingHighestTipLevel;
  metrics.openingTipUsageCounts = openingUsage;
}

export function analyzeDifficulty(solutionHex: string, maskHex: string, size: number): DifficultyMetrics {
  const board = decodePuzzle(solutionHex, maskHex, size);
  const solution = decodeSolution(solutionHex, size);
  const metrics: DifficultyMetrics = {
    givenCount: board.flat().filter(cell => cell !== null).length,
    sparseMoveCount: 0,
    tipSequencePressure: 0,
    highestTipLevel: 0,
    openingHighestTipLevel: 0,
    totalMoves: 0,
    openingMoves: 0,
    tipUsageCounts: createEmptyTipUsageCounts(),
    openingTipUsageCounts: createEmptyTipUsageCounts(),
    impossibleCombinationMaxLineCompletions: 0,
  };

  let previousLevel: number | null = null;
  let safetyCounter = 0;
  const moveHistory: EngineTipKey[] = [];

  while (!isSolved(board) && safetyCounter < size * size * 4) {
    safetyCounter++;

    const move =
      findPairMove(board) ??
      findAvoidTrioMove(board) ??
      findCompleteLineMove(board) ??
      findEliminateFilledLinesRow(board) ??
      findEliminateFilledLinesColumn(board) ??
      findImpossibleCombinationMove(board);

    if (!move) {
      throw new Error(`Could not fully analyze puzzle with current logical tips.\n${formatBoard(board)}`);
    }

    if (solution[move.row][move.col] !== move.value) {
      throw new Error(`Analyzer produced an incorrect move at ${move.row},${move.col}.`);
    }

    moveHistory.push(move.tip);
    previousLevel = recordMove(metrics, board, move, previousLevel);
  }

  if (!isSolved(board) || !hasUniqueLines(board)) {
    throw new Error('Difficulty analysis did not finish with a solved valid board.');
  }

  buildOpeningMetrics(metrics, moveHistory);
  return metrics;
}
