import type { PersistedSessionEnvelope } from '../../../../app/shell/types';
import type { PuzzleDifficulty } from '../../../shared/types';
import type { NonogramSession } from '../../gameplay/activePuzzle';

export interface NonogramCatalogEntry {
  id: string;
  difficulty: PuzzleDifficulty;
  rows: number;
  cols: number;
  solution: boolean[][];
}

export function normalizeNonogramCatalogEntry(entry: NonogramCatalogEntry): NonogramCatalogEntry {
  return {
    ...entry,
    rows: entry.solution.length,
    cols: entry.solution[0]?.length ?? 0,
    solution: entry.solution.map((row) => [...row]),
  };
}

export function serializeNonogramSession(
  session: NonogramSession,
): PersistedSessionEnvelope<NonogramSession> {
  return {
    gameId: 'nonogram',
    version: 1,
    payload: session,
  };
}

export function isNonogramSessionPayload(value: unknown): value is NonogramSession {
  return Boolean(value && typeof value === 'object' && 'puzzle' in (value as Record<string, unknown>));
}
