import { popcount } from '../bitmask';
import { arePeerCells, buildCandidateEliminationMove, buildPlacementHouseRefs, getCellCandidatesByIndex, isSameCell } from '../techniqueHelpers';
import type { SudokuTechniqueDispatcher } from '../techniqueModuleTypes';

export const xyWingTechnique: SudokuTechniqueDispatcher = {
  technique: 'xy-wing',
  tier: 'hard',
  findMove(state) {
    const bivalueCells = state.board
      .map((value, index) => ({ value, index }))
      .filter(({ value, index }) => value === 0 && popcount[state.candidateMask[index]] === 2)
      .map(({ index }) => ({
        row: Math.floor(index / 9),
        col: index % 9,
        index,
        candidates: getCellCandidatesByIndex(state, index),
      }));

    for (const pivot of bivalueCells) {
      const wings = bivalueCells.filter((cell) => !isSameCell(cell, pivot) && arePeerCells(cell, pivot));

      for (const leftWing of wings) {
        for (const rightWing of wings) {
          if (isSameCell(leftWing, rightWing)) {
            continue;
          }

          const leftSharedDigits = pivot.candidates.filter((digit) => leftWing.candidates.includes(digit));
          const rightSharedDigits = pivot.candidates.filter((digit) => rightWing.candidates.includes(digit));
          if (leftSharedDigits.length !== 1 || rightSharedDigits.length !== 1) {
            continue;
          }
          if (leftSharedDigits[0] === rightSharedDigits[0]) {
            continue;
          }

          const leftExtraDigits = leftWing.candidates.filter((digit) => !pivot.candidates.includes(digit));
          const rightExtraDigits = rightWing.candidates.filter((digit) => !pivot.candidates.includes(digit));
          if (leftExtraDigits.length !== 1 || rightExtraDigits.length !== 1) {
            continue;
          }

          const zDigit = leftExtraDigits[0];
          if (zDigit !== rightExtraDigits[0]) {
            continue;
          }

          const move = buildCandidateEliminationMove({
            technique: 'xy-wing',
            eliminations: state.board
              .map((value, index) => ({ value, index }))
              .filter(({ value }) => value === 0)
              .filter(({ index }) => ![pivot.index, leftWing.index, rightWing.index].includes(index))
              .filter(({ index }) => {
                const row = Math.floor(index / 9);
                const col = index % 9;
                return arePeerCells({ row, col }, leftWing) && arePeerCells({ row, col }, rightWing);
              })
              .filter(({ index }) => getCellCandidatesByIndex(state, index).includes(zDigit))
              .map(({ index }) => ({ index, digit: zDigit })),
            evidenceCells: [pivot.index, leftWing.index, rightWing.index],
            houses: buildPlacementHouseRefs(pivot.row, pivot.col),
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
