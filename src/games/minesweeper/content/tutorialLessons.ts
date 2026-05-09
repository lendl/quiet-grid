import type {
  MinesweeperBoard,
  MinesweeperCell,
  MinesweeperCellState,
} from '../types';
import {
  getMinesweeperTutorialText,
  type ActionLessonText,
  type InfoLessonText,
  type TutorialTextKey,
} from '../i18n';

export type MinesweeperTutorialAction = 'reveal' | 'flag';

export interface MinesweeperTutorialCell {
  row: number;
  col: number;
}

interface MinesweeperTutorialLessonBase {
  key: TutorialTextKey;
  title: string;
  body: string;
}

export interface MinesweeperTutorialActionLesson extends MinesweeperTutorialLessonBase {
  kind: 'action';
  prompt: string;
  expectedAction: MinesweeperTutorialAction;
  focusCell: MinesweeperTutorialCell;
  initialBoard: MinesweeperBoard;
  resultBoard: MinesweeperBoard;
  retry: string;
  success: string;
}

export interface MinesweeperTutorialInfoLesson extends MinesweeperTutorialLessonBase {
  kind: 'info';
  board: MinesweeperBoard;
  prompt: string;
  summary: string;
  continueLabel: string;
}

export type MinesweeperTutorialLesson =
  | MinesweeperTutorialActionLesson
  | MinesweeperTutorialInfoLesson;

type Coord = [row: number, col: number];

interface MinesweeperTutorialBoardPatchEntry {
  row: number;
  col: number;
  state: MinesweeperCellState;
}

interface MinesweeperTutorialActionLessonDefinition
  extends Omit<MinesweeperTutorialActionLesson, 'title' | 'body' | 'prompt' | 'retry' | 'success' | 'resultBoard'> {
  resultPatch: readonly MinesweeperTutorialBoardPatchEntry[];
}

interface MinesweeperTutorialInfoLessonDefinition {
  kind: 'info';
  key: TutorialTextKey;
  board: MinesweeperBoard;
}

type TutorialTextByKey = ReturnType<typeof getMinesweeperTutorialText>;

type ActionLessonKey = 'forced-flag' | 'safe-reveal' | 'compare-clues';

type InfoLessonKey = Exclude<TutorialTextKey, ActionLessonKey>;

interface MinesweeperTutorialActionLessonDefinitionBase
  extends Omit<MinesweeperTutorialActionLessonDefinition, 'key'> {
  key: ActionLessonKey;
}

interface MinesweeperTutorialInfoLessonDefinitionBase
  extends Omit<MinesweeperTutorialInfoLessonDefinition, 'key'> {
  key: InfoLessonKey;
}

function createCell(isMine: boolean, state: MinesweeperCellState): MinesweeperCell {
  return {
    isMine,
    adjacentMines: 0,
    state,
  };
}

function isNeighbor(row: number, col: number, mineRow: number, mineCol: number): boolean {
  return Math.abs(row - mineRow) <= 1
    && Math.abs(col - mineCol) <= 1
    && !(row === mineRow && col === mineCol);
}

function hasCoord(coords: readonly Coord[], row: number, col: number): boolean {
  return coords.some(([coordRow, coordCol]) => coordRow === row && coordCol === col);
}

function buildBoard({
  rows,
  cols,
  mines,
  revealed,
  flagged = [],
}: {
  rows: number;
  cols: number;
  mines: readonly Coord[];
  revealed: readonly Coord[];
  flagged?: readonly Coord[];
}): MinesweeperBoard {
  const cells = Array.from({ length: rows }, (_, row) => (
    Array.from({ length: cols }, (_, col) => {
      const isMine = hasCoord(mines, row, col);
      const state: MinesweeperCellState = hasCoord(flagged, row, col)
        ? 'flagged'
        : hasCoord(revealed, row, col)
          ? 'revealed'
          : 'hidden';

      return createCell(isMine, state);
    })
  ));

  for (let row = 0; row < rows; row++) {
    for (let col = 0; col < cols; col++) {
      cells[row][col].adjacentMines = mines.filter(([mineRow, mineCol]) => (
        isNeighbor(row, col, mineRow, mineCol)
      )).length;
    }
  }

  return {
    rows,
    cols,
    mines: mines.length,
    generated: true,
    status: 'playing',
    cells,
  };
}

