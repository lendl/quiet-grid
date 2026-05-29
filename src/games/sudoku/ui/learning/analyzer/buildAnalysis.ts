import type { PuzzleAnalysisPayload, PuzzleAnalysisSource } from '../../../../../app/analysis/types';
import { traceSudokuHumanSolve } from '../../../gameplay/analysis/dispatcher';
import { buildSudokuNextMoveHintFromMove } from '../../../gameplay/analysis/nextMove';
import type { SudokuPlaySession } from '../../../gameplay/playContract';
import {
  cloneSudokuBoard,
  type SudokuBoard,
  type SudokuDigit,
} from '../../../types';
import type {
  SudokuAnalysisPayload,
  SudokuAnalysisSource,
  SudokuAnalysisState,
  SudokuAnalysisStep,
} from './types';
import { getSudokuStrings } from '../../../content/strings';

function isSudokuBoard(value: unknown): value is SudokuBoard {
  return Array.isArray(value)
    && value.length === 9
    && value.every((row) => Array.isArray(row) && row.length === 9);
}

function isSudokuPlaySession(value: unknown): value is SudokuPlaySession {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const candidate = value as Partial<SudokuPlaySession>;
  return Boolean(candidate.puzzle)
    && typeof candidate.puzzle?.rows === 'number'
    && typeof candidate.puzzle?.cols === 'number'
    && isSudokuBoard(candidate.puzzle?.givens)
    && isSudokuBoard(candidate.puzzle?.solution)
    && isSudokuBoard(candidate.board);
}

function isSudokuAnalysisSource(source: PuzzleAnalysisSource | undefined): source is SudokuAnalysisSource {
  if (!source || source.gameId !== 'sudoku') {
    return false;
  }

  const payload = source.payload as Partial<SudokuAnalysisSource['payload']> | undefined;
  return Boolean(payload?.puzzle)
    && isSudokuBoard(payload?.puzzle?.givens)
    && isSudokuBoard(payload?.puzzle?.solution)
    && isSudokuBoard(payload?.board);
}

function cloneAnalysisState(state: SudokuAnalysisState): SudokuAnalysisState {
  return {
    board: cloneSudokuBoard(state.board),
  };
}

function applyPlacement(
  board: SudokuBoard,
  target: { row: number; col: number; digit: SudokuDigit },
): void {
  board[target.row][target.col] = target.digit;
}

function buildUnsupportedStep(
  board: SudokuBoard,
  stepNumber: number,
): SudokuAnalysisStep {
  const strings = getSudokuStrings();
  const state = cloneAnalysisState({ board });

  return {
    key: `step-${stepNumber}`,
    title: strings.play.nextMove.unsupportedTitle,
    body: strings.play.nextMove.unsupportedBody,
    evidenceCells: [],
    targetCells: [],
    highlightRows: [],
    highlightCols: [],
    highlightBoxes: [],
    beforeState: state,
    afterState: cloneAnalysisState(state),
    placementTargets: [],
    eliminationTargets: [],
  };
}

function buildSudokuAnalysisInternal(source: SudokuAnalysisSource): SudokuAnalysisPayload | null {
  const strings = getSudokuStrings();
  const { puzzle } = source.payload;
  const board = cloneSudokuBoard(puzzle.givens);
  const steps: SudokuAnalysisStep[] = [];
  const trace = traceSudokuHumanSolve(board);

  trace.moves
    .filter((move) => move.kind === 'placement')
    .forEach((move) => {
    const hint = buildSudokuNextMoveHintFromMove(move);
    const beforeState = cloneAnalysisState({ board });

    applyPlacement(board, hint.targetCells[0]);

    const afterState = cloneAnalysisState({ board });
    const placementTargets = hint.targetCells.filter((cell) => cell.action === 'place');
    const eliminationTargets = hint.targetCells.filter((cell) => cell.action === 'eliminate');

    steps.push({
      key: `step-${steps.length + 1}`,
      title: hint.title,
      body: hint.body,
      ruleKey: hint.ruleKey,
      evidenceCells: hint.evidenceCells,
      targetCells: hint.targetCells.map(({ row, col }) => ({ row, col })),
      highlightRows: hint.highlightRows,
      highlightCols: hint.highlightCols,
      beforeState,
      afterState,
      placementTargets,
      eliminationTargets,
      highlightBoxes: hint.highlightBoxes,
    });
    });

  if (!trace.solved) {
    steps.push(buildUnsupportedStep(board, steps.length + 1));
  }

  if (steps.length === 0) {
    return null;
  }

  return {
    gameId: 'sudoku',
    steps,
    payload: {
      givens: cloneSudokuBoard(source.payload.puzzle.givens),
      labels: {
        evidence: strings.learning.analyzer.legend.evidence,
        place: strings.learning.analyzer.legend.place,
        eliminate: strings.learning.analyzer.legend.eliminate,
      },
    },
  };
}

export function buildSudokuAnalysisSource(session: unknown): PuzzleAnalysisSource | null {
  if (!isSudokuPlaySession(session)) {
    return null;
  }

  return {
    gameId: 'sudoku',
    payload: {
      puzzle: {
        ...session.puzzle,
        givens: cloneSudokuBoard(session.puzzle.givens),
        solution: cloneSudokuBoard(session.puzzle.solution),
      },
      board: cloneSudokuBoard(session.puzzle.givens),
    },
  };
}

export function supportsSudokuAnalysis(source: PuzzleAnalysisSource | undefined): boolean {
  return isSudokuAnalysisSource(source);
}

export function buildSudokuAnalysis(source: PuzzleAnalysisSource): PuzzleAnalysisPayload {
  if (!isSudokuAnalysisSource(source)) {
    throw new Error('Sudoku analysis source is invalid.');
  }

  const analysis = buildSudokuAnalysisInternal(source);
  if (!analysis) {
    throw new Error('Sudoku analysis is unavailable for this puzzle state.');
  }

  return analysis;
}
