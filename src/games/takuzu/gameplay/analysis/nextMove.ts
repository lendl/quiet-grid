import { buildPausedNextMove } from './content';
import { getTakuzuRecoveryHint } from './recovery';
import { getTakuzuProgressHint } from './dispatcher';
import type { Grid, TakuzuNextMoveHint } from '../../types';

function createPausedHint(): TakuzuNextMoveHint {
  const paused = buildPausedNextMove();
  return {
    kind: 'paused',
    title: paused.title,
    body: paused.body,
    evidenceCells: [],
    targetCells: [],
    highlightRows: [],
    highlightCols: [],
  };
}

export function getTakuzuNextMoveHint(board: Grid): TakuzuNextMoveHint {
  return getTakuzuRecoveryHint(board)
    ?? getTakuzuProgressHint(board)
    ?? createPausedHint();
}
