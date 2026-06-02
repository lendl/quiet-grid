import type {
  ActiveSession,
  TakuzuActiveSession,
  MinesweeperActiveSession,
  NonogramActiveSession,
  SudokuActiveSession,
  WordSearchActiveSession,
} from '../shell/activeSessionTypes';
import type { Difficulty } from '../types';
import { isGameId } from '../../games/shared/types';
import {
  clearActiveSession,
  loadActiveSession as loadStoredActiveSession,
  saveActiveSession as saveStoredActiveSession,
} from '../shell/storage/activeSessionStorage';
import type { PersistedSessionEnvelope } from '../shell/types';

type LineKey = TakuzuActiveSession['penalizedLineKeys'][number];
type MinesweeperPuzzle = MinesweeperActiveSession['puzzle'];
type NonogramPuzzle = NonogramActiveSession['puzzle'];
type SudokuPuzzle = SudokuActiveSession['puzzle'];
type SudokuUnitKey = SudokuActiveSession['validatedUnitKeys'][number];
type WordSearchCellRef = WordSearchActiveSession['puzzle']['words'][number]['positions'][number];

const HEX_PATTERN = /^[\da-f]+$/i;

function isCellValue(value: unknown): value is 0 | 1 | null {
  return value === 0 || value === 1 || value === null;
}

function isGrid(value: unknown, size: number): boolean {
  return Array.isArray(value)
    && value.length === size
    && value.every((row) => Array.isArray(row) && row.length === size && row.every((cell) => isCellValue(cell)));
}

function isBooleanGrid(value: unknown, size: number): boolean {
  return Array.isArray(value)
    && value.length === size
    && value.every((row) => Array.isArray(row) && row.length === size && row.every((cell) => typeof cell === 'boolean'));
}

function isBooleanMatrix(value: unknown, rows: number, cols: number): boolean {
  return Array.isArray(value)
    && value.length === rows
    && value.every((row) => Array.isArray(row) && row.length === cols && row.every((cell) => typeof cell === 'boolean'));
}

function isTriStateMatrix(value: unknown, rows: number, cols: number): boolean {
  return Array.isArray(value)
    && value.length === rows
    && value.every((row) => Array.isArray(row)
      && row.length === cols
      && row.every((cell) => cell === 0 || cell === 1 || cell === null));
}

function isLineKey(value: unknown): value is LineKey {
  return typeof value === 'string' && /^[rc]\d+$/.test(value);
}

function isLineKeyArray(value: unknown): value is LineKey[] {
  return Array.isArray(value) && value.every((lk) => isLineKey(lk));
}

function isDifficulty(value: unknown): value is Difficulty {
  return value === 'easy' || value === 'medium' || value === 'hard' || value === 'expert';
}

function isFiniteNonNegativeNumber(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0;
}

function isSudokuDigit(value: unknown): value is 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 {
  return Number.isInteger(value) && (value as number) >= 1 && (value as number) <= 9;
}

function isSudokuCellValue(value: unknown): value is SudokuActiveSession['board'][number][number] {
  return value === null || isSudokuDigit(value);
}

function isSudokuBoard(value: unknown, rows: number, cols: number): boolean {
  return Array.isArray(value)
    && value.length === rows
    && value.every((row) => Array.isArray(row)
      && row.length === cols
      && row.every((cell) => isSudokuCellValue(cell)));
}

function isSudokuSolution(value: unknown, rows: number, cols: number): boolean {
  return Array.isArray(value)
    && value.length === rows
    && value.every((row) => Array.isArray(row)
      && row.length === cols
      && row.every((cell) => isSudokuDigit(cell)));
}

function isSudokuNotes(value: unknown, rows: number, cols: number): boolean {
  return Array.isArray(value)
    && value.length === rows
    && value.every((row) => Array.isArray(row)
      && row.length === cols
      && row.every((cellNotes) => Array.isArray(cellNotes)
        && cellNotes.length === 9
        && cellNotes.every((note) => typeof note === 'boolean')));
}

function isSudokuInputMode(value: unknown): value is SudokuActiveSession['inputMode'] {
  return value === 'digit' || value === 'notes';
}

