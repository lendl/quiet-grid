import { sudokuDigits } from '../../../types';
import { columnCellIndexes, rowCellIndexes } from '../bitmask';
import { buildCandidateEliminationMove, getHouseDigitMatches } from '../techniqueHelpers';
import type { SudokuTechniqueDispatcher } from '../techniqueModuleTypes';

export const xWingTechnique: SudokuTechniqueDispatcher = {
  technique: 'x-wing',
  tier: 'hard',
  findMove(state) {
    for (const digit of sudokuDigits) {
      const rowCandidates = rowCellIndexes.map((cells, row) => ({
        row,
        cols: getHouseDigitMatches(state, cells, digit).map((index) => index % 9),
      })).filter(({ cols }) => cols.length === 2);

      for (let first = 0; first < rowCandidates.length; first += 1) {
        for (let second = first + 1; second < rowCandidates.length; second += 1) {
          const left = rowCandidates[first];
          const right = rowCandidates[second];
          if (left.cols[0] !== right.cols[0] || left.cols[1] !== right.cols[1]) {
            continue;
          }

          const move = buildCandidateEliminationMove({
            technique: 'x-wing',
            eliminations: left.cols.flatMap((col) => columnCellIndexes[col]
              .filter((index) => Math.floor(index / 9) !== left.row && Math.floor(index / 9) !== right.row)
              .filter((index) => state.board[index] === 0 && (state.candidateMask[index] & (1 << (digit - 1))) !== 0)
              .map((index) => ({ index, digit }))),
            evidenceCells: [
              (left.row * 9) + left.cols[0],
              (left.row * 9) + left.cols[1],
              (right.row * 9) + right.cols[0],
              (right.row * 9) + right.cols[1],
            ],
            houses: [
              { kind: 'row', index: left.row },
              { kind: 'row', index: right.row },
              { kind: 'column', index: left.cols[0] },
              { kind: 'column', index: left.cols[1] },
            ],
          });
          if (move) {
            return move;
          }
        }
      }

      const columnCandidates = columnCellIndexes.map((cells, col) => ({
        col,
        rows: getHouseDigitMatches(state, cells, digit).map((index) => Math.floor(index / 9)),
      })).filter(({ rows }) => rows.length === 2);

      for (let first = 0; first < columnCandidates.length; first += 1) {
        for (let second = first + 1; second < columnCandidates.length; second += 1) {
          const left = columnCandidates[first];
          const right = columnCandidates[second];
          if (left.rows[0] !== right.rows[0] || left.rows[1] !== right.rows[1]) {
            continue;
          }

          const move = buildCandidateEliminationMove({
            technique: 'x-wing',
            eliminations: left.rows.flatMap((row) => rowCellIndexes[row]
              .filter((index) => (index % 9) !== left.col && (index % 9) !== right.col)
              .filter((index) => state.board[index] === 0 && (state.candidateMask[index] & (1 << (digit - 1))) !== 0)
              .map((index) => ({ index, digit }))),
            evidenceCells: [
              (left.rows[0] * 9) + left.col,
              (left.rows[1] * 9) + left.col,
              (right.rows[0] * 9) + right.col,
              (right.rows[1] * 9) + right.col,
            ],
            houses: [
              { kind: 'column', index: left.col },
              { kind: 'column', index: right.col },
              { kind: 'row', index: left.rows[0] },
              { kind: 'row', index: left.rows[1] },
            ],
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
