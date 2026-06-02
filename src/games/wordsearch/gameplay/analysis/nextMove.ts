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
  if (session.hiddenWordMode) {
    return null;
  }

  const strings = getWordSearchStrings();
  const targetWord = session.puzzle.words.find((word) => !session.foundWordIds.includes(word.id));
  if (targetWord) {
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