function isSudokuUnitKey(value: unknown): value is SudokuUnitKey {
  return typeof value === 'string' && /^[rcb]\d+$/.test(value);
}

function isSudokuUnitKeyArray(value: unknown): value is SudokuUnitKey[] {
  return Array.isArray(value) && value.every((unitKey) => isSudokuUnitKey(unitKey));
}

function isHexPuzzleData(value: unknown, size: number): value is string {
  const encodedLength = Math.ceil((size * size) / 4);
  return typeof value === 'string' && value.length === encodedLength && HEX_PATTERN.test(value);
}

function getStoredGameId(
  value: Record<string, unknown>,
): 'takuzu' | 'minesweeper' | 'nonogram' | 'sudoku' | 'wordsearch' | null {
  if (value.puzzleTypeId === 'binary') {
    return 'takuzu';
  }
  if (isGameId(value.gameId)) {
    return value.gameId;
  }
  if (isGameId(value.puzzleTypeId)) {
    return value.puzzleTypeId;
  }
  return null;
}

function isTakuzuPuzzle(value: unknown): boolean {
  if (!value || typeof value !== 'object') return false;
  const puzzle = value as Record<string, unknown>;
  const size = puzzle.size;
  return typeof puzzle.id === 'string'
    && (size === 6 || size === 8 || size === 10)
    && (puzzle.rows === undefined || puzzle.rows === size)
    && (puzzle.cols === undefined || puzzle.cols === size)
    && isDifficulty(puzzle.difficulty)
    && isHexPuzzleData(puzzle.solution, size)
    && isHexPuzzleData(puzzle.mask, size);
}

function isTakuzuActiveSession(value: unknown): value is TakuzuActiveSession {
  if (!value || typeof value !== 'object') return false;
  const obj = value as Record<string, unknown>;
  if (getStoredGameId(obj) !== 'takuzu') return false;
  if (!isTakuzuPuzzle(obj.puzzle)) return false;
  const size = (obj.puzzle as Record<string, unknown>).size as number;
  return isGrid(obj.board, size)
    && isFiniteNonNegativeNumber(obj.elapsedSeconds)
    && isFiniteNonNegativeNumber(obj.accuracyDrops)
    && isBooleanGrid(obj.finishedCells, size)
    && isLineKeyArray(obj.penalizedLineKeys);
}

function isLegacyTakuzuActiveSession(value: unknown): boolean {
  if (!value || typeof value !== 'object') return false;
  const activeSession = value as Record<string, unknown>;
  if ('puzzleTypeId' in activeSession || 'gameId' in activeSession || 'gameType' in activeSession) return false;
  if (!isTakuzuPuzzle(activeSession.puzzle)) return false;
  const size = (activeSession.puzzle as Record<string, unknown>).size as number;
  return isGrid(activeSession.board, size)
    && isFiniteNonNegativeNumber(activeSession.elapsedSeconds)
    && isFiniteNonNegativeNumber(activeSession.accuracyDrops)
    && (activeSession.finishedCells === undefined || isBooleanGrid(activeSession.finishedCells, size))
    && (activeSession.penalizedLineKeys === undefined || isLineKeyArray(activeSession.penalizedLineKeys));
}

function isMinesweeperPuzzle(value: unknown): value is MinesweeperPuzzle {
  if (!value || typeof value !== 'object') return false;
  const puzzle = value as Record<string, unknown>;
  return isDifficulty(puzzle.difficulty)
    && Number.isInteger(puzzle.rows)
    && Number.isInteger(puzzle.cols)
    && Number.isInteger(puzzle.mines)
    && (puzzle.rows as number) > 0
    && (puzzle.cols as number) > 0
    && (puzzle.mines as number) >= 0;
}

function isMinesweeperCellState(value: unknown): boolean {
  return value === 'hidden' || value === 'revealed' || value === 'flagged';
}

function isMinesweeperCell(value: unknown): boolean {
  if (!value || typeof value !== 'object') return false;
  const cell = value as Record<string, unknown>;
  return typeof cell.isMine === 'boolean'
    && typeof cell.adjacentMines === 'number'
    && Number.isInteger(cell.adjacentMines)
    && cell.adjacentMines >= 0
    && cell.adjacentMines <= 8
    && isMinesweeperCellState(cell.state);
}