function applyBoardPatch(
  board: MinesweeperBoard,
  patch: readonly MinesweeperTutorialBoardPatchEntry[],
): MinesweeperBoard {
  const cells = board.cells.map((row) => row.map((cell) => ({ ...cell })));

  for (const entry of patch) {
    cells[entry.row][entry.col] = {
      ...cells[entry.row][entry.col],
      state: entry.state,
    };
  }

  return {
    ...board,
    cells,
  };
}

function createActionLesson(
  definition: MinesweeperTutorialActionLessonDefinitionBase,
  text: ActionLessonText,
): MinesweeperTutorialActionLesson {
  const { resultPatch, ...lesson } = definition;

  return {
    ...lesson,
    title: text.title,
    body: text.body,
    prompt: text.prompt,
    retry: text.retry,
    success: text.success,
    resultBoard: applyBoardPatch(lesson.initialBoard, resultPatch),
  };
}

function createInfoLesson(
  definition: MinesweeperTutorialInfoLessonDefinitionBase,
  text: InfoLessonText,
): MinesweeperTutorialInfoLesson {
  return {
    ...definition,
    title: text.title,
    body: text.body,
    prompt: text.prompt,
    summary: text.summary,
    continueLabel: text.continueLabel,
  };
}

function getActionLessonText(
  text: TutorialTextByKey,
  key: ActionLessonKey,
): ActionLessonText {
  return text[key] as ActionLessonText;
}

function getInfoLessonText(
  text: TutorialTextByKey,
  key: InfoLessonKey,
): InfoLessonText {
  return text[key] as InfoLessonText;
}

const BOARD_SIZE = 5;

const GOAL_LESSON: MinesweeperTutorialInfoLessonDefinitionBase = {
  kind: 'info',
  key: 'goal-and-stakes',
  board: buildBoard({
    rows: BOARD_SIZE,
    cols: BOARD_SIZE,
    mines: [[0, 1], [1, 3], [3, 1]],
    revealed: [[2, 2], [2, 3], [3, 3], [4, 3], [4, 4]],
    flagged: [[1, 3]],
  }),
};

const CORE_ACTIONS_LESSON: MinesweeperTutorialInfoLessonDefinitionBase = {
  kind: 'info',
  key: 'core-actions',
  board: buildBoard({
    rows: BOARD_SIZE,
    cols: BOARD_SIZE,
    mines: [[1, 1], [3, 3]],
    revealed: [[0, 0], [0, 1], [1, 0], [2, 2]],
    flagged: [[3, 3]],
  }),
};

const READING_CLUES_LESSON: MinesweeperTutorialInfoLessonDefinitionBase = {
  kind: 'info',
  key: 'reading-clues',
  board: buildBoard({
    rows: BOARD_SIZE,
    cols: BOARD_SIZE,
    mines: [[1, 1], [1, 3], [3, 1]],
    revealed: [[0, 2], [2, 0], [2, 2], [2, 4]],
  }),
};

const FLAG_LESSON: MinesweeperTutorialActionLessonDefinitionBase = {
  key: 'forced-flag',
  kind: 'action',
  expectedAction: 'flag',
  focusCell: { row: 4, col: 1 },
  initialBoard: buildBoard({
    rows: BOARD_SIZE,
    cols: BOARD_SIZE,
    mines: [[4, 1], [0, 1], [0, 3]],
    revealed: [
      [1, 0],
      [1, 1],
      [1, 2],
      [1, 3],
      [1, 4],
      [2, 0],
      [2, 1],
      [2, 2],
      [2, 3],
      [2, 4],
      [3, 0],
      [3, 1],
      [3, 2],
      [3, 3],
      [3, 4],
      [4, 2],
      [4, 3],
      [4, 4],
    ],
  }),
  resultPatch: [{ row: 4, col: 1, state: 'flagged' }],
};

