import AsyncStorage from '@react-native-async-storage/async-storage';
import type { PuzzleSessionEnvelope } from '../types';
import { ACTIVE_PUZZLE_KEY } from '../../utils/storageKeys';

function isStoredPuzzleSessionEnvelope(value: unknown): boolean {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const envelope = value as Record<string, unknown>;
  return (
    typeof envelope.puzzleTypeId === 'string'
    && Number.isInteger(envelope.version)
    && 'payload' in envelope
  );
}

export async function loadActivePuzzle(): Promise<unknown> {
  let raw: string | null;

  try {
    raw = await AsyncStorage.getItem(ACTIVE_PUZZLE_KEY);
  } catch {
    return null;
  }

  if (!raw) {
    return null;
  }

  try {
    const parsed: unknown = JSON.parse(raw);
    if (isStoredPuzzleSessionEnvelope(parsed)) {
      return parsed;
    }

    return parsed;
  } catch {
    return null;
  }
}

export async function saveActivePuzzle<TPayload>(
  envelope: PuzzleSessionEnvelope<TPayload>,
): Promise<void> {
  try {
    await AsyncStorage.setItem(ACTIVE_PUZZLE_KEY, JSON.stringify(envelope));
  } catch {
    // Keep app stable if save fails.
  }
}

export async function clearActivePuzzle(): Promise<void> {
  try {
    await AsyncStorage.removeItem(ACTIVE_PUZZLE_KEY);
  } catch {
    // Keep app stable if cleanup fails.
  }
}
