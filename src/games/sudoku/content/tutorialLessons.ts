import {
  createEmptySudokuNotes,
  type SudokuBoard,
  type SudokuDigit,
  type SudokuNotes,
  type SudokuSolution,
} from '../types';
import type { SudokuHintTargetCell } from '../gameplay/analysis/nextMove';
import { getLogicalSudokuCandidates } from '../gameplay/analysis/candidates';
import { getSudokuBoxIndex } from '../gameplay/rules/validation';
import {
  getSudokuTutorialLessonCopies,
  type SudokuTutorialLessonKey,
} from './i18n';

export interface SudokuTutorialLesson {
  key: SudokuTutorialLessonKey;
  title: string;
  body: string;
  summary: string;
  controlHint: string;
  continueLabel?: string;
  prompt?: string;
  options?: Record<string, string>;
  correctOptionKey?: string;
  correctFeedback?: string;
  wrongFeedback?: string;
  board: SudokuBoard;
  givens: SudokuBoard;
  notes: SudokuNotes;
  evidenceCells: Array<{ row: number; col: number }>;
  targetCells: SudokuHintTargetCell[];
  highlightRows: number[];
  highlightCols: number[];
  highlightBoxes: number[];
}

interface TutorialLessonDefinition {
  key: SudokuTutorialLessonKey;
  board: SudokuBoard;
  solution: SudokuSolution;
  notes: SudokuNotes;
  evidenceCells: Array<{ row: number; col: number }>;
  targetCells: SudokuHintTargetCell[];
  highlightRows: number[];
  highlightCols: number[];
  highlightBoxes: number[];
}

function cloneBoard(board: SudokuBoard): SudokuBoard {
  return board.map((row) => [...row]);
}

function createNotes(): SudokuNotes {
  return createEmptySudokuNotes(9, 9);
}

function setCellNotes(
  notes: SudokuNotes,
  row: number,
  col: number,
  digits: readonly SudokuDigit[],
): SudokuNotes {
  digits.forEach((digit) => {
    notes[row][col][digit - 1] = true;
  });
  return notes;
}

const SOLUTION_A: SudokuSolution = [
  [5, 3, 4, 6, 7, 8, 9, 1, 2],
  [6, 7, 2, 1, 9, 5, 3, 4, 8],
  [1, 9, 8, 3, 4, 2, 5, 6, 7],
  [8, 5, 9, 7, 6, 1, 4, 2, 3],
  [4, 2, 6, 8, 5, 3, 7, 9, 1],
  [7, 1, 3, 9, 2, 4, 8, 5, 6],
  [9, 6, 1, 5, 3, 7, 2, 8, 4],
  [2, 8, 7, 4, 1, 9, 6, 3, 5],
  [3, 4, 5, 2, 8, 6, 1, 7, 9],
];

const BOARD_A: SudokuBoard = [
  [5, 3, null, 6, 7, null, 9, 1, null],
  [6, null, 2, 1, null, 5, 3, null, 8],
  [null, 9, 8, null, 4, 2, null, 6, 7],
  [8, 5, null, 7, 6, null, 4, 2, null],
  [4, null, 6, 8, null, 3, 7, null, 1],
  [null, 1, 3, null, 2, 4, null, 5, 6],
  [9, 6, null, 5, 3, null, 2, 8, null],
  [2, null, 7, 4, null, 9, 6, null, 5],
  [null, 4, 5, null, 8, 6, null, 7, 9],
];

const SOLUTION_B: SudokuSolution = [
  [7, 6, 5, 1, 4, 2, 3, 9, 8],
  [3, 9, 8, 6, 5, 7, 2, 1, 4],
  [2, 1, 4, 9, 8, 3, 7, 6, 5],
  [6, 4, 7, 8, 2, 1, 9, 5, 3],
  [1, 8, 2, 5, 3, 9, 6, 4, 7],
  [9, 5, 3, 4, 7, 6, 1, 8, 2],
  [8, 3, 1, 7, 9, 5, 4, 2, 6],
  [5, 7, 9, 2, 6, 4, 8, 3, 1],
  [4, 2, 6, 3, 1, 8, 5, 7, 9],
];

const BOARD_B: SudokuBoard = [
  [null, 6, null, null, null, null, 3, null, null],
  [3, 9, null, null, 5, null, 2, 1, 4],
  [null, 1, null, null, null, null, null, 6, 5],
  [null, null, null, null, 2, null, 9, null, 3],
  [null, null, null, 5, null, 9, null, 4, null],
  [9, null, 3, null, null, 6, null, 8, 2],
  [null, 3, null, null, null, null, null, 2, 6],
  [null, 7, null, null, 6, 4, null, null, null],
  [4, 2, null, 3, 1, null, 5, null, null],
];

function buildLessonBNotes(): SudokuNotes {
  const notes = createNotes();

  setCellNotes(notes, 5, 1, [4, 5]);
  setCellNotes(notes, 5, 3, [1, 4, 7]);
  setCellNotes(notes, 5, 4, [4, 7]);
  setCellNotes(notes, 5, 6, [1, 7]);

  return notes;
}