const SAFE_REVEAL_LESSON: MinesweeperTutorialActionLessonDefinitionBase = {
  key: 'safe-reveal',
  kind: 'action',
  expectedAction: 'reveal',
  focusCell: { row: 4, col: 0 },
  initialBoard: buildBoard({
    rows: BOARD_SIZE,
    cols: BOARD_SIZE,
    flagged: [[4, 1]],
    mines: [[4, 1], [0, 1], [0, 3]],
    revealed: [
      [1, 0],
      [1, 1],
      [1, 2],
      [1, 3],
      [1, 4],
      [2, 0],
      [2, 1],
      [2, 2],
      [2, 3],
      [2, 4],
      [3, 0],
      [3, 1],
      [3, 2],
      [3, 3],
      [3, 4],
      [4, 2],
      [4, 3],
      [4, 4],
    ],
  }),
  resultPatch: [{ row: 4, col: 0, state: 'revealed' }],
};

const ADVANCED_PATTERNS_LESSON: MinesweeperTutorialInfoLessonDefinitionBase = {
  kind: 'info',
  key: 'advanced-patterns',
  board: buildBoard({
    rows: BOARD_SIZE,
    cols: BOARD_SIZE,
    mines: [[1, 1], [0, 3]],
    flagged: [[1, 1]],
    revealed: [
      [0, 0],
      [0, 4],
      [1, 0],
      [1, 2],
      [1, 3],
      [1, 4],
      [2, 0],
      [2, 1],
      [2, 2],
      [2, 3],
      [2, 4],
    ],
  }),
};

const COMPARE_CLUES_LESSON: MinesweeperTutorialActionLessonDefinitionBase = {
  key: 'compare-clues',
  kind: 'action',
  expectedAction: 'reveal',
  focusCell: { row: 0, col: 2 },
  initialBoard: buildBoard({
    rows: BOARD_SIZE,
    cols: BOARD_SIZE,
    flagged: [[4, 1]],
    mines: [[4, 1], [0, 1], [0, 3]],
    revealed: [
      [1, 0],
      [1, 1],
      [1, 2],
      [1, 3],
      [1, 4],
      [2, 0],
      [2, 1],
      [2, 2],
      [2, 3],
      [2, 4],
      [3, 0],
      [3, 1],
      [3, 2],
      [3, 3],
      [3, 4],
      [4, 0],
      [4, 2],
      [4, 3],
      [4, 4],
    ],
  }),
  resultPatch: [{ row: 0, col: 2, state: 'revealed' }],
};

const GUESS_AND_HELP_LESSON: MinesweeperTutorialInfoLessonDefinitionBase = {
  kind: 'info',
  key: 'guess-and-help',
  board: buildBoard({
    rows: BOARD_SIZE,
    cols: BOARD_SIZE,
    mines: [[0, 0], [0, 3]],
    revealed: [
      [1, 0],
      [1, 1],
      [1, 2],
      [1, 3],
      [1, 4],
      [2, 0],
      [2, 1],
      [2, 2],
      [2, 3],
      [2, 4],
    ],
  }),
};

const TUTORIAL_LESSONS: readonly (
  | MinesweeperTutorialInfoLessonDefinitionBase
  | MinesweeperTutorialActionLessonDefinitionBase
)[] = [
  GOAL_LESSON,
  CORE_ACTIONS_LESSON,
  READING_CLUES_LESSON,
  FLAG_LESSON,
  SAFE_REVEAL_LESSON,
  COMPARE_CLUES_LESSON,
  ADVANCED_PATTERNS_LESSON,
  GUESS_AND_HELP_LESSON,
];

export function getMinesweeperTutorialLessons(): readonly MinesweeperTutorialLesson[] {
  const text = getMinesweeperTutorialText();
  return TUTORIAL_LESSONS.map((lesson) => {
    if (lesson.kind === 'info') {
      return createInfoLesson(lesson, getInfoLessonText(text, lesson.key));
    }

    return createActionLesson(lesson, getActionLessonText(text, lesson.key));
  });
}
