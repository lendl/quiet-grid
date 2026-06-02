import type { PersistedSessionEnvelope } from '../../../../app/shell/types';
import type { PuzzleDifficulty } from '../../../shared/types';
import type { WordSearchSession } from '../../gameplay/activePuzzle';
import type {
  WordSearchCellRef,
  WordSearchDirection,
  WordSearchLanguage,
} from '../../types';
import { collectEmptyCells, directionToDelta, toGridKey } from '../../engine/gridUtils';


export interface WordSearchCatalogEntry {
  schemaVersion: 1;
  id: string;
  difficulty: PuzzleDifficulty;
  rows: number;
  cols: number;
  language: WordSearchLanguage;
  themeId: string;
  words: Array<{
    id: string;
    word: string;
    start: WordSearchCellRef;
    direction: WordSearchDirection;
  }>;
  hiddenWord: {
    word: string;
    clue: string;
    positions: Array<{ row: number; col: number }>;
  };
  diversitySignature: string;
  quality: {
    overlapRatio: number;
    directionEntropy: number;
    spreadRatio: number;
    deadZoneRatio: number;
    score: number;
  };
}

export interface WordSearchResolvedCatalogEntry {
  id: string;
  difficulty: PuzzleDifficulty;
  rows: number;
  cols: number;
  language: WordSearchLanguage;
  themeId: string;
  grid: string[][];
  words: Array<{ id: string; word: string; positions: WordSearchCellRef[] }>;
  hiddenWord: {
    word: string;
    clue: string;
    positions: WordSearchCellRef[];
  };
  diversitySignature: string;
  quality: WordSearchCatalogEntry['quality'];
}

function normalizeWordToken(value: string): string {
  return value
    .normalize('NFKD')
    .replace(/[^A-Za-z]/g, '')
    .toUpperCase();
}

function buildPositions(
  rows: number,
  cols: number,
  start: WordSearchCellRef,
  direction: WordSearchDirection,
  length: number,
): WordSearchCellRef[] {
  const delta = directionToDelta[direction];
  return Array.from({ length }, (_, index) => ({
    row: start.row + (delta.row * index),
    col: start.col + (delta.col * index),
  }));
}

function isInside(rows: number, cols: number, row: number, col: number): boolean {
  return row >= 0 && col >= 0 && row < rows && col < cols;
}

export function normalizeWordSearchCatalogEntry(entry: WordSearchCatalogEntry): WordSearchCatalogEntry {
  return {
    ...entry,
    schemaVersion: 1,
    rows: entry.rows,
    cols: entry.cols,
    words: entry.words.map((word) => ({
      ...word,
      word: normalizeWordToken(word.word),
      start: { ...word.start },
    })),
    hiddenWord: {
      ...entry.hiddenWord,
      word: normalizeWordToken(entry.hiddenWord.word),
      clue: entry.hiddenWord.clue,
      positions: entry.hiddenWord.positions.map((cell) => ({ ...cell })),
    },
    diversitySignature: entry.diversitySignature,
    quality: { ...entry.quality },
  };
}

export function materializeWordSearchCatalogEntry(
  entry: WordSearchCatalogEntry,
): WordSearchResolvedCatalogEntry {
  const normalized = normalizeWordSearchCatalogEntry(entry);
  const grid = Array.from(
    { length: normalized.rows },
    () => Array.from({ length: normalized.cols }, () => ''),
  );

  const resolvedWords = normalized.words.map((word) => {
    const positions = buildPositions(
      normalized.rows,
      normalized.cols,
      word.start,
      word.direction,
      word.word.length,
    );
    positions.forEach((cell, index) => {
      if (!isInside(normalized.rows, normalized.cols, cell.row, cell.col)) {
        throw new Error(`Word Search catalog entry ${normalized.id} has out-of-bounds placement.`);
      }
      const current = grid[cell.row][cell.col];
      const next = word.word[index] ?? '';
      if (current && current !== next) {
        throw new Error(`Word Search catalog entry ${normalized.id} has conflicting overlaps.`);
      }
      grid[cell.row][cell.col] = next;
    });
    return {
      id: word.id,
      word: word.word,
      positions,
    };
  });

  const hiddenWord = normalizeWordToken(normalized.hiddenWord.word);
  if (hiddenWord.length !== normalized.hiddenWord.positions.length) {
    throw new Error(`Word Search entry ${normalized.id}: hidden word length ${hiddenWord.length} !== positions count ${normalized.hiddenWord.positions.length}.`);
  }
  normalized.hiddenWord.positions.forEach((cell, index) => {
    if (isInside(normalized.rows, normalized.cols, cell.row, cell.col)) {
      grid[cell.row][cell.col] = hiddenWord[index] ?? '';
    }
  });

  return {
    id: normalized.id,
    difficulty: normalized.difficulty,
    rows: normalized.rows,
    cols: normalized.cols,
    language: normalized.language,
    themeId: normalized.themeId,
    grid,
    words: resolvedWords,
    hiddenWord: {
      word: hiddenWord,
      clue: normalized.hiddenWord.clue,
      positions: normalized.hiddenWord.positions.map((cell) => ({ ...cell })),
    },
    diversitySignature: normalized.diversitySignature,
    quality: { ...normalized.quality },
  };
}

export function serializeWordSearchSession(
  session: WordSearchSession,
): PersistedSessionEnvelope<WordSearchSession> {
  return {
    gameId: 'wordsearch',
    version: 1,
    payload: session,
  };
}

export function isWordSearchSessionPayload(value: unknown): value is WordSearchSession {
  return Boolean(value && typeof value === 'object' && 'puzzle' in (value as Record<string, unknown>));
}
