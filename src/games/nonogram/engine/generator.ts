import type { EngineGenerateResult } from '../../../engine/gameDefinition';
import { buildColClues, buildRowClues } from '../gameplay/rules/clues';
import { solveNonogramFromState } from '../gameplay/rules/solver';
import { createEmptyCells } from '../gameplay/rules/board';
import { encodeSolutionGrid } from '../platform/puzzleData';
import type { NonogramDifficulty, NonogramPuzzle, NonogramSize } from '../types';
import { classifyNonogramDifficulty } from './difficulty';

const MAX_GENERATION_ATTEMPTS = 400;

function randomInt(min: number, max: number): number {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function coinFlip(probability: number): boolean {
  return Math.random() < probability;
}

function fillRun(row: boolean[], start: number, length: number): void {
  for (let offset = 0; offset < length; offset += 1) {
    row[start + offset] = true;
  }
}

function createEasyRow(size: NonogramSize): boolean[] {
  const row = Array.from({ length: size }, () => false);
  const strategy = randomInt(0, 3);
  if (strategy === 0) {
    const length = randomInt(Math.max(1, Math.floor(size * 0.6)), size);
    const start = randomInt(0, size - length);
    fillRun(row, start, length);
    return row;
  }

  if (strategy === 1) {
    const length = randomInt(Math.max(1, Math.floor(size * 0.5)), Math.max(1, size - 1));
    const start = randomInt(0, size - length);
    fillRun(row, start, length);
    return row;
  }

  if (strategy === 2) {
    const firstLength = randomInt(1, Math.max(1, Math.floor(size / 3)));
    const secondLength = randomInt(1, Math.max(1, Math.floor(size / 3)));
    if (firstLength + secondLength + 1 > size) {
      fillRun(row, 0, Math.min(size, Math.max(firstLength, secondLength)));
      return row;
    }
    const firstStart = randomInt(0, size - firstLength - secondLength - 1);
    const secondStart = randomInt(firstStart + firstLength + 1, size - secondLength);
    fillRun(row, firstStart, firstLength);
    fillRun(row, secondStart, secondLength);
    return row;
  }

  return row;
}

function createMediumRow(size: NonogramSize): boolean[] {
  const row = Array.from({ length: size }, () => false);
  const runCount = randomInt(1, size === 5 ? 3 : 4);
  let cursor = 0;

  for (let runIndex = 0; runIndex < runCount; runIndex += 1) {
    const remainingRuns = runCount - runIndex - 1;
    const maxLength = Math.max(1, size - cursor - remainingRuns * 2);
    if (maxLength <= 0) {
      break;
    }

    const runLength = randomInt(1, Math.min(maxLength, size === 5 ? 3 : 4));
    const start = randomInt(cursor, Math.max(cursor, size - runLength - remainingRuns * 2));
    fillRun(row, start, runLength);
    cursor = start + runLength + 1;
    if (cursor >= size) {
      break;
    }
  }

  return row;
}

function mirrorMaybe(grid: boolean[][]): boolean[][] {
  if (!coinFlip(0.5)) {
    return grid;
  }
  return grid.map((row) => [...row].reverse());
}

function buildCandidateGrid(size: NonogramSize, difficulty: NonogramDifficulty): boolean[][] {
  const rows = Array.from({ length: size }, () => (
    difficulty === 'easy' ? createEasyRow(size) : createMediumRow(size)
  ));

  if (coinFlip(0.35)) {
    for (let rowIndex = 0; rowIndex < size; rowIndex += 1) {
      const mirroredIndex = size - 1 - rowIndex;
      rows[mirroredIndex] = [...rows[rowIndex]];
    }
  }

  return mirrorMaybe(rows);
}

function hasInterestingColumns(grid: readonly (readonly boolean[])[]): boolean {
  const cols = grid[0]?.length ?? 0;
  for (let colIndex = 0; colIndex < cols; colIndex += 1) {
    let filled = 0;
    grid.forEach((row) => {
      if (row[colIndex]) {
        filled += 1;
      }
    });
    if (filled === 0 || filled === grid.length) {
      return false;
    }
  }
  return true;
}

function generatePuzzleEntry(
  size: NonogramSize,
  targetDifficulty: NonogramDifficulty,
): EngineGenerateResult<NonogramPuzzle> | null {
  for (let attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt += 1) {
    const grid = buildCandidateGrid(size, targetDifficulty);
    if (!hasInterestingColumns(grid)) {
      continue;
    }

    const solution = encodeSolutionGrid(grid);
    const puzzle: NonogramPuzzle = {
      id: 'pending',
      size,
      rows: size,
      cols: size,
      difficulty: targetDifficulty,
      solution,
      rowClues: buildRowClues(grid),
      colClues: buildColClues(grid),
    };

    const solveResult = solveNonogramFromState(puzzle, createEmptyCells(size, size));
    const classifiedDifficulty = classifyNonogramDifficulty(puzzle, solveResult);
    if (classifiedDifficulty !== targetDifficulty) {
      continue;
    }

    return {
      dedupeKey: solution,
      entry: {
        ...puzzle,
        difficulty: classifiedDifficulty,
      },
      label: `${size}x${size} ${classifiedDifficulty}`,
      score: solveResult.steps.length,
    };
  }

  return null;
}

export function generateNonogramPuzzle(
  size: NonogramSize,
  difficulty: NonogramDifficulty,
): EngineGenerateResult<NonogramPuzzle> | null {
  return generatePuzzleEntry(size, difficulty);
}
