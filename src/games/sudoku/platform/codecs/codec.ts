import type { PersistedSessionEnvelope } from '../../../../app/shell/types';
import type { PuzzleDifficulty } from '../../../shared/types';
import type { SudokuSession } from '../../gameplay/activePuzzle';
import {
  cloneSudokuBoard,
  cloneSudokuNotes,
  cloneSudokuSolution,
  cloneSudokuUnitKeys,
  type SudokuBoard,
  type SudokuSolution,
} from '../../types';

export interface SudokuCatalogEntry {
  id: string;
  difficulty: PuzzleDifficulty;
  rows: number;
  cols: number;
  givens: SudokuBoard;
  solution: SudokuSolution;
}

export function normalizeSudokuCatalogEntry(entry: SudokuCatalogEntry): SudokuCatalogEntry {
  return {
    ...entry,
    rows: entry.solution.length,
    cols: entry.solution[0]?.length ?? 0,
    givens: cloneSudokuBoard(entry.givens),
    solution: cloneSudokuSolution(entry.solution),
  };
}

export function serializeSudokuSession(
  session: SudokuSession,
): PersistedSessionEnvelope<SudokuSession> {
  return {
    gameId: 'sudoku',
    version: 1,
    payload: {
      ...session,
      puzzle: {
        ...session.puzzle,
        givens: cloneSudokuBoard(session.puzzle.givens),
        solution: cloneSudokuSolution(session.puzzle.solution),
      },
      board: cloneSudokuBoard(session.board),
      notes: cloneSudokuNotes(session.notes),
      validatedUnitKeys: cloneSudokuUnitKeys(session.validatedUnitKeys),
      penalizedUnitKeys: cloneSudokuUnitKeys(session.penalizedUnitKeys),
    },
  };
}

export function isSudokuSessionPayload(value: unknown): value is SudokuSession {
  return Boolean(value && typeof value === 'object' && 'puzzle' in (value as Record<string, unknown>));
}
