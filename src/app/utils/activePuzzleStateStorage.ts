import type {
  ActivePuzzle,
  TakuzuActivePuzzle,
  MinesweeperActivePuzzle,
  NonogramActivePuzzle,
} from '../shell/activePuzzleTypes';
import type { Difficulty } from '../types';
import {
  clearActivePuzzle,
  loadActivePuzzle as loadStoredActivePuzzle,
  saveActivePuzzle as saveStoredActivePuzzle,
} from '../shell/storage/activePuzzleStorage';
import type { PuzzleSessionEnvelope } from '../shell/types';

type LineKey = TakuzuActivePuzzle['penalizedLineKeys'][number];
type MinesweeperPuzzle = MinesweeperActivePuzzle['puzzle'];
type NonogramPuzzle = NonogramActivePuzzle['puzzle'];

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

function isHexPuzzleData(value: unknown, size: number): value is string {
  const encodedLength = Math.ceil((size * size) / 4);
  return typeof value === 'string' && value.length === encodedLength && HEX_PATTERN.test(value);
}

function getPuzzleTypeId(value: Record<string, unknown>): 'takuzu' | 'minesweeper' | 'nonogram' | null {
  if (value.puzzleTypeId === 'binary') {
    return 'takuzu';
  }
  if (
    value.puzzleTypeId === 'takuzu'
    || value.puzzleTypeId === 'minesweeper'
    || value.puzzleTypeId === 'nonogram'
  ) {
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

function isTakuzuActivePuzzle(value: unknown): value is TakuzuActivePuzzle {
  if (!value || typeof value !== 'object') return false;
  const obj = value as Record<string, unknown>;
  if (getPuzzleTypeId(obj) !== 'takuzu') return false;
  if (!isTakuzuPuzzle(obj.puzzle)) return false;
  const size = (obj.puzzle as Record<string, unknown>).size as number;
  return isGrid(obj.board, size)
    && isFiniteNonNegativeNumber(obj.elapsedSeconds)
    && isFiniteNonNegativeNumber(obj.accuracyDrops)
    && isBooleanGrid(obj.finishedCells, size)
    && isLineKeyArray(obj.penalizedLineKeys);
}

function isLegacyTakuzuActivePuzzle(value: unknown): boolean {
  if (!value || typeof value !== 'object') return false;
  const activePuzzle = value as Record<string, unknown>;
  if ('puzzleTypeId' in activePuzzle || 'gameType' in activePuzzle) return false;
  if (!isTakuzuPuzzle(activePuzzle.puzzle)) return false;
  const size = (activePuzzle.puzzle as Record<string, unknown>).size as number;
  return isGrid(activePuzzle.board, size)
    && isFiniteNonNegativeNumber(activePuzzle.elapsedSeconds)
    && isFiniteNonNegativeNumber(activePuzzle.accuracyDrops)
    && (activePuzzle.finishedCells === undefined || isBooleanGrid(activePuzzle.finishedCells, size))
    && (activePuzzle.penalizedLineKeys === undefined || isLineKeyArray(activePuzzle.penalizedLineKeys));
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

function isMinesweeperActivePuzzle(value: unknown): value is MinesweeperActivePuzzle {
  if (!value || typeof value !== 'object') return false;
  const obj = value as Record<string, unknown>;
  return getPuzzleTypeId(obj) === 'minesweeper'
    && isMinesweeperPuzzle(obj.puzzle)
    && isMinesweeperBoard(obj.board)
    && isFiniteNonNegativeNumber(obj.elapsedSeconds);
}

function isClueLine(value: unknown): boolean {
  return Array.isArray(value)
    && value.every((entry) => Number.isInteger(entry) && (entry as number) >= 0);
}

function isNonogramPuzzle(value: unknown): value is NonogramPuzzle {
  if (!value || typeof value !== 'object') return false;
  const puzzle = value as Record<string, unknown>;
  const rows = puzzle.rows;
  const cols = puzzle.cols;
  return typeof puzzle.id === 'string'
    && (puzzle.size === 5 || puzzle.size === 10)
    && Number.isInteger(rows)
    && Number.isInteger(cols)
    && rows === puzzle.size
    && cols === puzzle.size
    && isDifficulty(puzzle.difficulty)
    && isHexPuzzleData(puzzle.solution, (rows as number))
    && Array.isArray(puzzle.rowClues)
    && Array.isArray(puzzle.colClues)
    && (puzzle.rowClues as unknown[]).every(isClueLine)
    && (puzzle.colClues as unknown[]).every(isClueLine);
}

function isNonogramCellState(value: unknown): boolean {
  return value === 'empty' || value === 'filled' || value === 'marked';
}

function isNonogramCells(value: unknown, total: number): boolean {
  return Array.isArray(value) && value.length === total && value.every(isNonogramCellState);
}

function isNonogramActivePuzzle(value: unknown): value is NonogramActivePuzzle {
  if (!value || typeof value !== 'object') return false;
  const obj = value as Record<string, unknown>;
  if (getPuzzleTypeId(obj) !== 'nonogram' || !isNonogramPuzzle(obj.puzzle)) {
    return false;
  }

  return isNonogramCells(obj.cells, obj.puzzle.rows * obj.puzzle.cols)
    && isFiniteNonNegativeNumber(obj.elapsedSeconds);
}

function isActivePuzzle(value: unknown): value is ActivePuzzle {
  if (!value || typeof value !== 'object') return false;
  const activePuzzle = value as Record<string, unknown>;
  if (getPuzzleTypeId(activePuzzle) === 'takuzu') return isTakuzuActivePuzzle(activePuzzle);
  if (getPuzzleTypeId(activePuzzle) === 'minesweeper') return isMinesweeperActivePuzzle(activePuzzle);
  if (getPuzzleTypeId(activePuzzle) === 'nonogram') return isNonogramActivePuzzle(activePuzzle);
  return false;
}

function normalizeLegacyTakuzuPuzzle(raw: Record<string, unknown>): TakuzuActivePuzzle {
  const puzzle = normalizeTakuzuPuzzle(raw.puzzle as TakuzuActivePuzzle['puzzle']);
  return {
    puzzleTypeId: 'takuzu',
    puzzle,
    board: raw.board as TakuzuActivePuzzle['board'],
    elapsedSeconds: raw.elapsedSeconds as number,
    accuracyDrops: raw.accuracyDrops as number,
    finishedCells: (raw.finishedCells as boolean[][] | undefined)
      ?? makeEmptyBooleanGrid(puzzle.size),
    penalizedLineKeys: (raw.penalizedLineKeys as LineKey[] | undefined) ?? [],
  };
}

function normalizeTakuzuPuzzle(puzzle: TakuzuActivePuzzle['puzzle']): TakuzuActivePuzzle['puzzle'] {
  return {
    ...puzzle,
    rows: puzzle.size,
    cols: puzzle.size,
  };
}

function normalizeTakuzuActivePuzzle(raw: TakuzuActivePuzzle): TakuzuActivePuzzle {
  return {
    ...raw,
    puzzleTypeId: 'takuzu',
    puzzle: normalizeTakuzuPuzzle(raw.puzzle),
  };
}

function normalizeMinesweeperActivePuzzle(raw: MinesweeperActivePuzzle): MinesweeperActivePuzzle {
  return {
    ...raw,
    puzzleTypeId: 'minesweeper',
    puzzle: {
      ...raw.puzzle,
      rows: raw.puzzle.rows,
      cols: raw.puzzle.cols,
      mines: raw.puzzle.mines,
    },
  };
}

function normalizeNonogramPuzzle(puzzle: NonogramPuzzle): NonogramPuzzle {
  return {
    ...puzzle,
    rows: puzzle.size,
    cols: puzzle.size,
  };
}

function normalizeNonogramActivePuzzle(raw: NonogramActivePuzzle): NonogramActivePuzzle {
  return {
    ...raw,
    puzzleTypeId: 'nonogram',
    puzzle: normalizeNonogramPuzzle(raw.puzzle),
    cells: [...raw.cells],
  };
}

export function makeEmptyBooleanGrid(size: number): boolean[][] {
  return Array.from({ length: size }, (): boolean[] => Array.from({ length: size }, () => false));
}

export async function loadActivePuzzleState(): Promise<ActivePuzzle | null> {
  const stored = await loadStoredActivePuzzle();
  if (!stored) return null;

  try {
    const parsed: unknown = isPuzzleSessionEnvelope(stored)
      ? stored.payload
      : stored;

    if (isActivePuzzle(parsed)) {
      if (getPuzzleTypeId(parsed as unknown as Record<string, unknown>) === 'takuzu') {
        return normalizeTakuzuActivePuzzle(parsed as TakuzuActivePuzzle);
      }
      if (getPuzzleTypeId(parsed as unknown as Record<string, unknown>) === 'nonogram') {
        return normalizeNonogramActivePuzzle(parsed as NonogramActivePuzzle);
      }
      return normalizeMinesweeperActivePuzzle(parsed as MinesweeperActivePuzzle);
    }
    if (isLegacyTakuzuActivePuzzle(parsed)) {
      return normalizeLegacyTakuzuPuzzle(parsed as Record<string, unknown>);
    }
  } catch {
    // Invalid JSON or shape. Drop below and clear corrupted save.
  }

  await clearActivePuzzle();
  return null;
}

export async function saveActivePuzzleState(activePuzzle: ActivePuzzle): Promise<void> {
  const normalizedActivePuzzle = activePuzzle.puzzleTypeId === 'takuzu'
    ? normalizeTakuzuActivePuzzle(activePuzzle)
    : activePuzzle.puzzleTypeId === 'nonogram'
      ? normalizeNonogramActivePuzzle(activePuzzle)
      : normalizeMinesweeperActivePuzzle(activePuzzle);

  await saveStoredActivePuzzle({
    puzzleTypeId: normalizedActivePuzzle.puzzleTypeId,
    version: 1,
    payload: normalizedActivePuzzle,
  });
}

export async function clearActivePuzzleState(): Promise<void> {
  await clearActivePuzzle();
}

function isPuzzleSessionEnvelope(value: unknown): value is PuzzleSessionEnvelope {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const envelope = value as Record<string, unknown>;
  return (
     (envelope.puzzleTypeId === 'binary'
       || envelope.puzzleTypeId === 'takuzu'
       || envelope.puzzleTypeId === 'minesweeper'
       || envelope.puzzleTypeId === 'nonogram')
    && Number.isInteger(envelope.version)
    && 'payload' in envelope
  );
}
