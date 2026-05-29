import { sudokuDigits } from '../../../types';
import { allHouseRefs, buildCandidateEliminationMove, getCellCandidatesByIndex, getHouseDigitMatches } from '../techniqueHelpers';
import type { SudokuTechniqueDispatcher } from '../techniqueModuleTypes';

export const hiddenPairTechnique: SudokuTechniqueDispatcher = {
  technique: 'hidden-pair',
  tier: 'medium',
  findMove(state) {
    for (const { house, cells } of allHouseRefs) {
      const digitPositions = new Map<number, number[]>();
      sudokuDigits.forEach((digit) => {
        digitPositions.set(digit, getHouseDigitMatches(state, cells, digit));
      });

      for (let leftIndex = 0; leftIndex < sudokuDigits.length; leftIndex += 1) {
        for (let rightIndex = leftIndex + 1; rightIndex < sudokuDigits.length; rightIndex += 1) {
          const leftDigit = sudokuDigits[leftIndex];
          const rightDigit = sudokuDigits[rightIndex];
          const leftPositions = digitPositions.get(leftDigit) ?? [];
          const rightPositions = digitPositions.get(rightDigit) ?? [];
          if (leftPositions.length !== 2 || rightPositions.length !== 2) {
            continue;
          }

          if (leftPositions[0] !== rightPositions[0] || leftPositions[1] !== rightPositions[1]) {
            continue;
          }

          const move = buildCandidateEliminationMove({
            technique: 'hidden-pair',
            eliminations: leftPositions.flatMap((index) => getCellCandidatesByIndex(state, index)
              .filter((digit) => digit !== leftDigit && digit !== rightDigit)
              .map((digit) => ({ index, digit }))),
            evidenceCells: leftPositions,
            houses: [house],
          });
          if (move) {
            return move;
          }
        }
      }
    }

    return null;
  },
};
