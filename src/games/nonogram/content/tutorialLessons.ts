import type { NonogramCellState, NonogramTutorialLesson } from '../types';
import {
  getNonogramTutorialLessonCopies,
  type NonogramTutorialLessonKey,
} from './i18n';

type LessonConfig = Omit<NonogramTutorialLesson, 'title' | 'body' | 'prompt' | 'retry' | 'success'>;

function board(rows: number, cols: number, value: NonogramCellState = 'empty'): NonogramCellState[] {
  return Array.from({ length: rows * cols }, () => value);
}

const LESSONS: Record<NonogramTutorialLessonKey, LessonConfig> = {
  'read-clues': {
    key: 'read-clues',
    puzzle: {
      rows: 1,
      cols: 5,
      rowClues: [[3]],
      colClues: [[0], [1], [1], [1], [0]],
    },
    initialCells: board(1, 5),
    targetCells: [{ row: 0, col: 1 }, { row: 0, col: 2 }, { row: 0, col: 3 }],
    action: 'filled',
  },
  'forced-fill': {
    key: 'forced-fill',
    puzzle: {
      rows: 1,
      cols: 5,
      rowClues: [[4]],
      colClues: [[1], [1], [1], [1], [0]],
    },
    initialCells: board(1, 5),
    targetCells: [{ row: 0, col: 0 }, { row: 0, col: 1 }, { row: 0, col: 2 }, { row: 0, col: 3 }],
    action: 'filled',
  },
  'forced-mark': {
    key: 'forced-mark',
    puzzle: {
      rows: 1,
      cols: 5,
      rowClues: [[2]],
      colClues: [[1], [1], [0], [0], [0]],
    },
    initialCells: ['filled', 'filled', 'empty', 'empty', 'empty'],
    targetCells: [{ row: 0, col: 2 }, { row: 0, col: 3 }, { row: 0, col: 4 }],
    action: 'marked',
  },
  'combine-lines': {
    key: 'combine-lines',
    puzzle: {
      rows: 3,
      cols: 3,
      rowClues: [[1], [3], [1]],
      colClues: [[1], [3], [1]],
    },
    initialCells: [
      'empty', 'filled', 'empty',
      'filled', 'filled', 'empty',
      'empty', 'filled', 'empty',
    ],
    targetCells: [{ row: 1, col: 2 }],
    action: 'filled',
  },
  'tap-cycle': {
    key: 'tap-cycle',
    puzzle: {
      rows: 1,
      cols: 3,
      rowClues: [[1]],
      colClues: [[1], [0], [0]],
    },
    initialCells: ['filled', 'empty', 'empty'],
    targetCells: [{ row: 0, col: 1 }],
    action: 'marked',
  },
};

export function getNonogramTutorialLessons(): NonogramTutorialLesson[] {
  const copies = getNonogramTutorialLessonCopies();
  return (Object.keys(LESSONS) as NonogramTutorialLessonKey[]).map((key) => ({
    ...LESSONS[key],
    ...copies[key],
  }));
}
