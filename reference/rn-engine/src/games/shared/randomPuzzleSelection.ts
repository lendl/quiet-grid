import type { PuzzleDifficulty } from './types';

type DifficultyEntry = {
  id: string;
  difficulty: PuzzleDifficulty;
};

const lastSelectedByKey = new Map<string, string>();

function buildSelectionKey(gameId: string, difficulty: PuzzleDifficulty): string {
  return `${gameId}:${difficulty}`;
}

export function pickRandomPuzzleForDifficulty<T extends DifficultyEntry>(
  gameId: string,
  entries: readonly T[],
  difficulty: PuzzleDifficulty,
): T | null {
  const matches = entries.filter((entry) => entry.difficulty === difficulty);
  if (matches.length === 0) {
    return null;
  }

  const selectionKey = buildSelectionKey(gameId, difficulty);
  const lastSelectedId = lastSelectedByKey.get(selectionKey);
  const candidatePool = matches.length > 1 && lastSelectedId
    ? matches.filter((entry) => entry.id !== lastSelectedId)
    : matches;
  const pool = candidatePool.length > 0 ? candidatePool : matches;
  const chosen = pool[Math.floor(Math.random() * pool.length)] ?? null;

  if (chosen) {
    lastSelectedByKey.set(selectionKey, chosen.id);
  }

  return chosen;
}
