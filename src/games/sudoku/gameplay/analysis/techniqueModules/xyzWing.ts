import { popcount } from '../bitmask';
import { arePeerIndexes, buildCandidateEliminationMove, getCellCandidatesByIndex, peerIntersectionIndexes } from '../techniqueHelpers';
import type { SudokuTechniqueDispatcher } from '../techniqueModuleTypes';

export const xyzWingTechnique: SudokuTechniqueDispatcher = {
  technique: 'xyz-wing',
  tier: 'expert',
  findMove(state) {
    const pivotCells = state.board
      .map((value, index) => ({ value, index }))
      .filter(({ value, index }) => value === 0 && popcount[state.candidateMask[index]] === 3)
      .map(({ index }) => ({ index, candidates: getCellCandidatesByIndex(state, index) }));
    const wings = state.board
      .map((value, index) => ({ value, index }))
      .filter(({ value, index }) => value === 0 && popcount[state.candidateMask[index]] === 2)
      .map(({ index }) => ({ index, candidates: getCellCandidatesByIndex(state, index) }));

    const complexity = pivotCells.length;

    for (const pivot of pivotCells) {
      const pivotPeers = wings.filter((wing) => arePeerIndexes(wing.index, pivot.index));
      for (let leftIndex = 0; leftIndex < pivotPeers.length; leftIndex += 1) {
        for (let rightIndex = leftIndex + 1; rightIndex < pivotPeers.length; rightIndex += 1) {
          const leftWing = pivotPeers[leftIndex];
          const rightWing = pivotPeers[rightIndex];
          const leftSubset = leftWing.candidates.every((digit) => pivot.candidates.includes(digit));
          const rightSubset = rightWing.candidates.every((digit) => pivot.candidates.includes(digit));
          if (!leftSubset || !rightSubset) {
            continue;
          }

          const wingUnion = Array.from(new Set([...leftWing.candidates, ...rightWing.candidates])).sort((a, b) => a - b);
          if (wingUnion.length !== 3 || wingUnion.some((digit) => !pivot.candidates.includes(digit))) {
            continue;
          }

          const commonDigits = leftWing.candidates.filter((digit) => rightWing.candidates.includes(digit));
          if (commonDigits.length !== 1) {
            continue;
          }

          const zDigit = commonDigits[0];
          const move = buildCandidateEliminationMove({
            technique: 'xyz-wing',
            eliminations: peerIntersectionIndexes([pivot.index, leftWing.index, rightWing.index])
              .filter((index) => ![pivot.index, leftWing.index, rightWing.index].includes(index))
              .filter((index) => getCellCandidatesByIndex(state, index).includes(zDigit))
              .map((index) => ({ index, digit: zDigit })),
            evidenceCells: [pivot.index, leftWing.index, rightWing.index],
            complexity,
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