function isMinesweeperBoard(value: unknown): boolean {
  if (!value || typeof value !== 'object') return false;
  const board = value as Record<string, unknown>;
  if (
    !Number.isInteger(board.rows)
    || !Number.isInteger(board.cols)
    || !Number.isInteger(board.mines)
    || typeof board.generated !== 'boolean'
  ) {
    return false;
  }

  const rows = board.rows as number;
  const cols = board.cols as number;
  const mines = board.mines as number;
  const generated = board.generated;
  if (rows <= 0 || cols <= 0 || mines < 0) return false;
  if (board.status !== 'playing' && board.status !== 'won' && board.status !== 'lost') return false;
  if (!Array.isArray(board.cells) || board.cells.length !== rows) return false;

  const cells = board.cells as unknown[];
  if (!cells.every((row) => Array.isArray(row) && row.length === cols && row.every(isMinesweeperCell))) {
    return false;
  }

  const flatCells = (cells as unknown[][]).flat();
  const mineCount = flatCells.filter((cell) => (cell as Record<string, unknown>).isMine === true).length;
  if (!generated) {
    return flatCells.every((cell) => {
      const typedCell = cell as Record<string, unknown>;
      return typedCell.state === 'hidden'
        && typedCell.isMine === false
        && typedCell.adjacentMines === 0;
    });
  }

  return mineCount === mines;
}

function isMinesweeperActiveSession(value: unknown): value is MinesweeperActiveSession {
  if (!value || typeof value !== 'object') return false;
  const obj = value as Record<string, unknown>;
  return getStoredGameId(obj) === 'minesweeper'
    && isMinesweeperPuzzle(obj.puzzle)
    && isMinesweeperBoard(obj.board)
    && isFiniteNonNegativeNumber(obj.elapsedSeconds);
}

function isNonogramClueLine(value: unknown): boolean {
  return Array.isArray(value)
    && value.every((segment) => Number.isInteger(segment) && (segment as number) > 0);
}

function isNonogramPuzzle(value: unknown): value is NonogramPuzzle {
  if (!value || typeof value !== 'object') return false;
  const puzzle = value as Record<string, unknown>;
  const rows = puzzle.rows;
  const cols = puzzle.cols;
  const rowClues = puzzle.rowClues;
  const colClues = puzzle.colClues;
  return typeof puzzle.id === 'string'
    && typeof rows === 'number'
    && Number.isInteger(rows)
    && rows > 0
    && typeof cols === 'number'
    && Number.isInteger(cols)
    && cols > 0
    && isDifficulty(puzzle.difficulty)
    && Array.isArray(rowClues)
    && rowClues.length === rows
    && rowClues.every(isNonogramClueLine)
    && Array.isArray(colClues)
    && colClues.length === cols
    && colClues.every(isNonogramClueLine);
}

function isNonogramActiveSession(value: unknown): value is NonogramActiveSession {
  if (!value || typeof value !== 'object') return false;
  const obj = value as Record<string, unknown>;
  if (getStoredGameId(obj) !== 'nonogram' || !isNonogramPuzzle(obj.puzzle)) {
    return false;
  }

  const puzzle = obj.puzzle;

  return isTriStateMatrix(obj.board, puzzle.rows, puzzle.cols)
    && isBooleanMatrix(obj.solution, puzzle.rows, puzzle.cols)
    && isFiniteNonNegativeNumber(obj.elapsedSeconds);
}

function isSudokuPuzzle(value: unknown): value is SudokuPuzzle {
  if (!value || typeof value !== 'object') return false;
  const puzzle = value as Record<string, unknown>;
  const rows = puzzle.rows;
  const cols = puzzle.cols;
  return typeof puzzle.id === 'string'
    && rows === 9
    && cols === 9
    && isDifficulty(puzzle.difficulty)
    && isSudokuBoard(puzzle.givens, rows, cols)
    && isSudokuSolution(puzzle.solution, rows, cols);
}

