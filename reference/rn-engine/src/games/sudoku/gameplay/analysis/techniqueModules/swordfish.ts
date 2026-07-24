import { sudokuDigits } from '../../../types';
import { columnCellIndexes, rowCellIndexes } from '../bitmask';
import { buildCandidateEliminationMove, getHouseDigitMatches } from '../techniqueHelpers';
import type { SudokuTechniqueDispatcher } from '../techniqueModuleTypes';
import type { SudokuCanonicalMove } from '../moves';

export const swordfishTechnique: SudokuTechniqueDispatcher = {
  technique: 'swordfish',
  tier: 'hard',
  findMove(state) {
    let best: SudokuCanonicalMove | null = null;
    let bestComplexity = Infinity;

    for (const digit of sudokuDigits) {
      const rowCandidates = rowCellIndexes.map((cells, row) => ({
        row,
        cols: getHouseDigitMatches(state, cells, digit).map((index) => index % 9),
      })).filter(({ cols }) => cols.length >= 2 && cols.length <= 3);

      const rowComplexity = rowCandidates.length;
      if (rowComplexity < bestComplexity) {
        outer: for (let first = 0; first < rowCandidates.length; first += 1) {
          for (let second = first + 1; second < rowCandidates.length; second += 1) {
            for (let third = second + 1; third < rowCandidates.length; third += 1) {
              const fish = [rowCandidates[first], rowCandidates[second], rowCandidates[third]];
              const cols = Array.from(new Set(fish.flatMap((entry) => entry.cols))).sort((a, b) => a - b);
              if (cols.length !== 3) {
                continue;
              }

              const move = buildCandidateEliminationMove({
                technique: 'swordfish',
                eliminations: cols.flatMap((col) => columnCellIndexes[col]
                  .filter((index) => !fish.some((entry) => Math.floor(index / 9) === entry.row))
                  .filter((index) => state.board[index] === 0 && (state.candidateMask[index] & (1 << (digit - 1))) !== 0)
                  .map((index) => ({ index, digit }))),
                evidenceCells: fish.flatMap((entry) => entry.cols.map((col) => (entry.row * 9) + col)),
                houses: [
                  ...fish.map((entry) => ({ kind: 'row' as const, index: entry.row })),
                  ...cols.map((col) => ({ kind: 'column' as const, index: col })),
                ],
                complexity: rowComplexity,
              });
              if (move) {
                best = move;
                bestComplexity = rowComplexity;
                break outer;
              }
            }
          }
        }
      }

      const columnCandidates = columnCellIndexes.map((cells, col) => ({
        col,
        rows: getHouseDigitMatches(state, cells, digit).map((index) => Math.floor(index / 9)),
      })).filter(({ rows }) => rows.length >= 2 && rows.length <= 3);

      const colComplexity = columnCandidates.length;
      if (colComplexity < bestComplexity) {
        outer: for (let first = 0; first < columnCandidates.length; first += 1) {
          for (let second = first + 1; second < columnCandidates.length; second += 1) {
            for (let third = second + 1; third < columnCandidates.length; third += 1) {
              const fish = [columnCandidates[first], columnCandidates[second], columnCandidates[third]];
              const rows = Array.from(new Set(fish.flatMap((entry) => entry.rows))).sort((a, b) => a - b);
              if (rows.length !== 3) {
                continue;
              }

              const move = buildCandidateEliminationMove({
                technique: 'swordfish',
                eliminations: rows.flatMap((row) => rowCellIndexes[row]
                  .filter((index) => !fish.some((entry) => (index % 9) === entry.col))
                  .filter((index) => state.board[index] === 0 && (state.candidateMask[index] & (1 << (digit - 1))) !== 0)
                  .map((index) => ({ index, digit }))),
                evidenceCells: fish.flatMap((entry) => entry.rows.map((row) => (row * 9) + entry.col)),
                houses: [
                  ...fish.map((entry) => ({ kind: 'column' as const, index: entry.col })),
                  ...rows.map((row) => ({ kind: 'row' as const, index: row })),
                ],
                complexity: colComplexity,
              });
              if (move) {
                best = move;
                bestComplexity = colComplexity;
                break outer;
              }
            }
          }
        }
      }
    }

    return best;
  },
};
