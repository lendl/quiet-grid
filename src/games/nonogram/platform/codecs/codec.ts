import type { PuzzleSessionEnvelope } from '../../../../app/shell/types';
import type { NonogramActivePuzzle } from '../../gameplay/activePuzzle';

export function serializeNonogramSession(
  session: NonogramActivePuzzle,
): PuzzleSessionEnvelope<NonogramActivePuzzle> {
  return {
    puzzleTypeId: 'nonogram',
    version: 1,
    payload: session,
  };
}

export function isNonogramSessionPayload(value: unknown): value is NonogramActivePuzzle {
  return Boolean(value && typeof value === 'object' && 'puzzleTypeId' in (value as Record<string, unknown>));
}
