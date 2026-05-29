import { bitToDigit, popcount } from '../bitmask';
import { buildPlacementMove } from '../techniqueHelpers';
import type { SudokuTechniqueDispatcher } from '../techniqueModuleTypes';
import type { SudokuDigit } from '../../../types';

export const nakedSingleTechnique: SudokuTechniqueDispatcher = {
  technique: 'naked-single',
  tier: 'easy',
  findMove(state) {
    for (let index = 0; index < state.board.length; index += 1) {
      if (state.board[index] !== 0 || popcount[state.candidateMask[index]] !== 1) {
        continue;
      }

      return buildPlacementMove({
        technique: 'naked-single',
        row: Math.floor(index / 9),
        col: index % 9,
        digit: bitToDigit[state.candidateMask[index]] as SudokuDigit,
        evidenceCells: [index],
      });
    }

    return null;
  },
};
