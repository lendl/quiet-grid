import { sudokuDigits } from '../../../types';
import { allHouseRefs, buildPlacementMove, getHouseDigitMatches } from '../techniqueHelpers';
import type { SudokuTechniqueDispatcher } from '../techniqueModuleTypes';

const HOUSE_SCAN_WEIGHTS = { row: 1.2, column: 1.4, box: 1.0 } as const;

export const hiddenSingleTechnique: SudokuTechniqueDispatcher = {
  technique: 'hidden-single',
  tier: 'easy',
  findMove(state) {
    let best = null as ReturnType<typeof buildPlacementMove> | null;
    let bestComplexity = Infinity;

    for (const { house, cells } of allHouseRefs) {
      const emptyCells = cells.filter((i) => state.board[i] === 0).length;
      const houseComplexity = emptyCells * HOUSE_SCAN_WEIGHTS[house.kind];
      if (houseComplexity >= bestComplexity) {
        continue;
      }

      for (const digit of sudokuDigits) {
        const matches = getHouseDigitMatches(state, cells, digit);
        if (matches.length !== 1) {
          continue;
        }

        bestComplexity = houseComplexity;
        const targetIndex = matches[0];
        const row = Math.floor(targetIndex / 9);
        const col = targetIndex % 9;
        best = buildPlacementMove({
          technique: 'hidden-single',
          row,
          col,
          digit,
          evidenceCells: cells,
          houses: [
            house,
            ...[
              { kind: 'row' as const, index: row },
              { kind: 'column' as const, index: col },
              { kind: 'box' as const, index: Math.floor(row / 3) * 3 + Math.floor(col / 3) },
            ].filter((ref) => !(ref.kind === house.kind && ref.index === house.index)),
          ],
          complexity: houseComplexity,
        });
        break; // one match per house is enough — all digits in this house share the same complexity
      }
    }

    return best;
  },
};
