import { sudokuDigits } from '../../../types';
import { allHouseRefs, buildPlacementMove, getHouseDigitMatches } from '../techniqueHelpers';
import type { SudokuTechniqueDispatcher } from '../techniqueModuleTypes';

export const hiddenSingleTechnique: SudokuTechniqueDispatcher = {
  technique: 'hidden-single',
  tier: 'easy',
  findMove(state) {
    for (const { house, cells } of allHouseRefs) {
      for (const digit of sudokuDigits) {
        const matches = getHouseDigitMatches(state, cells, digit);
        if (matches.length !== 1) {
          continue;
        }

        return buildPlacementMove({
          technique: 'hidden-single',
          row: Math.floor(matches[0] / 9),
          col: matches[0] % 9,
          digit,
          evidenceCells: cells,
          houses: [
            house,
            ...[
              { kind: 'row' as const, index: Math.floor(matches[0] / 9) },
              { kind: 'column' as const, index: matches[0] % 9 },
              { kind: 'box' as const, index: Math.floor(Math.floor(matches[0] / 9) / 3) * 3 + Math.floor((matches[0] % 9) / 3) },
            ].filter((ref) => !(ref.kind === house.kind && ref.index === house.index)),
          ],
        });
      }
    }

    return null;
  },
};
