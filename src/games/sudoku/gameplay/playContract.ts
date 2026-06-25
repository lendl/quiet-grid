import { formatElapsed } from '../../../app/utils/formatElapsed';
import { computeAccuracyPct, computeFinalScore } from '../../../app/utils/scoring';
import { makeEmptyBooleanGrid } from '../../../app/utils/activeSessionStateStorage';

const TIME_TO_ZERO_SECONDS = { easy: 600, medium: 900, hard: 1200, expert: 1800 };
import type { PuzzlePlayContract } from '../../../app/shell/playContract';
import type { SessionResult } from '../../../app/shell/types';
import { pickRandomPuzzleForDifficulty } from '../../shared/randomPuzzleSelection';
import sudokuPuzzles from '../puzzles/all';
import {
  cloneSudokuBoard,
  cloneSudokuBooleanGrid,
  cloneSudokuNotes,
  cloneSudokuSolution,
  cloneSudokuUnitKeys,
  countFilledSudokuCells,
  createEmptySudokuNotes,
  hasSudokuNotes,
  isSudokuSolved,
  type SudokuActiveSession,
  type SudokuSession,
} from '../types';

export type SudokuPlaySession = SudokuSession;

export interface SudokuHudState {
  elapsedLabel: string;
}

export type { SudokuAction, SudokuActionResult, SudokuValidationEffect } from './actions';

function createSudokuSession(difficulty: SudokuActiveSession['puzzle']['difficulty']): SudokuPlaySession | null {
  const entry = pickRandomPuzzleForDifficulty('sudoku', sudokuPuzzles, difficulty);
  if (!entry) {
    return null;
  }

  return {
    puzzle: {
      id: entry.id,
      difficulty: entry.difficulty,
      rows: entry.rows,
      cols: entry.cols,
      givens: cloneSudokuBoard(entry.givens),
      solution: cloneSudokuSolution(entry.solution),
    },
    board: cloneSudokuBoard(entry.givens),
    notes: createEmptySudokuNotes(entry.rows, entry.cols),
    inputMode: 'digit',
    selectedNoteDigit: null,
    accuracyDrops: 0,
    finishedCells: makeEmptyBooleanGrid(entry.rows),
    validatedUnitKeys: [],
    penalizedUnitKeys: [],
  };
}

function hasSudokuProgress(session: SudokuPlaySession): boolean {
  return session.board.some((row, rowIndex) => (
    row.some((cell, colIndex) => cell !== session.puzzle.givens[rowIndex]?.[colIndex])
  ))
    || hasSudokuNotes(session.notes)
    || session.finishedCells.some((row) => row.some(Boolean))
    || session.accuracyDrops > 0
    || session.validatedUnitKeys.length > 0
    || session.penalizedUnitKeys.length > 0;
}

export function buildSudokuResult(
  session: SudokuPlaySession,
  elapsedSeconds = 0,
): SessionResult {
  const solved = isSudokuSolved(session.board, session.puzzle.solution);

  return {
    gameId: 'sudoku',
    difficulty: session.puzzle.difficulty,
    status: solved ? 'solved' : 'failed',
    score: solved ? computeFinalScore(session.puzzle.difficulty, elapsedSeconds, session.accuracyDrops, TIME_TO_ZERO_SECONDS) : 0,
    accuracy: computeAccuracyPct(session.accuracyDrops),
    elapsedSeconds,
    streak: 0,
  };
}

export const sudokuPlayContract: PuzzlePlayContract<
  SudokuPlaySession,
  SudokuActiveSession,
  SudokuHudState
> = {
  createSession: ({ difficulty }) => createSudokuSession(difficulty),
  canResume: (activeSession): activeSession is SudokuActiveSession => activeSession?.gameId === 'sudoku',
  restoreSession: (activeSession) => ({
    session: {
      puzzle: {
        ...activeSession.puzzle,
        givens: cloneSudokuBoard(activeSession.puzzle.givens),
        solution: cloneSudokuSolution(activeSession.puzzle.solution),
      },
      board: cloneSudokuBoard(activeSession.board),
      notes: cloneSudokuNotes(activeSession.notes),
      inputMode: activeSession.inputMode,
      selectedNoteDigit: activeSession.selectedNoteDigit,
      accuracyDrops: activeSession.accuracyDrops,
      finishedCells: cloneSudokuBooleanGrid(activeSession.finishedCells),
      validatedUnitKeys: cloneSudokuUnitKeys(activeSession.validatedUnitKeys),
      penalizedUnitKeys: cloneSudokuUnitKeys(activeSession.penalizedUnitKeys),
    },
    elapsedSeconds: activeSession.elapsedSeconds,
  }),
  serializeSession: ({ session, elapsedSeconds }) => ({
    gameId: 'sudoku',
    puzzle: {
      ...session.puzzle,
      givens: cloneSudokuBoard(session.puzzle.givens),
      solution: cloneSudokuSolution(session.puzzle.solution),
    },
    board: cloneSudokuBoard(session.board),
    notes: cloneSudokuNotes(session.notes),
    inputMode: session.inputMode,
    selectedNoteDigit: session.selectedNoteDigit,
    elapsedSeconds,
    accuracyDrops: session.accuracyDrops,
    finishedCells: cloneSudokuBooleanGrid(session.finishedCells),
    validatedUnitKeys: cloneSudokuUnitKeys(session.validatedUnitKeys),
    penalizedUnitKeys: cloneSudokuUnitKeys(session.penalizedUnitKeys),
  }),
  getPersistenceKey: ({ session, elapsedBucket }) => JSON.stringify({
    puzzleId: session.puzzle.id,
    board: session.board,
    notes: session.notes,
    inputMode: session.inputMode,
    selectedNoteDigit: session.selectedNoteDigit,
    accuracyDrops: session.accuracyDrops,
    finishedCells: session.finishedCells,
    validatedUnitKeys: session.validatedUnitKeys,
    penalizedUnitKeys: session.penalizedUnitKeys,
    elapsedBucket,
  }),
  getHudState: ({ elapsedSeconds }) => ({
    elapsedLabel: formatElapsed(elapsedSeconds),
  }),
  getSolvedState: ({ session, elapsedSeconds }) => (isSudokuSolved(session.board, session.puzzle.solution)
    ? {
        gameId: 'sudoku',
        difficulty: session.puzzle.difficulty,
        status: 'solved',
        score: computeFinalScore(session.puzzle.difficulty, elapsedSeconds, session.accuracyDrops, TIME_TO_ZERO_SECONDS),
        accuracy: computeAccuracyPct(session.accuracyDrops),
        elapsedSeconds,
      }
    : null),
  isInProgress: (session) => !isSudokuSolved(session.board, session.puzzle.solution),
  hasMeaningfulProgress: hasSudokuProgress,
};

export function getSudokuBoardFillSummary(session: SudokuPlaySession): string {
  return `${countFilledSudokuCells(session.board)} / ${session.puzzle.rows * session.puzzle.cols}`;
}
