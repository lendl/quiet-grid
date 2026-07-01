import type { WordSearchCellRef } from '../types';

export interface ReservedHiddenWord {
  word: string;
  positions: WordSearchCellRef[];
}

function randomInt(maxExclusive: number): number {
  return Math.floor(Math.random() * maxExclusive);
}

function normalizeWordToken(value: string): string {
  return value.trim().toUpperCase().replace(/[^A-Z]/g, '');
}

export function buildHiddenWordPool(themeWords: readonly string[]): string[] {
  const seen = new Set<string>();
  const pool: string[] = [];
  for (const word of themeWords) {
    const normalized = normalizeWordToken(word);
    if (normalized.length >= 3 && !seen.has(normalized)) {
      seen.add(normalized);
      pool.push(normalized);
    }
  }
  return pool;
}

export function pickHiddenWord(pool: readonly string[], rows: number, cols: number): string | null {
  const maxLength = rows * cols;
  const candidates = pool.filter((word) => word.length <= maxLength);
  if (candidates.length === 0) {
    return null;
  }
  return candidates[randomInt(candidates.length)]!;
}

// Reserves `word.length` random grid cells for the hidden word, sorted into
// reading order (top-to-bottom, left-to-right) so the letters spell the word
// when read in the order a player naturally scans the finished grid.
export function reserveHiddenWordCells(word: string, rows: number, cols: number): ReservedHiddenWord {
  const allCells: WordSearchCellRef[] = [];
  for (let row = 0; row < rows; row += 1) {
    for (let col = 0; col < cols; col += 1) {
      allCells.push({ row, col });
    }
  }
  for (let i = 0; i < word.length; i += 1) {
    const j = i + randomInt(allCells.length - i);
    const tmp = allCells[i]!;
    allCells[i] = allCells[j]!;
    allCells[j] = tmp;
  }
  const positions = allCells
    .slice(0, word.length)
    .sort((a, b) => (a.row !== b.row ? a.row - b.row : a.col - b.col));
  return { word, positions };
}
