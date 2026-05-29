import { popcount } from '../bitmask';
import { allHouseRefs, buildCandidateEliminationMove, getCellCandidatesByIndex } from '../techniqueHelpers';
import type { SudokuTechniqueDispatcher } from '../techniqueModuleTypes';

export const nakedPairTechnique: SudokuTechniqueDispatcher = {
  technique: 'naked-pair',
  tier: 'easy',
  findMove(state) {
    for (const { house, cells } of allHouseRefs) {
      const pairMap = new Map<number, number[]>();

      cells.forEach((index) => {
        const mask = state.candidateMask[index];
        if (state.board[index] !== 0 || popcount[mask] !== 2) {
          return;
        }

        pairMap.set(mask, [...(pairMap.get(mask) ?? []), index]);
      });

      for (const [mask, pairCells] of pairMap.entries()) {
        if (pairCells.length !== 2) {
          continue;
        }

        const pairDigits = getCellCandidatesByIndex(state, pairCells[0]);
        const move = buildCandidateEliminationMove({
          technique: 'naked-pair',
          eliminations: cells
            .filter((index) => !pairCells.includes(index))
            .flatMap((index) => pairDigits
              .filter((digit) => getCellCandidatesByIndex(state, index).includes(digit))
              .map((digit) => ({ index, digit }))),
          evidenceCells: pairCells,
          houses: [house],
        });
        if (move) {
          return move;
        }
      }
    }

    return null;
  },
};
