import { sudokuDigits } from '../../../types';
import { allHouseRefs, buildCandidateEliminationMove, getCellCandidatesByIndex, getHouseDigitMatches } from '../techniqueHelpers';
import type { SudokuTechniqueDispatcher } from '../techniqueModuleTypes';
import type { SudokuCanonicalMove } from '../moves';

export const hiddenPairTechnique: SudokuTechniqueDispatcher = {
  technique: 'hidden-pair',
  tier: 'medium',
  findMove(state) {
    let best: SudokuCanonicalMove | null = null;
    let bestComplexity = Infinity;

    for (const { house, cells } of allHouseRefs) {
      const complexity = cells.filter((i) => state.board[i] === 0).length * 1.5;
      if (complexity >= bestComplexity) {
        continue;
      }

      const digitPositions = new Map<number, number[]>();
      sudokuDigits.forEach((digit) => {
        digitPositions.set(digit, getHouseDigitMatches(state, cells, digit));
      });

      let found = false;
      for (let leftIndex = 0; leftIndex < sudokuDigits.length && !found; leftIndex += 1) {
        for (let rightIndex = leftIndex + 1; rightIndex < sudokuDigits.length && !found; rightIndex += 1) {
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
            complexity,
          });
          if (move) {
            best = move;
            bestComplexity = complexity;
            found = true;
          }
        }
      }
    }

    return best;
  },
};
