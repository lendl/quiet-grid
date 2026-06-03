import type { WordSearchCellRef, WordSearchSession } from '../../types';
import { getWordSearchStrings } from '../../content/strings';

export interface WordSearchNextMoveHint {
  kind: 'find-word' | 'find-hidden-word-letter';
  ruleKey: 'scan-line' | 'hidden-word';
  title: string;
  body: string;
  evidenceCells: WordSearchCellRef[];
  targetCells: [WordSearchCellRef];
}

export function getWordSearchNextMoveHint(session: WordSearchSession): WordSearchNextMoveHint | null {
  const strings = getWordSearchStrings();

  if (session.hiddenWordMode) {
    if (session.hiddenWordSolved) {
      return null;
    }
    const nextHiddenCell = session.puzzle.hiddenWord.positions[session.hiddenWordProgress.length];
    if (!nextHiddenCell) {
      return null;
    }
    return {
      kind: 'find-hidden-word-letter',
      ruleKey: 'hidden-word',
      title: strings.play.hiddenWord.nextLetterTitle(session.puzzle.hiddenWord.clue),
      body: strings.play.hiddenWord.nextLetterBody,
      evidenceCells: [{ ...nextHiddenCell }],
      targetCells: [{ ...nextHiddenCell }],
    };
  }

  const foundIds = new Set(session.foundWordIds);
  const unfoundWords = session.puzzle.words.filter((word) => !foundIds.has(word.id));

  if (unfoundWords.length > 0) {
    // Build a set of cells that are part of already-found words.
    const foundCellKeys = new Set<string>();
    session.puzzle.words.forEach((word) => {
      if (foundIds.has(word.id)) {
        word.positions.forEach((cell) => foundCellKeys.add(`${cell.row},${cell.col}`));
      }
    });

    // Prefer an unfound word that shares a cell with a found word — the player
    // can already see that cell highlighted green and work from it.
    for (const word of unfoundWords) {
      const overlapCell = word.positions.find(
        (cell) => foundCellKeys.has(`${cell.row},${cell.col}`),
      );
      if (overlapCell) {
        return {
          kind: 'find-word',
          ruleKey: 'scan-line',
          title: strings.play.nextMove.title(word.word),
          body: strings.play.nextMove.body,
          evidenceCells: [{ ...overlapCell }],
          targetCells: [{ ...overlapCell }],
        };
      }
    }

    // No overlap with any found word — hint with the first cell of the first unfound word.
    const targetWord = unfoundWords[0]!;
    const targetCell = targetWord.positions[0];
    if (!targetCell) {
      return null;
    }
    return {
      kind: 'find-word',
      ruleKey: 'scan-line',
      title: strings.play.nextMove.title(targetWord.word),
      body: strings.play.nextMove.body,
      evidenceCells: [{ ...targetCell }],
      targetCells: [{ ...targetCell }],
    };
  }

  if (session.hiddenWordSolved) {
    return null;
  }

  const nextHiddenCell = session.puzzle.hiddenWord.positions[session.hiddenWordProgress.length];
  if (!nextHiddenCell) {
    return null;
  }
  return {
    kind: 'find-hidden-word-letter',
    ruleKey: 'hidden-word',
    title: strings.play.hiddenWord.nextLetterTitle(session.puzzle.hiddenWord.clue),
    body: strings.play.hiddenWord.nextLetterBody,
    evidenceCells: [{ ...nextHiddenCell }],
    targetCells: [{ ...nextHiddenCell }],
  };
}
