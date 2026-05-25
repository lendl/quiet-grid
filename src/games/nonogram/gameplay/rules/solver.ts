import type {
  NonogramBoard,
  NonogramCellRef,
  NonogramCellValue,
  NonogramLineOrientation,
  NonogramPuzzle,
} from '../../types';

export interface NonogramLinePlacement {
  starts: number[];
}

export interface NonogramLineAnalysis {
  placements: readonly NonogramLinePlacement[];
  overlapFillCells: readonly number[];
  forcedEmptyCells: readonly number[];
  isComplete: boolean;
}

export interface NonogramLineCheck {
  orientation: NonogramLineOrientation;
  index: number;
  clues: readonly number[];
  cells: readonly NonogramCellValue[];
  analysis: NonogramLineAnalysis | null;
}

export interface NonogramBoardAnalysis {
  puzzle: NonogramPuzzle;
  rows: readonly NonogramLineCheck[];
  cols: readonly NonogramLineCheck[];
  invalidLine: NonogramLineCheck | null;
}

function isFilledCell(cell: NonogramCellValue): cell is 1 {
  return cell === 1;
}

function rangeHasFilledCell(
  line: readonly NonogramCellValue[],
  start: number,
  endExclusive: number,
): boolean {
  for (let index = start; index < endExclusive; index++) {
    if (line[index] === 1) {
      return true;
    }
  }

  return false;
}

function canPlaceBlockAt(
  line: readonly NonogramCellValue[],
  start: number,
  length: number,
): boolean {
  const end = start + length - 1;
  if (end >= line.length) {
    return false;
  }

  for (let index = start; index <= end; index++) {
    if (line[index] === 0) {
      return false;
    }
  }

  if (start > 0 && line[start - 1] === 1) {
    return false;
  }

  if (end < line.length - 1 && line[end + 1] === 1) {
    return false;
  }

  return true;
}

function placementCoversAllFilledCells(
  line: readonly NonogramCellValue[],
  starts: readonly number[],
  clues: readonly number[],
): boolean {
  const intervals = starts.map((start, clueIndex) => ({
    start,
    end: start + clues[clueIndex] - 1,
  }));

  return line.every((cell, cellIndex) => {
    if (!isFilledCell(cell)) {
      return true;
    }

    return intervals.some(({ start, end }) => cellIndex >= start && cellIndex <= end);
  });
}

function getMinimumRemainingLengths(clues: readonly number[]): number[] {
  const result = new Array(clues.length).fill(0);
  let remaining = 0;

  for (let index = clues.length - 1; index >= 0; index--) {
    remaining += clues[index];
    result[index] = remaining + (clues.length - index - 1);
  }

  return result;
}

export function getNonogramLineCells(
  board: NonogramBoard,
  orientation: NonogramLineOrientation,
  index: number,
): NonogramCellValue[] {
  if (orientation === 'row') {
    return [...(board[index] ?? [])];
  }

  return board.map((row) => row[index] ?? null);
}

export function isNonogramLineComplete(
  line: readonly NonogramCellValue[],
  clues: readonly number[],
): boolean {
  const segments: number[] = [];
  let currentLength = 0;

  line.forEach((cell) => {
    if (cell === 1) {
      currentLength += 1;
      return;
    }

    if (currentLength > 0) {
      segments.push(currentLength);
      currentLength = 0;
    }
  });

  if (currentLength > 0) {
    segments.push(currentLength);
  }

  return segments.length === clues.length
    && segments.every((length, index) => length === clues[index]);
}