function isSudokuActiveSession(value: unknown): value is SudokuActiveSession {
  if (!value || typeof value !== 'object') return false;
  const obj = value as Record<string, unknown>;
  if (getStoredGameId(obj) !== 'sudoku' || !isSudokuPuzzle(obj.puzzle)) {
    return false;
  }

  const puzzle = obj.puzzle;

  return isSudokuBoard(obj.board, puzzle.rows, puzzle.cols)
    && isSudokuNotes(obj.notes, puzzle.rows, puzzle.cols)
    && isBooleanMatrix(obj.finishedCells, puzzle.rows, puzzle.cols)
    && isSudokuInputMode(obj.inputMode)
    && (obj.selectedNoteDigit === null || isSudokuDigit(obj.selectedNoteDigit))
    && isFiniteNonNegativeNumber(obj.accuracyDrops)
    && isSudokuUnitKeyArray(obj.validatedUnitKeys)
    && isSudokuUnitKeyArray(obj.penalizedUnitKeys)
    && isFiniteNonNegativeNumber(obj.elapsedSeconds);
}

function isLegacySudokuActiveSession(value: unknown): boolean {
  if (!value || typeof value !== 'object') return false;
  const obj = value as Record<string, unknown>;
  if (getStoredGameId(obj) !== 'sudoku' || !isSudokuPuzzle(obj.puzzle)) {
    return false;
  }

  const puzzle = obj.puzzle;

  return isSudokuBoard(obj.board, puzzle.rows, puzzle.cols)
    && isFiniteNonNegativeNumber(obj.elapsedSeconds);
}


function isWordSearchCellRef(value: unknown): value is WordSearchCellRef {
  return Boolean(value && typeof value === 'object'
    && Number.isInteger((value as { row?: unknown }).row)
    && Number.isInteger((value as { col?: unknown }).col));
}

function isWordSearchGrid(value: unknown, rows: number, cols: number): boolean {
  return Array.isArray(value)
    && value.length === rows
    && value.every((row) => Array.isArray(row)
      && row.length === cols
      && row.every((cell) => typeof cell === 'string' && cell.length === 1));
}

function isWordSearchPuzzle(value: unknown): value is WordSearchActiveSession['puzzle'] {
  if (!value || typeof value !== 'object') return false;
  const puzzle = value as Record<string, unknown>;
  const rows = puzzle.rows;
  const cols = puzzle.cols;

  return typeof puzzle.id === 'string'
    && (puzzle.language === 'en' || puzzle.language === 'nl' || puzzle.language === 'de' || puzzle.language === 'fr' || puzzle.language === 'es')
    && typeof puzzle.themeId === 'string'
    && typeof rows === 'number'
    && Number.isInteger(rows)
    && rows > 0
    && typeof cols === 'number'
    && Number.isInteger(cols)
    && cols > 0
    && isDifficulty(puzzle.difficulty)
    && isWordSearchGrid(puzzle.grid, rows, cols)
    && Array.isArray(puzzle.words)
    && puzzle.words.every((word) => Boolean(word)
      && typeof word === 'object'
      && typeof (word as { id?: unknown }).id === 'string'
      && typeof (word as { word?: unknown }).word === 'string'
      && Array.isArray((word as { positions?: unknown }).positions)
      && (word as { positions: unknown[] }).positions.every(isWordSearchCellRef))
    && Boolean(puzzle.hiddenWord)
    && typeof (puzzle.hiddenWord as { word?: unknown }).word === 'string'
    && typeof (puzzle.hiddenWord as { clue?: unknown }).clue === 'string'
    && Array.isArray((puzzle.hiddenWord as { positions?: unknown }).positions)
    && (puzzle.hiddenWord as { positions: unknown[] }).positions.every(isWordSearchCellRef);
}

function isWordSearchSelection(value: unknown): boolean {
  if (value === null) {
    return true;
  }

  if (!value || typeof value !== 'object') {
    return false;
  }

  const selection = value as Record<string, unknown>;
  return isWordSearchCellRef(selection.start)
    && isWordSearchCellRef(selection.end)
    && Array.isArray(selection.path)
    && selection.path.every(isWordSearchCellRef);
}

