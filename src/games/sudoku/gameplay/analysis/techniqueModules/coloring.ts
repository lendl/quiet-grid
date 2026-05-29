import { sudokuDigits } from '../../../types';
import { cellPeers } from '../bitmask';
import { buildCandidateEliminationMove, collectHousesFromIndexes, getHouseDigitMatches } from '../techniqueHelpers';
import type { SudokuTechniqueDispatcher } from '../techniqueModuleTypes';

function addEdge(graph: Map<number, Set<number>>, left: number, right: number): void {
  graph.set(left, new Set([...(graph.get(left) ?? []), right]));
  graph.set(right, new Set([...(graph.get(right) ?? []), left]));
}

export const coloringTechnique: SudokuTechniqueDispatcher = {
  technique: 'coloring',
  tier: 'expert',
  findMove(state) {
    const houses = [
      ...Array.from({ length: 9 }, (_, index) => Array.from({ length: 9 }, (_, col) => (index * 9) + col)),
      ...Array.from({ length: 9 }, (_, index) => Array.from({ length: 9 }, (_, row) => (row * 9) + index)),
      ...Array.from({ length: 9 }, (_, box) => {
        const rowStart = Math.floor(box / 3) * 3;
        const colStart = (box % 3) * 3;
        return Array.from({ length: 9 }, (_, offset) => (
          ((rowStart + Math.floor(offset / 3)) * 9) + colStart + (offset % 3)
        ));
      }),
    ];

    for (const digit of sudokuDigits) {
      const graph = new Map<number, Set<number>>();
      houses.forEach((houseCells) => {
        const matches = getHouseDigitMatches(state, houseCells, digit);
        if (matches.length === 2) {
          addEdge(graph, matches[0], matches[1]);
        }
      });

      const seen = new Set<number>();
      for (const start of graph.keys()) {
        if (seen.has(start)) {
          continue;
        }

        const colors = new Map<number, 0 | 1>([[start, 0]]);
        const queue = [start];
        const component: number[] = [];

        while (queue.length > 0) {
          const current = queue.shift();
          if (typeof current === 'undefined' || seen.has(current)) {
            continue;
          }
          seen.add(current);
          component.push(current);

          const currentColor = colors.get(current) ?? 0;
          (graph.get(current) ?? []).forEach((neighbor) => {
            if (!colors.has(neighbor)) {
              colors.set(neighbor, currentColor === 0 ? 1 : 0);
            }
            if (!seen.has(neighbor)) {
              queue.push(neighbor);
            }
          });
        }

        for (const color of [0, 1] as const) {
          const colorCells = component.filter((index) => colors.get(index) === color);
          const hasConflict = colorCells.some((left, leftIndex) => (
            colorCells.slice(leftIndex + 1).some((right) => cellPeers[left].includes(right))
          ));
          if (!hasConflict) {
            continue;
          }

          const move = buildCandidateEliminationMove({
            technique: 'coloring',
            eliminations: colorCells.map((index) => ({ index, digit })),
            evidenceCells: component,
            houses: collectHousesFromIndexes(component),
          });
          if (move) {
            return move;
          }
        }

        const colorZero = component.filter((index) => colors.get(index) === 0);
        const colorOne = component.filter((index) => colors.get(index) === 1);
        const move = buildCandidateEliminationMove({
          technique: 'coloring',
          eliminations: state.board
            .map((value, index) => ({ value, index }))
            .filter(({ value, index }) => value === 0 && !component.includes(index))
            .filter(({ index }) => getHouseDigitMatches(state, [index], digit).length === 1)
            .filter(({ index }) => (
              colorZero.some((colorIndex) => cellPeers[index].includes(colorIndex))
              && colorOne.some((colorIndex) => cellPeers[index].includes(colorIndex))
            ))
            .map(({ index }) => ({ index, digit })),
          evidenceCells: component,
          houses: collectHousesFromIndexes(component),
        });
        if (move) {
          return move;
        }
      }
    }

    return null;
  },
};
