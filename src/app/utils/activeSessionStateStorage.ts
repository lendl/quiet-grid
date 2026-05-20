import type {
  ActiveSession,
  TakuzuActiveSession,
  MinesweeperActiveSession,
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

function getStoredGameId(value: Record<string, unknown>): 'takuzu' | 'minesweeper' | null {
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

function isActiveSession(value: unknown): value is ActiveSession {
  if (!value || typeof value !== 'object') return false;
  const activeSession = value as Record<string, unknown>;
  if (getStoredGameId(activeSession) === 'takuzu') return isTakuzuActiveSession(activeSession);
  if (getStoredGameId(activeSession) === 'minesweeper') return isMinesweeperActiveSession(activeSession);
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
      return normalizeMinesweeperActiveSession(parsed as MinesweeperActiveSession);
    }
    if (isLegacyTakuzuActiveSession(parsed)) {
      return normalizeLegacyTakuzuSession(parsed as Record<string, unknown>);
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
