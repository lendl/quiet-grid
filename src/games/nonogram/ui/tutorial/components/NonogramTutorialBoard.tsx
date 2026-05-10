import React from 'react';
import type { NonogramCellState, NonogramTutorialLesson } from '../../../types';
import NonogramBoard from '../../play/components/NonogramBoard';

interface NonogramTutorialBoardProps {
  lesson: NonogramTutorialLesson;
  cells: readonly NonogramCellState[];
}

export default function NonogramTutorialBoard({ lesson, cells }: NonogramTutorialBoardProps) {
  const firstTarget = lesson.targetCells[0];

  return (
    <NonogramBoard
      puzzle={lesson.puzzle}
      cells={cells}
      interactive={false}
      nextMoveTargetCells={lesson.targetCells}
      nextMoveHighlightRows={firstTarget ? [firstTarget.row] : []}
      nextMoveHighlightCols={firstTarget ? [firstTarget.col] : []}
    />
  );
}