function isWordSearchActiveSession(value: unknown): value is WordSearchActiveSession {
  if (!value || typeof value !== 'object') return false;
  const obj = value as Record<string, unknown>;

  return getStoredGameId(obj) === 'wordsearch'
    && isWordSearchPuzzle(obj.puzzle)
    && Array.isArray(obj.foundWordIds)
    && obj.foundWordIds.every((id) => typeof id === 'string')
    && isWordSearchSelection(obj.tempSelection)
    && isFiniteNonNegativeNumber(obj.elapsedSeconds);
}

function normalizeWordSearchActiveSession(raw: WordSearchActiveSession): WordSearchActiveSession {
  return {
    ...raw,
    gameId: 'wordsearch',
    puzzle: {
      ...raw.puzzle,
      grid: raw.puzzle.grid.map((row) => [...row]),
      words: raw.puzzle.words.map((word) => ({
        ...word,
        positions: word.positions.map((cell) => ({ ...cell })),
      })),
      hiddenWord: {
        ...raw.puzzle.hiddenWord,
        positions: raw.puzzle.hiddenWord.positions.map((cell) => ({ ...cell })),
      },
    },
    foundWordIds: [...raw.foundWordIds],
    tempSelection: raw.tempSelection
      ? {
          start: { ...raw.tempSelection.start },
          end: { ...raw.tempSelection.end },
          path: raw.tempSelection.path.map((cell) => ({ ...cell })),
        }
      : null,
  };
}
function isActiveSession(value: unknown): value is ActiveSession {
  if (!value || typeof value !== 'object') return false;
  const activeSession = value as Record<string, unknown>;
  if (getStoredGameId(activeSession) === 'takuzu') return isTakuzuActiveSession(activeSession);
  if (getStoredGameId(activeSession) === 'minesweeper') return isMinesweeperActiveSession(activeSession);
  if (getStoredGameId(activeSession) === 'nonogram') return isNonogramActiveSession(activeSession);
  if (getStoredGameId(activeSession) === 'sudoku') return isSudokuActiveSession(activeSession);
  if (getStoredGameId(activeSession) === 'wordsearch') return isWordSearchActiveSession(activeSession);
  return false;
}

function normalizeLegacyTakuzuSession(raw: Record<string, unknown>): TakuzuActiveSession {
  const puzzle = normalizeTakuzuPuzzle(raw.puzzle as TakuzuActiveSession['puzzle']);
  return {
    gameId: 'takuzu',
    puzzle,
    board: raw.board as TakuzuActiveSession['board'],
    elapsedSeconds: raw.elapsedSeconds as number,
    accuracyDrops: raw.accuracyDrops as number,
    finishedCells: (raw.finishedCells as boolean[][] | undefined)
      ?? makeEmptyBooleanGrid(puzzle.size),
    penalizedLineKeys: (raw.penalizedLineKeys as LineKey[] | undefined) ?? [],
  };
}

function normalizeTakuzuPuzzle(puzzle: TakuzuActiveSession['puzzle']): TakuzuActiveSession['puzzle'] {
  return {
    ...puzzle,
    rows: puzzle.size,
    cols: puzzle.size,
  };
}

function normalizeTakuzuActiveSession(raw: TakuzuActiveSession): TakuzuActiveSession {
  return {
    ...raw,
    gameId: 'takuzu',
    puzzle: normalizeTakuzuPuzzle(raw.puzzle),
  };
}

function normalizeMinesweeperActiveSession(raw: MinesweeperActiveSession): MinesweeperActiveSession {
  return {
    ...raw,
    gameId: 'minesweeper',
    puzzle: {
      ...raw.puzzle,
      rows: raw.puzzle.rows,
      cols: raw.puzzle.cols,
      mines: raw.puzzle.mines,
    },
  };
}

function normalizeNonogramActiveSession(raw: NonogramActiveSession): NonogramActiveSession {
  return {
    ...raw,
    gameId: 'nonogram',
    board: raw.board.map((row) => [...row]),
    solution: raw.solution.map((row) => [...row]),
  };
}

