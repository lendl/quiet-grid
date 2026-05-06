import type { TutorialLesson, TutorialLessonKey } from '../types';
import type { HowToPlayCellValue } from './howToPlayTips';
import { getTakuzuTutorialLessonCopies } from '../i18n';

const EXAMPLES: Record<TutorialLessonKey, HowToPlayCellValue[][]> = {
  'find-pairs': [[0, 0, 'a1']],
  'avoid-trios': [[1, 'a0', 1]],
  'complete-lines': [[0, 1, 0, 1, 0, 'a1']],
  'eliminate-filled-lines': [
    [1, 0, 1, 0, 1, 0],
    [1, 0, 'a0', 'a1', 1, 0],
  ],
  'eliminate-impossible-combinations': [[1, 1, 0, null, null, 'a0']],
};

function toGridAndMoves(example: HowToPlayCellValue[][]) {
  const moves: TutorialLesson['moves'] = [];
  const grid = example.map((row, rowIndex) =>
    row.map((cell, colIndex) => {
      if (cell === 'a0') {
        moves.push({ row: rowIndex, col: colIndex, value: 0 });
        return null;
      }

      if (cell === 'a1') {
        moves.push({ row: rowIndex, col: colIndex, value: 1 });
        return null;
      }

      return cell;
    }),
  );

  return { grid, moves };
}

export function getTakuzuTutorialLessons(): TutorialLesson[] {
  const lessonCopies = getTakuzuTutorialLessonCopies();
  return (Object.keys(EXAMPLES) as TutorialLessonKey[]).map((key) => {
    const lesson = lessonCopies[key];
    const example = toGridAndMoves(EXAMPLES[key]);

    return {
      key,
      ...lesson,
      grid: example.grid,
      moves: example.moves,
    };
  });
}
