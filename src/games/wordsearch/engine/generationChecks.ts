import type { WordSearchDirection } from '../types';
import { directionToDelta, toGridKey } from './gridUtils';

export interface WordPlacementLike {
  word: string;
  positions: readonly { row: number; col: number }[];
}

// A word is "dominated" if every one of its cells is also covered by some
// other placed word — it contributes no cell a player couldn't already
// account for via another word, so it shouldn't be listed.
export function hasCoverageViolation(placements: readonly WordPlacementLike[]): boolean {
  const sets = placements.map((p) => new Set(p.positions.map((c) => toGridKey(c))));
  return sets.some((set, index) => (
    [...set].every((key) => sets.some((other, otherIndex) => otherIndex !== index && other.has(key)))
  ));
}

function positionsMatch(
  a: readonly { row: number; col: number }[],
  b: readonly { row: number; col: number }[],
): boolean {
  if (a.length !== b.length) return false;
  const forward = a.every((p, i) => p.row === b[i]!.row && p.col === b[i]!.col);
  if (forward) return true;
  const n = b.length;
  return a.every((p, i) => p.row === b[n - 1 - i]!.row && p.col === b[n - 1 - i]!.col);
}

// Scans the finished grid in all 8 straight-line directions and confirms
// every entry in `words` (target words plus, optionally, the hidden word)
// appears ONLY at its intended position — nowhere else, forward or reversed.
export function hasDuplicateOccurrence(
  grid: readonly string[][],
  words: readonly WordPlacementLike[],
): boolean {
  const rows = grid.length;
  const cols = grid[0]?.length ?? 0;
  const allDirs = Object.keys(directionToDelta) as WordSearchDirection[];

  const intendedByText = new Map<string, Array<readonly { row: number; col: number }[]>>();
  const addIntended = (text: string, positions: readonly { row: number; col: number }[]) => {
    if (!intendedByText.has(text)) intendedByText.set(text, []);
    intendedByText.get(text)!.push(positions);
  };
  for (const entry of words) {
    addIntended(entry.word, entry.positions);
    addIntended(entry.word.split('').reverse().join(''), entry.positions);
  }
  const wordLengths = new Set(words.map((w) => w.word.length));

  for (const dir of allDirs) {
    const delta = directionToDelta[dir];
    for (let row = 0; row < rows; row += 1) {
      for (let col = 0; col < cols; col += 1) {
        const positions: Array<{ row: number; col: number }> = [];
        let sequence = '';
        let r = row;
        let c = col;
        while (r >= 0 && r < rows && c >= 0 && c < cols) {
          const cell = grid[r][c];
          if (cell === '' || cell === '#') break;
          positions.push({ row: r, col: c });
          sequence += cell;
          if (wordLengths.has(sequence.length)) {
            const intendedList = intendedByText.get(sequence);
            if (intendedList !== undefined) {
              const isIntended = intendedList.some((intended) => positionsMatch(positions, intended));
              if (!isIntended) return true;
            }
          }
          r += delta.row;
          c += delta.col;
        }
      }
    }
  }
  return false;
}