function normalizeSudokuActiveSession(raw: SudokuActiveSession): SudokuActiveSession {
  return {
    ...raw,
    gameId: 'sudoku',
    puzzle: {
      ...raw.puzzle,
      givens: raw.puzzle.givens.map((row) => [...row]),
      solution: raw.puzzle.solution.map((row) => [...row]),
    },
    board: raw.board.map((row) => [...row]),
    notes: raw.notes.map((row) => row.map((cellNotes) => [...cellNotes])),
    finishedCells: raw.finishedCells.map((row) => [...row]),
    accuracyDrops: raw.accuracyDrops,
    validatedUnitKeys: [...raw.validatedUnitKeys],
    penalizedUnitKeys: [...raw.penalizedUnitKeys],
  };
}

function normalizeLegacySudokuActiveSession(raw: Record<string, unknown>): SudokuActiveSession {
  const puzzle = raw.puzzle as SudokuActiveSession['puzzle'];
  return {
    gameId: 'sudoku',
    puzzle: {
      ...puzzle,
      givens: puzzle.givens.map((row) => [...row]),
      solution: puzzle.solution.map((row) => [...row]),
    },
    board: (raw.board as SudokuActiveSession['board']).map((row) => [...row]),
    notes: Array.from({ length: puzzle.rows }, () => Array.from({ length: puzzle.cols }, () => Array.from({ length: 9 }, () => false))),
    finishedCells: makeEmptyBooleanGrid(puzzle.rows),
    inputMode: 'digit',
    selectedNoteDigit: null,
    elapsedSeconds: raw.elapsedSeconds as number,
    accuracyDrops: 0,
    validatedUnitKeys: [],
    penalizedUnitKeys: [],
  };
}

export function makeEmptyBooleanGrid(size: number): boolean[][] {
  return Array.from({ length: size }, (): boolean[] => Array.from({ length: size }, () => false));
}

export async function loadActiveSessionState(): Promise<ActiveSession | null> {
  const stored = await loadStoredActiveSession();
  if (!stored) return null;

  try {
    const parsed: unknown = isPersistedSessionEnvelope(stored)
      ? stored.payload
      : stored;

    if (isActiveSession(parsed)) {
      const parsedSession = parsed as unknown as Record<string, unknown>;
      if (getStoredGameId(parsedSession) === 'takuzu') {
        return normalizeTakuzuActiveSession(parsed as TakuzuActiveSession);
      }
      if (getStoredGameId(parsedSession) === 'nonogram') {
        return normalizeNonogramActiveSession(parsed as NonogramActiveSession);
      }
      if (getStoredGameId(parsedSession) === 'sudoku') {
        return normalizeSudokuActiveSession(parsed as SudokuActiveSession);
      }
      if (getStoredGameId(parsedSession) === 'wordsearch') {
        return normalizeWordSearchActiveSession(parsed as WordSearchActiveSession);
      }
      return normalizeMinesweeperActiveSession(parsed as MinesweeperActiveSession);
    }
    if (isLegacyTakuzuActiveSession(parsed)) {
      return normalizeLegacyTakuzuSession(parsed as Record<string, unknown>);
    }
    if (isLegacySudokuActiveSession(parsed)) {
      return normalizeLegacySudokuActiveSession(parsed as Record<string, unknown>);
    }
  } catch {
    // Invalid JSON or shape. Drop below and clear corrupted save.
  }

  await clearActiveSession();
  return null;
}

export async function saveActiveSessionState(activeSession: ActiveSession): Promise<void> {
  const normalizedActiveSession = activeSession.gameId === 'takuzu'
    ? normalizeTakuzuActiveSession(activeSession)
    : activeSession.gameId === 'nonogram'
      ? normalizeNonogramActiveSession(activeSession)
      : activeSession.gameId === 'sudoku'
        ? normalizeSudokuActiveSession(activeSession)
        : activeSession.gameId === 'wordsearch'
          ? normalizeWordSearchActiveSession(activeSession)
          : normalizeMinesweeperActiveSession(activeSession);

  await saveStoredActiveSession({
    gameId: normalizedActiveSession.gameId,
    version: 1,
    payload: normalizedActiveSession,
  });
}

export async function clearActiveSessionState(): Promise<void> {
  await clearActiveSession();
}

function isPersistedSessionEnvelope(value: unknown): value is PersistedSessionEnvelope {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const envelope = value as Record<string, unknown>;
  return (
    getStoredGameId(envelope) !== null
    && Number.isInteger(envelope.version)
    && 'payload' in envelope
  );
}