export function enumerateNonogramLinePlacements(
  line: readonly NonogramCellValue[],
  clues: readonly number[],
): NonogramLinePlacement[] {
  if (clues.length === 0) {
    return line.some(isFilledCell) ? [] : [{ starts: [] }];
  }

  const minimumRemainingLengths = getMinimumRemainingLengths(clues);
  if (minimumRemainingLengths[0] > line.length) {
    return [];
  }

  const placements: NonogramLinePlacement[] = [];
  const starts = new Array(clues.length).fill(0);
  const stack: Array<{ clueIndex: number; nextStart: number }> = [
    { clueIndex: 0, nextStart: 0 },
  ];

  while (stack.length > 0) {
    const frame = stack[stack.length - 1];
    const clueIndex = frame.clueIndex;
    const clueLength = clues[clueIndex];
    const latestStart = line.length - minimumRemainingLengths[clueIndex];
    let candidateStart = frame.nextStart;
    let advanced = false;

    while (candidateStart <= latestStart) {
      if (!canPlaceBlockAt(line, candidateStart, clueLength)) {
        candidateStart += 1;
        continue;
      }

      if (clueIndex === 0) {
        if (rangeHasFilledCell(line, 0, candidateStart)) {
          candidateStart += 1;
          continue;
        }
      } else {
        const previousEnd = starts[clueIndex - 1] + clues[clueIndex - 1] - 1;
        const gapStart = previousEnd + 1;
        if (candidateStart < gapStart + 1) {
          candidateStart += 1;
          continue;
        }

        if (rangeHasFilledCell(line, gapStart, candidateStart)) {
          candidateStart += 1;
          continue;
        }
      }

      starts[clueIndex] = candidateStart;
      frame.nextStart = candidateStart + 1;
      advanced = true;

      if (clueIndex === clues.length - 1) {
        if (placementCoversAllFilledCells(line, starts, clues)) {
          placements.push({ starts: [...starts] });
        }
      } else {
        stack.push({
          clueIndex: clueIndex + 1,
          nextStart: candidateStart + clueLength + 1,
        });
      }

      break;
    }

    if (!advanced) {
      stack.pop();
    }
  }

  return placements;
}

export function analyzeNonogramLine(
  line: readonly NonogramCellValue[],
  clues: readonly number[],
): NonogramLineAnalysis | null {
  const placements = enumerateNonogramLinePlacements(line, clues);
  if (placements.length === 0) {
    return null;
  }

  const coverage = new Array(line.length).fill(0);
  placements.forEach((placement) => {
    placement.starts.forEach((start, clueIndex) => {
      const end = start + clues[clueIndex] - 1;
      for (let index = start; index <= end; index++) {
        coverage[index] += 1;
      }
    });
  });

  const overlapFillCells: number[] = [];
  const forcedEmptyCells: number[] = [];

  line.forEach((cell, index) => {
    if (cell !== null) {
      return;
    }

    if (coverage[index] === placements.length) {
      overlapFillCells.push(index);
      return;
    }

    if (coverage[index] === 0) {
      forcedEmptyCells.push(index);
    }
  });

  return {
    placements,
    overlapFillCells,
    forcedEmptyCells,
    isComplete: isNonogramLineComplete(line, clues),
  };
}

function buildLineCheck(
  board: NonogramBoard,
  clues: readonly number[],
  orientation: NonogramLineOrientation,
  index: number,
): NonogramLineCheck {
  const cells = getNonogramLineCells(board, orientation, index);
  const analysis = analyzeNonogramLine(cells, clues);

  return {
    orientation,
    index,
    clues,
    cells,
    analysis,
  };
}

export function analyzeNonogramBoard(
  puzzle: NonogramPuzzle,
  board: NonogramBoard,
): NonogramBoardAnalysis {
  const rows = puzzle.rowClues.map((clues, rowIndex) => buildLineCheck(board, clues, 'row', rowIndex));
  const cols = puzzle.colClues.map((clues, colIndex) => buildLineCheck(board, clues, 'col', colIndex));
  const invalidLine = [...rows, ...cols].find((line) => line.analysis === null) ?? null;

  return {
    puzzle,
    rows,
    cols,
    invalidLine,
  };
}

export function isNonogramBoardSolved(
  board: NonogramBoard,
  solution: boolean[][],
): boolean {
  return board.length === solution.length
    && board.every((row, rowIndex) => row.length === solution[rowIndex]?.length
      && row.every((cell, colIndex) => (
        solution[rowIndex][colIndex]
          ? cell === 1
          : cell !== 1
      )));
}

export function lineCellsToRefs(
  orientation: NonogramLineOrientation,
  index: number,
  cellIndexes: readonly number[],
): NonogramCellRef[] {
  return cellIndexes.map((cellIndex) => (
    orientation === 'row'
      ? { row: index, col: cellIndex }
      : { row: cellIndex, col: index }
  ));
}
