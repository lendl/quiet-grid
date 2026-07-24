import { bitToDigit, boxCellIndexes, cellBoxIndexes, columnCellIndexes, popcount, rowCellIndexes } from '../bitmask';
import { buildPlacementMove } from '../techniqueHelpers';
import type { SudokuTechniqueDispatcher } from '../techniqueModuleTypes';
import type { SudokuDigit } from '../../../types';

function computeComplexity(board: Int32Array | number[], index: number): number {
  const row = Math.floor(index / 9);
  const col = index % 9;
  const box = cellBoxIndexes[index];
  const rowEmpty = rowCellIndexes[row].filter((i) => board[i] === 0 && i !== index).length;
  const colEmpty = columnCellIndexes[col].filter((i) => board[i] === 0 && i !== index).length;
  const boxEmpty = boxCellIndexes[box].filter((i) => board[i] === 0 && i !== index).length;
  return Math.min(rowEmpty, colEmpty, boxEmpty);
}

export const nakedSingleTechnique: SudokuTechniqueDispatcher = {
  technique: 'naked-single',
  tier: 'easy',
  findMove(state) {
    let best = null as ReturnType<typeof buildPlacementMove> | null;
    let bestComplexity = Infinity;

    for (let index = 0; index < state.board.length; index += 1) {
      if (state.board[index] !== 0 || popcount[state.candidateMask[index]] !== 1) {
        continue;
      }
      const complexity = computeComplexity(state.board, index);
      if (complexity >= bestComplexity) {
        continue;
      }
      bestComplexity = complexity;
      best = buildPlacementMove({
        technique: 'naked-single',
        row: Math.floor(index / 9),
        col: index % 9,
        digit: bitToDigit[state.candidateMask[index]] as SudokuDigit,
        evidenceCells: [index],
        complexity,
      });
    }

    return best;
  },
};
