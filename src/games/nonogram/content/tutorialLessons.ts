import {
  createEmptyNonogramBoard,
  type NonogramBoard,
  type NonogramCellRef,
  type NonogramCellValue,
  type NonogramDirectState,
  type NonogramPuzzle,
} from '../types';
import {
  getNonogramTutorialLessonCopies,
  type NonogramTutorialLessonKey,
} from './i18n';

export interface NonogramTutorialLesson {
  key: NonogramTutorialLessonKey;
  title: string;
  body: string;
  summary: string;
  continueLabel: string;
  puzzle: NonogramPuzzle;
  board: NonogramBoard;
  evidenceCells: readonly NonogramCellRef[];
  targetCells: ReadonlyArray<NonogramCellRef & { value: NonogramDirectState }>;
  highlightRows: readonly number[];
  highlightCols: readonly number[];
}

const SOLUTION: readonly boolean[][] = [
  [false, true, true, true, false],
  [true, false, false, false, true],
  [true, true, true, true, true],
  [true, false, false, false, true],
  [false, true, true, true, false],
];

function buildClues(line: readonly boolean[]): number[] {
  const clues: number[] = [];
  let run = 0;

  line.forEach((cell) => {
    if (cell) {
      run += 1;
      return;
    }

    if (run > 0) {
      clues.push(run);
      run = 0;
    }
  });

  if (run > 0) {
    clues.push(run);
  }

  return clues.length > 0 ? clues : [0];
}

function buildPuzzle(id: string, solution: readonly boolean[][]): NonogramPuzzle {
  return {
    id,
    difficulty: 'easy',
    rows: solution.length,
    cols: solution[0]?.length ?? 0,
    rowClues: solution.map((row) => buildClues(row)),
    colClues: Array.from({ length: solution[0]?.length ?? 0 }, (_, colIndex) => (
      buildClues(solution.map((row) => row[colIndex]))
    )),
  };
}

function buildBoard(rows: readonly (readonly NonogramCellValue[])[]): NonogramBoard {
  return rows.map((row) => [...row]);
}

function createEmptyLessonBoard(): NonogramBoard {
  return createEmptyNonogramBoard(SOLUTION.length, SOLUTION[0]?.length ?? 0);
}

function assertBoardMatchesSolution(board: NonogramBoard): void {
  board.forEach((row, rowIndex) => {
    row.forEach((cell, colIndex) => {
      if (cell === null) {
        return;
      }

      const expected = SOLUTION[rowIndex]?.[colIndex] ? 1 : 0;
      if (cell !== expected) {
        throw new Error(`Nonogram tutorial board mismatch at ${rowIndex},${colIndex}.`);
      }
    });
  });
}

const PUZZLE = buildPuzzle('nonogram-tutorial-cross', SOLUTION);

const LESSON_DEFS: readonly Omit<NonogramTutorialLesson, 'title' | 'body' | 'summary' | 'continueLabel'>[] = [
  {
    key: 'tap-swipe',
    puzzle: PUZZLE,
    board: createEmptyLessonBoard(),
    evidenceCells: [],
    targetCells: [],
    highlightRows: [],
    highlightCols: [],
  },
  {
    key: 'overlap-fill',
    puzzle: PUZZLE,
    board: createEmptyLessonBoard(),
    evidenceCells: [{ row: 0, col: 0 }, { row: 0, col: 1 }, { row: 0, col: 2 }, { row: 0, col: 3 }, { row: 0, col: 4 }],
    targetCells: [{ row: 0, col: 2, value: 1 }],
    highlightRows: [0],
    highlightCols: [],
  },
  {
    key: 'forced-empty',
    puzzle: PUZZLE,
    board: buildBoard([
      [null, null, null, null, null],
      [1, null, null, null, null],
      [null, null, null, null, null],
      [null, null, null, null, null],
      [null, null, null, null, null],
    ]),
    evidenceCells: [{ row: 1, col: 0 }, { row: 1, col: 1 }, { row: 1, col: 2 }, { row: 1, col: 3 }, { row: 1, col: 4 }],
    targetCells: [{ row: 1, col: 1, value: 0 }],
    highlightRows: [1],
    highlightCols: [],
  },
  {
    key: 'complete-line',
    puzzle: PUZZLE,
    board: buildBoard([
      [null, null, null, null, null],
      [1, null, null, null, 1],
      [null, null, null, null, null],
      [null, null, null, null, null],
      [null, null, null, null, null],
    ]),
    evidenceCells: [{ row: 1, col: 0 }, { row: 1, col: 4 }],
    targetCells: [
      { row: 1, col: 1, value: 0 },
      { row: 1, col: 2, value: 0 },
      { row: 1, col: 3, value: 0 },
    ],
    highlightRows: [1],
    highlightCols: [],
  },
];

for (const lesson of LESSON_DEFS) {
  assertBoardMatchesSolution(lesson.board);
}

export function getNonogramTutorialLessons(): NonogramTutorialLesson[] {
  const copies = getNonogramTutorialLessonCopies();
  return LESSON_DEFS.map((lesson) => ({
    ...lesson,
    ...copies[lesson.key],
  }));
}
