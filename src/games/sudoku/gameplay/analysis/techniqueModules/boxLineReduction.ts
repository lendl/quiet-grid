import { sudokuDigits } from '../../../types';
import { boxCellIndexes, cellBoxIndexes, columnCellIndexes, rowCellIndexes } from '../bitmask';
import { buildCandidateEliminationMove, getHouseDigitMatches } from '../techniqueHelpers';
import type { SudokuTechniqueDispatcher } from '../techniqueModuleTypes';
import type { SudokuCanonicalMove } from '../moves';

export const boxLineReductionTechnique: SudokuTechniqueDispatcher = {
  technique: 'box-line-reduction',
  tier: 'medium',
  findMove(state) {
    let best: SudokuCanonicalMove | null = null;
    let bestComplexity = Infinity;

    for (let rowIndex = 0; rowIndex < rowCellIndexes.length; rowIndex += 1) {
      const rowCells = rowCellIndexes[rowIndex];
      const complexity = rowCells.filter((i) => state.board[i] === 0).length;
      if (complexity >= bestComplexity) {
        continue;
      }

      for (const digit of sudokuDigits) {
        const matches = getHouseDigitMatches(state, rowCells, digit);
        if (matches.length < 2 || matches.length > 3) {
          continue;
        }

        const boxIndexes = Array.from(new Set(matches.map((index) => cellBoxIndexes[index])));
        if (boxIndexes.length === 1) {
          const boxIndex = boxIndexes[0];
          const move = buildCandidateEliminationMove({
            technique: 'box-line-reduction',
            eliminations: rowCells.length > 0
              ? boxCellIndexes[boxIndex]
                .filter((index) => Math.floor(index / 9) !== rowIndex)
                .filter((index) => state.board[index] === 0 && (state.candidateMask[index] & (1 << (digit - 1))) !== 0)
                .map((index) => ({ index, digit }))
              : [],
            evidenceCells: matches,
            houses: [
              { kind: 'row', index: rowIndex },
              { kind: 'box', index: boxIndex },
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

    for (let colIndex = 0; colIndex < columnCellIndexes.length; colIndex += 1) {
      const colCells = columnCellIndexes[colIndex];
      const complexity = colCells.filter((i) => state.board[i] === 0).length;
      if (complexity >= bestComplexity) {
        continue;
      }

      for (const digit of sudokuDigits) {
        const matches = getHouseDigitMatches(state, colCells, digit);
        if (matches.length < 2 || matches.length > 3) {
          continue;
        }

        const boxIndexes = Array.from(new Set(matches.map((index) => cellBoxIndexes[index])));
        if (boxIndexes.length === 1) {
          const boxIndex = boxIndexes[0];
          const move = buildCandidateEliminationMove({
            technique: 'box-line-reduction',
            eliminations: boxCellIndexes[boxIndex]
              .filter((index) => (index % 9) !== colIndex)
              .filter((index) => state.board[index] === 0 && (state.candidateMask[index] & (1 << (digit - 1))) !== 0)
              .map((index) => ({ index, digit })),
            evidenceCells: matches,
            houses: [
              { kind: 'column', index: colIndex },
              { kind: 'box', index: boxIndex },
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
