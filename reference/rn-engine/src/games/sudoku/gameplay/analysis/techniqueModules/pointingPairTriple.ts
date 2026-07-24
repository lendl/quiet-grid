import { sudokuDigits } from '../../../types';
import { boxCellIndexes, cellBoxIndexes, cellColIndexes, cellRowIndexes } from '../bitmask';
import { buildCandidateEliminationMove, getHouseDigitMatches } from '../techniqueHelpers';
import type { SudokuTechniqueDispatcher } from '../techniqueModuleTypes';
import type { SudokuCanonicalMove } from '../moves';

export const pointingPairTripleTechnique: SudokuTechniqueDispatcher = {
  technique: 'pointing-pair-triple',
  tier: 'medium',
  findMove(state) {
    let best: SudokuCanonicalMove | null = null;
    let bestComplexity = Infinity;

    for (let boxIndex = 0; boxIndex < boxCellIndexes.length; boxIndex += 1) {
      const boxCells = boxCellIndexes[boxIndex];
      const complexity = boxCells.filter((i) => state.board[i] === 0).length;
      if (complexity >= bestComplexity) {
        continue;
      }

      for (const digit of sudokuDigits) {
        const matches = getHouseDigitMatches(state, boxCells, digit);
        if (matches.length < 2 || matches.length > 3) {
          continue;
        }

        const distinctRows = Array.from(new Set(matches.map((index) => cellRowIndexes[index])));
        if (distinctRows.length === 1) {
          const targetRow = distinctRows[0];
          const move = buildCandidateEliminationMove({
            technique: 'pointing-pair-triple',
            eliminations: Array.from({ length: 9 }, (_, col) => (targetRow * 9) + col)
              .filter((index) => cellBoxIndexes[index] !== boxIndex)
              .filter((index) => state.board[index] === 0 && (state.candidateMask[index] & (1 << (digit - 1))) !== 0)
              .map((index) => ({ index, digit })),
            evidenceCells: matches,
            houses: [
              { kind: 'box', index: boxIndex },
              { kind: 'row', index: targetRow },
            ],
            complexity,
          });
          if (move) {
            best = move;
            bestComplexity = complexity;
            break;
          }
        }

        const distinctCols = Array.from(new Set(matches.map((index) => cellColIndexes[index])));
        if (distinctCols.length === 1) {
          const targetCol = distinctCols[0];
          const move = buildCandidateEliminationMove({
            technique: 'pointing-pair-triple',
            eliminations: Array.from({ length: 9 }, (_, row) => (row * 9) + targetCol)
              .filter((index) => cellBoxIndexes[index] !== boxIndex)
              .filter((index) => state.board[index] === 0 && (state.candidateMask[index] & (1 << (digit - 1))) !== 0)
              .map((index) => ({ index, digit })),
            evidenceCells: matches,
            houses: [
              { kind: 'box', index: boxIndex },
              { kind: 'column', index: targetCol },
            ],
            complexity,
          });
          if (move) {
            best = move;
            bestComplexity = complexity;
            break;
          }
        }
      }
    }

    return best;
  },
};
