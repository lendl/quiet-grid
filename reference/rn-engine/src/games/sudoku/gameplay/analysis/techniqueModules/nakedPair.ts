import { popcount } from '../bitmask';
import { allHouseRefs, buildCandidateEliminationMove, getCellCandidatesByIndex } from '../techniqueHelpers';
import type { SudokuTechniqueDispatcher } from '../techniqueModuleTypes';
import type { SudokuCanonicalMove } from '../moves';

export const nakedPairTechnique: SudokuTechniqueDispatcher = {
  technique: 'naked-pair',
  tier: 'easy',
  findMove(state) {
    let best: SudokuCanonicalMove | null = null;
    let bestComplexity = Infinity;

    for (const { house, cells } of allHouseRefs) {
      const complexity = cells.filter((i) => state.board[i] === 0).length;
      if (complexity >= bestComplexity) {
        continue;
      }

      const pairMap = new Map<number, number[]>();
      cells.forEach((index) => {
        const mask = state.candidateMask[index];
        if (state.board[index] !== 0 || popcount[mask] !== 2) {
          return;
        }
        pairMap.set(mask, [...(pairMap.get(mask) ?? []), index]);
      });

      for (const [, pairCells] of pairMap.entries()) {
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
          complexity,
        });
        if (move) {
          best = move;
          bestComplexity = complexity;
          break;
        }
      }
    }

    return best;
  },
};
