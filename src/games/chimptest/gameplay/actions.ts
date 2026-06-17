import type { PuzzleActionResult } from '../../../app/shell/games/playAdapter';
import type { ChimpTestCell, ChimpTestSession } from '../types';
import { generateChimpTestCells } from '../platform/puzzleData';

export type ChimpTestAction = {
  kind: 'tap';
  row: number;
  col: number;
  elapsedSeconds: number;
};

export type ChimpTestEffect = { type: 'wrong-tap' };

export function runChimpTestAction(
  session: ChimpTestSession,
  action: ChimpTestAction,
): PuzzleActionResult<ChimpTestSession, ChimpTestEffect> {
  if (session.status !== 'playing' || session.revealAll) {
    return { changed: false, session, effects: [] };
  }

  // Allow tapping hidden cells — after the first tap all remaining cells lose their numbers
  // but must still be tappable. Cells with number < nextExpected are already correctly tapped.
  const tappedCell = session.cells.find(
    (c) => c.number >= session.nextExpected && c.row === action.row && c.col === action.col,
  );

  if (!tappedCell || tappedCell.number !== session.nextExpected) {
    return {
      changed: true,
      session: { ...session, revealAll: true, wrongTapCell: tappedCell?.number ?? null },
      effects: [{ type: 'wrong-tap' }],
    };
  }

  // On the first tap, hide ALL remaining numbers (classic Chimp Test: memorise the rest).
  const isFirstTap = session.nextExpected === 1;
  const newCells: ChimpTestCell[] = session.cells.map((c) =>
    isFirstTap || c.number === session.nextExpected ? { ...c, hidden: true } : c,
  );

  const newNextExpected = session.nextExpected + 1;

  if (newNextExpected > session.currentCount) {
    const roundTime = action.elapsedSeconds - session.roundStartElapsed;
    const newRoundTimes = [...session.roundTimes, roundTime];

    if (session.currentCount >= session.puzzle.maxCount) {
      return {
        changed: true,
        session: { ...session, cells: newCells, status: 'won', roundTimes: newRoundTimes },
        effects: [],
      };
    }

    const nextCount = session.currentCount + 1;
    return {
      changed: true,
      session: {
        ...session,
        currentCount: nextCount,
        cells: generateChimpTestCells(nextCount, session.puzzle.gridSize),
        nextExpected: 1,
        roundTimes: newRoundTimes,
        roundStartElapsed: action.elapsedSeconds,
      },
      effects: [],
    };
  }

  return {
    changed: true,
    session: { ...session, cells: newCells, nextExpected: newNextExpected },
    effects: [],
  };
}
