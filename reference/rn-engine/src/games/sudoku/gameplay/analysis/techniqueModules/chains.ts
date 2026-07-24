import type { SudokuDigit } from '../../../types';
import type { SudokuCanonicalMove } from '../moves';
import { popcount } from '../bitmask';
import { arePeerIndexes, buildCandidateEliminationMove, getCellCandidatesByIndex } from '../techniqueHelpers';
import type { SudokuTechniqueDispatcher } from '../techniqueModuleTypes';

const MAX_CHAIN_LENGTH = 6;

export const chainsTechnique: SudokuTechniqueDispatcher = {
  technique: 'chains',
  tier: 'expert',
  findMove(state) {
    const bivalueCells = state.board
      .map((value, index) => ({ value, index }))
      .filter(({ value, index }) => value === 0 && popcount[state.candidateMask[index]] === 2)
      .map(({ index }) => ({ index, candidates: getCellCandidatesByIndex(state, index) }));

    const search = (
      startIndex: number,
      currentIndex: number,
      targetDigit: SudokuDigit,
      sharedDigit: SudokuDigit,
      path: number[],
    ): SudokuCanonicalMove | null => {
      if (path.length >= MAX_CHAIN_LENGTH) {
        return null;
      }

      const nextMoves = bivalueCells
        .filter((cell) => !path.includes(cell.index) && arePeerIndexes(cell.index, currentIndex))
        .filter((cell) => cell.candidates.includes(sharedDigit));

      for (const nextCell of nextMoves) {
        const nextDigit = nextCell.candidates.find((digit) => digit !== sharedDigit);
        if (!nextDigit) {
          continue;
        }

        const chainLength = path.length + 1;

        if (nextCell.candidates.includes(targetDigit) && path.length >= 3) {
          const move = buildCandidateEliminationMove({
            technique: 'chains',
            eliminations: state.board
              .map((value, index) => ({ value, index }))
              .filter(({ value, index }) => value === 0 && ![...path, nextCell.index].includes(index))
              .filter(({ index }) => arePeerIndexes(index, startIndex) && arePeerIndexes(index, nextCell.index))
              .filter(({ index }) => getCellCandidatesByIndex(state, index).includes(targetDigit))
              .map(({ index }) => ({ index, digit: targetDigit })),
            evidenceCells: [...path, nextCell.index],
            complexity: chainLength,
          });
          if (move) {
            return move;
          }
        }

        const nested = search(startIndex, nextCell.index, targetDigit, nextDigit, [...path, nextCell.index]);
        if (nested) {
          return nested;
        }
      }

      return null;
    };

    for (const startCell of bivalueCells) {
      const [firstDigit, secondDigit] = startCell.candidates;
      const firstMove = search(startCell.index, startCell.index, firstDigit, secondDigit, [startCell.index]);
      if (firstMove) {
        return firstMove;
      }

      const secondMove = search(startCell.index, startCell.index, secondDigit, firstDigit, [startCell.index]);
      if (secondMove) {
        return secondMove;
      }
    }

    return null;
  },
};
