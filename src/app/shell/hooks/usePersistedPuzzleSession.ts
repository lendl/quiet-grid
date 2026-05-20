import { useActiveSessionPersistence } from '../../hooks/useActivePuzzlePersistence';

export function usePersistedPuzzleSession<T>(
  args: Parameters<typeof useActiveSessionPersistence<T>>[0],
) {
  useActiveSessionPersistence(args);
}
