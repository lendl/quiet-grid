import type { SudokuBoard, SudokuSession } from '../../types';
import {
  createSudokuBitmaskStateFromBoard,
  createSudokuBitmaskStateFromSession,
  eliminateSudokuCandidate,
  encodeSudokuBitmaskState,
  getCellIndex,
  isSudokuBitmaskStateSolved,
  placeSudokuDigit,
  type SudokuBitmaskState,
} from './bitmask';
import type { SudokuCanonicalMove } from './moves';
import { boxLineReductionTechnique } from './techniqueModules/boxLineReduction';
import { chainsTechnique } from './techniqueModules/chains';
import { coloringTechnique } from './techniqueModules/coloring';
import { hiddenPairTechnique } from './techniqueModules/hiddenPair';
import { hiddenSingleTechnique } from './techniqueModules/hiddenSingle';
import { nakedPairTechnique } from './techniqueModules/nakedPair';
import { nakedSingleTechnique } from './techniqueModules/nakedSingle';
import { pointingPairTripleTechnique } from './techniqueModules/pointingPairTriple';
import { swordfishTechnique } from './techniqueModules/swordfish';
import { xWingTechnique } from './techniqueModules/xWing';
import { xyWingTechnique } from './techniqueModules/xyWing';
import { xyzWingTechnique } from './techniqueModules/xyzWing';
import type { SudokuTechniqueDispatcher, SudokuTechniqueDispatcherState } from './techniqueModuleTypes';
import { sudokuTechniques, type SudokuTechnique } from './techniques';

export type { SudokuTechniqueDispatcher, SudokuTechniqueDispatcherState } from './techniqueModuleTypes';

export interface SudokuSolveTrace {
  solved: boolean;
  blocked: boolean;
  blockedByUnsupportedTechnique: boolean;
  requiresGuess: boolean;
  moves: SudokuCanonicalMove[];
}

export const orderedSudokuTechniqueDispatchers: readonly SudokuTechniqueDispatcher[] = [
  nakedSingleTechnique,
  hiddenSingleTechnique,
  nakedPairTechnique,
  hiddenPairTechnique,
  pointingPairTripleTechnique,
  boxLineReductionTechnique,
  xWingTechnique,
  swordfishTechnique,
  xyWingTechnique,
  xyzWingTechnique,
  coloringTechnique,
  chainsTechnique,
] as const;

// Base scores match DEFAULT_SUDOKU_DIFFICULTY_WEIGHTS.techniqueWeights in difficulty.ts
const TECHNIQUE_BASE_SCORES: Record<SudokuTechnique, number> = {
  'naked-single': 2,
  'hidden-single': 3,
  'naked-pair': 6,
  'hidden-pair': 9,
  'pointing-pair-triple': 10,
  'box-line-reduction': 10,
  'x-wing': 17,
  'swordfish': 24,
  'xy-wing': 21,
  'xyz-wing': 28,
  coloring: 27,
  chains: 30,
};

function moveScore(move: SudokuCanonicalMove): number {
  return TECHNIQUE_BASE_SCORES[move.technique] + move.complexity;
}

function applySudokuMove(state: SudokuBitmaskState, move: SudokuCanonicalMove): void {
  if (move.kind === 'placement') {
    placeSudokuDigit(state, getCellIndex(move.target.row, move.target.col), move.target.digit);
    return;
  }

  move.eliminations.forEach((elimination) => {
    eliminateSudokuCandidate(state, getCellIndex(elimination.row, elimination.col), elimination.digit);
  });
}

function findNextSudokuMoveInState(
  state: SudokuBitmaskState,
  allowedTechniques: readonly SudokuTechnique[],
): SudokuCanonicalMove | null {
  const allowedTechniqueSet = new Set(allowedTechniques);
  let best: SudokuCanonicalMove | null = null;
  let bestScore = Infinity;

  for (const dispatcher of orderedSudokuTechniqueDispatchers) {
    if (!allowedTechniqueSet.has(dispatcher.technique)) {
      continue;
    }

    // Prune: the minimum possible score for this technique is its base score + 0 complexity.
    // If that already can't beat the current best, skip entirely.
    if (TECHNIQUE_BASE_SCORES[dispatcher.technique] >= bestScore) {
      continue;
    }

    const move = dispatcher.findMove(state);
    if (!move) {
      continue;
    }

    const score = moveScore(move);
    if (score < bestScore) {
      best = move;
      bestScore = score;
    }
  }

  return best;
}

export function traceSudokuHumanSolve(
  board: SudokuBoard,
  allowedTechniques: readonly SudokuTechnique[] = sudokuTechniques,
): SudokuSolveTrace {
  const state = createSudokuBitmaskStateFromBoard(board);
  const moves: SudokuCanonicalMove[] = [];
  const seenStates = new Set<string>();

  while (!isSudokuBitmaskStateSolved(state)) {
    const stateKey = encodeSudokuBitmaskState(state);
    if (seenStates.has(stateKey)) {
      return {
        solved: false,
        blocked: true,
        blockedByUnsupportedTechnique: true,
        requiresGuess: false,
        moves,
      };
    }
    seenStates.add(stateKey);

    const move = findNextSudokuMoveInState(state, allowedTechniques);
    if (!move) {
      return {
        solved: false,
        blocked: true,
        blockedByUnsupportedTechnique: true,
        requiresGuess: false,
        moves,
      };
    }

    applySudokuMove(state, move);
    moves.push(move);
    if (moves.length > 512) {
      return {
        solved: false,
        blocked: true,
        blockedByUnsupportedTechnique: true,
        requiresGuess: false,
        moves,
      };
    }
  }

  return {
    solved: true,
    blocked: false,
    blockedByUnsupportedTechnique: false,
    requiresGuess: false,
    moves,
  };
}

export function findNextSudokuMove(
  session: SudokuSession,
  allowedTechniques: readonly SudokuTechnique[] = sudokuTechniques,
): SudokuCanonicalMove | null {
  const state = createSudokuBitmaskStateFromSession(session);
  return findNextSudokuMoveInState(state, allowedTechniques);
}
