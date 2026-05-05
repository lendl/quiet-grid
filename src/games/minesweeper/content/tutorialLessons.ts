import type {
  MinesweeperBoard,
  MinesweeperCell,
  MinesweeperCellState,
} from '../types';
import { getCurrentLanguage } from '../../../app/i18n';

export type MinesweeperTutorialAction = 'reveal' | 'flag';

export interface MinesweeperTutorialCell {
  row: number;
  col: number;
}

interface MinesweeperTutorialLessonBase {
  key: string;
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
  extends Omit<MinesweeperTutorialActionLesson, 'resultBoard'> {
  resultPatch: readonly MinesweeperTutorialBoardPatchEntry[];
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
  definition: MinesweeperTutorialActionLessonDefinition,
): MinesweeperTutorialActionLesson {
  const { resultPatch, ...lesson } = definition;

  return {
    ...lesson,
    resultBoard: applyBoardPatch(lesson.initialBoard, resultPatch),
  };
}

const BOARD_SIZE = 5;

const FLAG_LESSON = createActionLesson({
  kind: 'action',
  key: 'forced-flag',
  title: 'Flag tile that must hide a mine',
  body: 'That 1 still needs one mine, and the highlighted tile is its only hidden neighbor.',
  prompt: 'What should you do with the highlighted tile?',
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
  retry: 'Look at the 1 beside the highlighted tile. It still needs one mine, and no other hidden neighbor can supply it.',
  success: 'Right. That clue still needed one mine, so the highlighted tile had to be flagged.',
});

const SAFE_REVEAL_LESSON = createActionLesson({
  kind: 'action',
  key: 'safe-reveal',
  title: 'Reveal tile that must be safe',
  body: 'This 1 already touches its flagged mine, so the highlighted tile cannot hide another one.',
  prompt: 'What should you do with the highlighted tile?',
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
  retry: 'That clue is already satisfied by the flagged mine. The highlighted tile is the remaining hidden neighbor, so it is safe.',
  success: 'Right. Once that clue already has its mine, the highlighted tile can be revealed safely.',
});

const DIAGONAL_LESSON: MinesweeperTutorialActionLesson = {
  kind: 'action',
  key: 'diagonals-count',
  title: 'Diagonal neighbors count too',
  body: 'The visible clues pin down the flagged mine first. After that, the corner 1 matters because it counts diagonal neighbors too.',
  prompt: 'What should you do with the highlighted tile?',
  expectedAction: 'reveal',
  focusCell: { row: 0, col: 1 },
  initialBoard: buildBoard({
    rows: BOARD_SIZE,
    cols: BOARD_SIZE,
    mines: [[1, 1]],
    flagged: [[1, 1]],
    revealed: [
      [0, 0],
      [0, 4],
      [1, 4],
      [2, 0],
      [2, 1],
      [2, 2],
      [2, 3],
      [2, 4],
      [3, 1],
      [3, 2],
      [3, 3],
      [3, 4],
      [4, 0],
      [4, 1],
      [4, 2],
      [4, 4],
    ],
  }),
  resultBoard: buildBoard({
    rows: BOARD_SIZE,
    cols: BOARD_SIZE,
    mines: [[1, 1]],
    flagged: [[1, 1]],
    revealed: [
      [0, 0],
      [0, 1],
      [0, 4],
      [1, 4],
      [2, 0],
      [2, 1],
      [2, 2],
      [2, 3],
      [2, 4],
      [3, 1],
      [3, 2],
      [3, 3],
      [3, 4],
      [4, 0],
      [4, 1],
      [4, 2],
      [4, 4],
    ],
  }),
  retry: 'The visible clues already force the flagged tile to be a mine. Once you include that diagonal mine in the corner 1, the highlighted tile is safe.',
  success: 'Right. The flagged tile is a known mine, and the corner clue counts it diagonally, so the highlighted tile can be revealed.',
};

const COMPARE_CLUES_LESSON = createActionLesson({
  kind: 'action',
  key: 'compare-clues',
  title: 'Compare two clues together',
  body: 'These two 1 clues share hidden tiles. Once the shared pair takes one mine, the extra tile by the right 1 must be safe.',
  prompt: 'What should you do with the highlighted tile?',
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
  retry: 'Read both 1 clues together. The shared hidden pair can contain only one mine, so the extra tile by the right clue is safe.',
  success: 'Right. Comparing both clues shows the highlighted tile cannot hide a mine.',
});

const GUESS_LESSON: MinesweeperTutorialInfoLesson = {
  kind: 'info',
  key: 'guess-moments',
  title: 'Sometimes next move is a guess',
  body: 'Not every puzzle offers a fully proved next move. On this board, the hidden top edge still supports more than one possible mine layout.',
  board: buildBoard({
    rows: BOARD_SIZE,
    cols: BOARD_SIZE,
    mines: [[0, 1], [0, 3], [4, 1], [4, 3]],
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
      [4, 4],
    ],
  }),
  prompt: 'When clues do not prove one move, make the calmest guess you can.',
  summary: 'More than one mine pattern can still fit the hidden top edge, so no single move is proved there yet.',
  continueLabel: 'Continue',
};

