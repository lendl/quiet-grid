import type { PersistedSessionEnvelope } from '../../../../app/shell/types';
import type { TakuzuPuzzleSession } from '../../types';

export function serializeTakuzuSession(
  session: TakuzuPuzzleSession,
): PersistedSessionEnvelope<TakuzuPuzzleSession> {
  return {
    gameId: 'takuzu',
    version: 1,
    payload: session,
  };
}

export function isTakuzuSessionPayload(value: unknown): value is TakuzuPuzzleSession {
  return Boolean(value && typeof value === 'object' && 'puzzle' in (value as Record<string, unknown>));
}
