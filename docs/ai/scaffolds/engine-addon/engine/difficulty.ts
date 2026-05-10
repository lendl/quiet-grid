export type DifficultyEvidenceTemplate = {
  moveCount: number;
  advancedMoveCount: number;
  guessCount: number;
};

export function classifyDifficultyTemplate(
  evidence: DifficultyEvidenceTemplate,
): 'easy' | 'medium' | 'hard' | 'expert' | null {
  if (evidence.guessCount > 0) {
    return null;
  }
  if (evidence.advancedMoveCount >= 8) {
    return 'expert';
  }
  if (evidence.advancedMoveCount >= 4) {
    return 'hard';
  }
  if (evidence.moveCount >= 6) {
    return 'medium';
  }
  return 'easy';
}
