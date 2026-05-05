import type { PuzzleSessionEnvelope } from '../../../app/shell/types';
import type { BinaryPuzzleSession } from '../types';

export function serializeBinarySession(
  session: BinaryPuzzleSession,
): PuzzleSessionEnvelope<BinaryPuzzleSession> {
  return {
    puzzleTypeId: 'binary',
    version: 1,
    payload: session,
  };
}

export function isBinarySessionPayload(value: unknown): value is BinaryPuzzleSession {
  return Boolean(value && typeof value === 'object' && 'puzzle' in (value as Record<string, unknown>));
}
