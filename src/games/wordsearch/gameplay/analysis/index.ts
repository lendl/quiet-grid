import type { PuzzleAnalysisPayload, PuzzleAnalysisSource } from '../../../../app/analysis/types';
import {
  cloneWordSearchPuzzle,
  cloneWordSearchSelection,
  type WordSearchSession,
} from '../../types';
import { getWordSearchNextMoveHint } from './nextMove';

interface WordSearchAnalysisSource extends PuzzleAnalysisSource {
  payload: WordSearchSession;
}

function isCell(value: unknown): value is { row: number; col: number } {
  return Boolean(
    value
    && typeof value === 'object'
    && Number.isInteger((value as { row?: unknown }).row)
    && Number.isInteger((value as { col?: unknown }).col),
  );
}

function isGrid(value: unknown, rows: number, cols: number): boolean {
  return Array.isArray(value)
    && value.length === rows
    && value.every((row) => Array.isArray(row)
      && row.length === cols
      && row.every((cell) => typeof cell === 'string' && cell.length === 1));
}

function isWordSearchSession(value: unknown): value is WordSearchSession {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const session = value as Partial<WordSearchSession>;
  if (!session.puzzle || typeof session.puzzle !== 'object') {
    return false;
  }

  const puzzle = session.puzzle as WordSearchSession['puzzle'];
  return typeof puzzle.id === 'string'
    && (puzzle.difficulty === 'easy' || puzzle.difficulty === 'medium' || puzzle.difficulty === 'hard' || puzzle.difficulty === 'expert')
    && Number.isInteger(puzzle.rows)
    && Number.isInteger(puzzle.cols)
    && puzzle.rows > 0
    && puzzle.cols > 0
    && isGrid(puzzle.grid, puzzle.rows, puzzle.cols)
    && Array.isArray(puzzle.words)
    && puzzle.words.every((word) => typeof word.id === 'string'
      && typeof word.word === 'string'
      && Array.isArray(word.positions)
      && word.positions.every(isCell))
    && Boolean(puzzle.hiddenWord)
    && typeof puzzle.hiddenWord.word === 'string'
    && Array.isArray(puzzle.hiddenWord.positions)
    && puzzle.hiddenWord.positions.every(isCell)
    && Array.isArray(session.foundWordIds)
    && session.foundWordIds.every((wordId) => typeof wordId === 'string')
    && (session.hiddenWordMode === undefined || typeof session.hiddenWordMode === 'boolean')
    && (session.hiddenWordSolved === undefined || typeof session.hiddenWordSolved === 'boolean')
    && (session.hiddenWordProgress === undefined
      || (Array.isArray(session.hiddenWordProgress) && session.hiddenWordProgress.every(isCell)))
    && (session.tempSelection === null
      || session.tempSelection === undefined
      || (Boolean(session.tempSelection)
        && isCell(session.tempSelection.start)
        && isCell(session.tempSelection.end)
        && Array.isArray(session.tempSelection.path)
        && session.tempSelection.path.every(isCell)));
}

function isWordSearchAnalysisSource(source: PuzzleAnalysisSource | undefined): source is WordSearchAnalysisSource {
  return source?.gameId === 'wordsearch' && isWordSearchSession(source.payload);
}

function buildWordSearchAnalysisInternal(source: PuzzleAnalysisSource): PuzzleAnalysisPayload | null {
  if (!isWordSearchAnalysisSource(source)) {
    return null;
  }

  const hint = getWordSearchNextMoveHint(source.payload);
  if (!hint) {
    return null;
  }

  const afterFound = source.payload.puzzle.words
    .find((word) => !source.payload.foundWordIds.includes(word.id));

  return {
    gameId: 'wordsearch',
    steps: [{
      key: 'step-1',
      title: hint.title,
      body: hint.body,
      ruleKey: hint.ruleKey,
      evidenceCells: hint.evidenceCells,
      targetCells: hint.targetCells,
      highlightRows: Array.from(new Set(hint.targetCells.map((cell) => cell.row))).sort((a, b) => a - b),
      highlightCols: Array.from(new Set(hint.targetCells.map((cell) => cell.col))).sort((a, b) => a - b),
      beforeState: {
        foundWordIds: [...source.payload.foundWordIds],
      },
      afterState: {
        foundWordIds: afterFound
          ? [...source.payload.foundWordIds, afterFound.id]
          : [...source.payload.foundWordIds],
      },
    }],
    payload: {
      puzzle: cloneWordSearchPuzzle(source.payload.puzzle),
    },
  };
}

export function buildWordSearchAnalysisSource(session: unknown): PuzzleAnalysisSource | null {
  if (!isWordSearchSession(session)) {
    return null;
  }

  return {
    gameId: 'wordsearch',
    payload: {
      puzzle: cloneWordSearchPuzzle(session.puzzle),
      foundWordIds: [...session.foundWordIds],
      tempSelection: cloneWordSearchSelection(session.tempSelection),
      hiddenWordMode: session.hiddenWordMode ?? false,
      hiddenWordProgress: session.hiddenWordProgress?.map((cell) => ({ ...cell })) ?? [],
      hiddenWordSolved: session.hiddenWordSolved ?? false,
    },
  };
}

export function supportsWordSearchAnalysis(source: PuzzleAnalysisSource | undefined): boolean {
  return isWordSearchAnalysisSource(source)
    && buildWordSearchAnalysisInternal(source) !== null;
}

export function buildWordSearchAnalysis(source: PuzzleAnalysisSource): PuzzleAnalysisPayload {
  const analysis = buildWordSearchAnalysisInternal(source);
  if (!analysis) {
    throw new Error('Word Search analysis is unavailable for this puzzle state.');
  }

  return analysis;
}
