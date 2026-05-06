import type { PuzzleSessionEnvelope } from '../../../app/shell/types';
import type { TakuzuPuzzleSession } from '../types';

export function serializeTakuzuSession(
  session: TakuzuPuzzleSession,
): PuzzleSessionEnvelope<TakuzuPuzzleSession> {
  return {
    puzzleTypeId: 'takuzu',
    version: 1,
    payload: session,
  };
}

export function isTakuzuSessionPayload(value: unknown): value is TakuzuPuzzleSession {
  return Boolean(value && typeof value === 'object' && 'puzzle' in (value as Record<string, unknown>));
}
