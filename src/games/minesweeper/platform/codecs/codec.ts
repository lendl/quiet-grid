import type { PersistedSessionEnvelope } from '../../../../app/shell/types';
import type { MinesweeperPuzzleSession } from '../../types';

export function serializeMinesweeperSession(
  session: MinesweeperPuzzleSession,
): PersistedSessionEnvelope<MinesweeperPuzzleSession> {
  return {
    gameId: 'minesweeper',
    version: 1,
    payload: session,
  };
}

export function isMinesweeperSessionPayload(value: unknown): value is MinesweeperPuzzleSession {
  return Boolean(value && typeof value === 'object' && 'board' in (value as Record<string, unknown>));
}