const ENGLISH_LESSONS: readonly MinesweeperTutorialLesson[] = [
  FLAG_LESSON,
  SAFE_REVEAL_LESSON,
  COMPARE_CLUES_LESSON,
  DIAGONAL_LESSON,
  GUESS_LESSON,
];

const DUTCH_TEXT = {
  'forced-flag': {
    title: 'Markeer het vak dat zeker een mijn verbergt',
    body: 'Die 1 heeft nog steeds één mijn nodig, en het gemarkeerde vak is de enige verborgen buur.',
    prompt: 'Wat moet je doen met het gemarkeerde vak?',
    retry: 'Kijk naar de 1 naast het gemarkeerde vak. Die heeft nog steeds één mijn nodig, en geen andere verborgen buur kan dat leveren.',
    success: 'Juist. Die aanwijzing had nog één mijn nodig, dus het gemarkeerde vak moest worden gemarkeerd.',
  },
  'safe-reveal': {
    title: 'Open het vak dat zeker veilig is',
    body: 'Deze 1 raakt zijn gemarkeerde mijn al, dus het gemarkeerde vak kan geen andere mijn verbergen.',
    prompt: 'Wat moet je doen met het gemarkeerde vak?',
    retry: 'Die aanwijzing is al voldaan door de gemarkeerde mijn. Het gemarkeerde vak is de overblijvende verborgen buur, dus het is veilig.',
    success: 'Juist. Zodra die aanwijzing zijn mijn al heeft, kun je het gemarkeerde vak veilig openen.',
  },
  'diagonals-count': {
    title: 'Diagonale buren tellen ook mee',
    body: 'De zichtbare aanwijzingen leggen eerst de gemarkeerde mijn vast. Daarna telt de hoek-1 mee omdat diagonale buren ook meetellen.',
    prompt: 'Wat moet je doen met het gemarkeerde vak?',
    retry: 'De zichtbare aanwijzingen forceren de gemarkeerde tegel al als mijn. Als je die diagonale mijn meetelt voor de hoek-1, is het gemarkeerde vak veilig.',
    success: 'Juist. De gemarkeerde tegel is een bekende mijn, en de hoekaanwijzing telt die diagonaal mee, dus het gemarkeerde vak kan worden geopend.',
  },
  'compare-clues': {
    title: 'Vergelijk twee aanwijzingen samen',
    body: 'Deze twee 1-aanwijzingen delen verborgen vakken. Zodra het gedeelde paar één mijn bevat, moet het extra vak bij de rechter 1 veilig zijn.',
    prompt: 'Wat moet je doen met het gemarkeerde vak?',
    retry: 'Lees beide 1-aanwijzingen samen. Het gedeelde verborgen paar kan maar één mijn bevatten, dus het extra vak bij de rechter aanwijzing is veilig.',
    success: 'Juist. Door beide aanwijzingen te vergelijken zie je dat het gemarkeerde vak geen mijn kan verbergen.',
  },
  'guess-moments': {
    title: 'Soms is de volgende zet een gok',
    body: 'Niet elke puzzel biedt een volledig bewezen volgende zet. Op dit bord ondersteunt de verborgen bovenrand nog meer dan één mogelijke mijnopstelling.',
    prompt: 'Wanneer aanwijzingen niet één zet bewijzen, maak dan de rustigste gok die je kunt.',
    summary: 'Er past nog meer dan één mijnpatroon in de verborgen bovenrand, dus daar is nog geen enkele zet bewezen.',
    continueLabel: 'Verder',
  },
} as const;

type DutchActionLessonText = {
  title: string;
  body: string;
  prompt: string;
  retry: string;
  success: string;
};

type DutchInfoLessonText = {
  title: string;
  body: string;
  prompt: string;
  summary: string;
  continueLabel: string;
};

const DUTCH_ACTION_TEXT: Record<'forced-flag' | 'safe-reveal' | 'diagonals-count' | 'compare-clues', DutchActionLessonText> = {
  'forced-flag': DUTCH_TEXT['forced-flag'],
  'safe-reveal': DUTCH_TEXT['safe-reveal'],
  'diagonals-count': DUTCH_TEXT['diagonals-count'],
  'compare-clues': DUTCH_TEXT['compare-clues'],
};

const DUTCH_INFO_TEXT: Record<'guess-moments', DutchInfoLessonText> = {
  'guess-moments': DUTCH_TEXT['guess-moments'],
};

export function getMinesweeperTutorialLessons(): readonly MinesweeperTutorialLesson[] {
  if (getCurrentLanguage() !== 'nl') {
    return ENGLISH_LESSONS;
  }

  return ENGLISH_LESSONS.map((lesson) => {
    if (lesson.kind === 'info') {
      const translated = DUTCH_INFO_TEXT[lesson.key as keyof typeof DUTCH_INFO_TEXT];

      return {
        ...lesson,
        title: translated.title,
        body: translated.body,
        prompt: translated.prompt,
        summary: translated.summary,
        continueLabel: translated.continueLabel,
      };
    }

    const translated = DUTCH_ACTION_TEXT[lesson.key as keyof typeof DUTCH_ACTION_TEXT];

    return {
      ...lesson,
      title: translated.title,
      body: translated.body,
      prompt: translated.prompt,
      retry: translated.retry,
      success: translated.success,
    };
  });
}