const LESSON_DEFS: readonly TutorialLessonDefinition[] = [
  {
    key: 'goal',
    board: BOARD_A,
    solution: SOLUTION_A,
    notes: createNotes(),
    evidenceCells: [],
    targetCells: [],
    highlightRows: [],
    highlightCols: [],
    highlightBoxes: [],
  },
  {
    key: 'naked-single',
    board: BOARD_A,
    solution: SOLUTION_A,
    notes: createNotes(),
    evidenceCells: [
      { row: 0, col: 0 },
      { row: 0, col: 1 },
      { row: 0, col: 3 },
      { row: 0, col: 4 },
      { row: 0, col: 6 },
      { row: 0, col: 7 },
      { row: 1, col: 2 },
      { row: 2, col: 2 },
    ],
    targetCells: [{ row: 0, col: 2, digit: 4, action: 'place' }],
    highlightRows: [0],
    highlightCols: [2],
    highlightBoxes: [0],
  },
  {
    key: 'notes-mode',
    board: BOARD_B,
    solution: SOLUTION_B,
    notes: setCellNotes(createNotes(), 5, 1, [4, 5]),
    evidenceCells: [{ row: 5, col: 1 }],
    targetCells: [],
    highlightRows: [5],
    highlightCols: [1],
    highlightBoxes: [3],
  },
  {
    key: 'hidden-single',
    board: BOARD_B,
    solution: SOLUTION_B,
    notes: buildLessonBNotes(),
    evidenceCells: [
      { row: 5, col: 1 },
      { row: 5, col: 3 },
      { row: 5, col: 4 },
      { row: 5, col: 6 },
    ],
    targetCells: [{ row: 5, col: 1, digit: 5, action: 'place' }],
    highlightRows: [5],
    highlightCols: [],
    highlightBoxes: [],
  },
] as const;

function assertLessonBoardMatchesSolution(board: SudokuBoard, solution: SudokuSolution): void {
  board.forEach((row, rowIndex) => {
    row.forEach((value, colIndex) => {
      if (value === null) {
        return;
      }

      if (solution[rowIndex]?.[colIndex] !== value) {
        throw new Error(`Sudoku tutorial board mismatch at ${rowIndex},${colIndex}.`);
      }
    });
  });
}

function assertLessonNotesAreValid(
  board: SudokuBoard,
  notes: SudokuNotes,
): void {
  notes.forEach((row, rowIndex) => {
    row.forEach((cellNotes, colIndex) => {
      const logicalCandidates = getLogicalSudokuCandidates(board, rowIndex, colIndex);
      const logicalSet = new Set(logicalCandidates);

      if (board[rowIndex][colIndex] !== null && cellNotes.some(Boolean)) {
        throw new Error(`Sudoku tutorial notes must stay empty on filled cells (${rowIndex},${colIndex}).`);
      }

      cellNotes.forEach((enabled, noteIndex) => {
        if (!enabled) {
          return;
        }

        const digit = (noteIndex + 1) as SudokuDigit;
        if (!logicalSet.has(digit)) {
          throw new Error(`Sudoku tutorial note ${digit} is invalid at ${rowIndex},${colIndex}.`);
        }
      });
    });
  });
}

function assertLessonTargetsAreValid(
  lesson: TutorialLessonDefinition,
): void {
  lesson.targetCells.forEach((target) => {
    if (target.action === 'place') {
      if (lesson.solution[target.row]?.[target.col] !== target.digit) {
        throw new Error(`Sudoku tutorial target digit mismatch at ${target.row},${target.col}.`);
      }
      return;
    }

    const logical = getLogicalSudokuCandidates(lesson.board, target.row, target.col);
    if (!logical.includes(target.digit)) {
      throw new Error(`Sudoku tutorial elimination target is invalid at ${target.row},${target.col}.`);
    }
  });
}

for (const lesson of LESSON_DEFS) {
  assertLessonBoardMatchesSolution(lesson.board, lesson.solution);
  assertLessonNotesAreValid(lesson.board, lesson.notes);
  assertLessonTargetsAreValid(lesson);

  lesson.highlightBoxes.forEach((boxIndex) => {
    if (boxIndex < 0 || boxIndex > 8) {
      throw new Error(`Sudoku tutorial highlight box ${boxIndex} is out of range.`);
    }
  });

  lesson.targetCells.forEach((target) => {
    if (lesson.highlightBoxes.length > 0 && !lesson.highlightBoxes.includes(getSudokuBoxIndex(target.row, target.col))) {
      throw new Error(`Sudoku tutorial target ${target.row},${target.col} is outside the highlighted box.`);
    }
  });
}

export function getSudokuTutorialLessons(): SudokuTutorialLesson[] {
  const copies = getSudokuTutorialLessonCopies();

  return LESSON_DEFS.map((lesson) => ({
    ...lesson,
    board: cloneBoard(lesson.board),
    givens: cloneBoard(lesson.board),
    notes: lesson.notes.map((row) => row.map((cellNotes) => [...cellNotes])),
    ...copies[lesson.key],
  }));
}
