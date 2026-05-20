import AsyncStorage from '@react-native-async-storage/async-storage';
import type { PersistedSessionEnvelope } from '../types';
import { ACTIVE_PUZZLE_KEY } from '../../utils/storageKeys';

function isStoredPersistedSessionEnvelope(value: unknown): boolean {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const envelope = value as Record<string, unknown>;
  return (
    (typeof envelope.gameId === 'string' || typeof envelope.puzzleTypeId === 'string')
    && Number.isInteger(envelope.version)
    && 'payload' in envelope
  );
}

export async function loadActiveSession(): Promise<unknown> {
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
    if (isStoredPersistedSessionEnvelope(parsed)) {
      return parsed;
    }

    return parsed;
  } catch {
    return null;
  }
}

export async function saveActiveSession<TPayload>(
  envelope: PersistedSessionEnvelope<TPayload>,
): Promise<void> {
  try {
    await AsyncStorage.setItem(ACTIVE_PUZZLE_KEY, JSON.stringify(envelope));
  } catch {
    // Keep app stable if save fails.
  }
}

export async function clearActiveSession(): Promise<void> {
  try {
    await AsyncStorage.removeItem(ACTIVE_PUZZLE_KEY);
  } catch {
    // Keep app stable if cleanup fails.
  }
}
