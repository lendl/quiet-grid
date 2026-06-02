import type { PuzzleDifficulty } from '../shared/types';

export type WordSearchLanguage = 'en' | 'nl' | 'de' | 'fr' | 'es';
export type WordSearchDirection =
  | 'right'
  | 'left'
  | 'down'
  | 'up'
  | 'down-right'
  | 'down-left'
  | 'up-right'
  | 'up-left';

export interface WordSearchCellRef {
  row: number;
  col: number;
}

export interface WordSearchWordEntry {
  id: string;
  word: string;
  positions: WordSearchCellRef[];
}

export interface WordSearchHiddenWord {
  word: string;
  clue: string;
  positions: WordSearchCellRef[];
}

export interface WordSearchPuzzle {
  id: string;
  difficulty: PuzzleDifficulty;
  rows: number;
  cols: number;
  language: WordSearchLanguage;
  themeId: string;
  grid: string[][];
  words: WordSearchWordEntry[];
  hiddenWord: WordSearchHiddenWord;
}

export interface WordSearchSelection {
  start: WordSearchCellRef;
  end: WordSearchCellRef;
  path: WordSearchCellRef[];
}

export interface WordSearchSession {
  puzzle: WordSearchPuzzle;
  foundWordIds: string[];
  tempSelection: WordSearchSelection | null;
  hiddenWordMode: boolean;
  hiddenWordProgress: WordSearchCellRef[];
  hiddenWordSolved: boolean;
}

export interface WordSearchActiveSession extends WordSearchSession {
  gameId: 'wordsearch';
  elapsedSeconds: number;
}

export function cloneWordSearchCell(cell: WordSearchCellRef): WordSearchCellRef {
  return { row: cell.row, col: cell.col };
}

export function cloneWordSearchWord(word: WordSearchWordEntry): WordSearchWordEntry {
  return {
    ...word,
    positions: word.positions.map(cloneWordSearchCell),
  };
}

export function cloneWordSearchPuzzle(puzzle: WordSearchPuzzle): WordSearchPuzzle {
  return {
    ...puzzle,
    grid: puzzle.grid.map((row) => [...row]),
    words: puzzle.words.map(cloneWordSearchWord),
    hiddenWord: {
      ...puzzle.hiddenWord,
      positions: puzzle.hiddenWord.positions.map(cloneWordSearchCell),
    },
  };
}

export function cloneWordSearchSelection(selection: WordSearchSelection | null): WordSearchSelection | null {
  if (!selection) {
    return null;
  }

  return {
    start: cloneWordSearchCell(selection.start),
    end: cloneWordSearchCell(selection.end),
    path: selection.path.map(cloneWordSearchCell),
  };
}

export function cloneWordSearchSession(session: WordSearchSession): WordSearchSession {
  return {
    puzzle: cloneWordSearchPuzzle(session.puzzle),
    foundWordIds: [...session.foundWordIds],
    tempSelection: cloneWordSearchSelection(session.tempSelection),
    hiddenWordMode: session.hiddenWordMode,
    hiddenWordProgress: session.hiddenWordProgress.map(cloneWordSearchCell),
    hiddenWordSolved: session.hiddenWordSolved,
  };
}

export function isWordSearchSolved(session: WordSearchSession): boolean {
  return session.foundWordIds.length >= session.puzzle.words.length
    && session.hiddenWordSolved;
}
