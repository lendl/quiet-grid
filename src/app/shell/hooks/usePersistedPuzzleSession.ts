import { useActivePuzzlePersistence } from '../../hooks/useActivePuzzlePersistence';

export function usePersistedPuzzleSession<T>(
  args: Parameters<typeof useActivePuzzlePersistence<T>>[0],
) {
  useActivePuzzlePersistence(args);
}
