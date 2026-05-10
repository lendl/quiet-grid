export interface GeneratedPuzzleTemplate {
  id: string;
  difficulty: 'easy' | 'medium' | 'hard' | 'expert';
  size: number;
  solution: string;
}

export function generatePuzzleTemplate(size: number): GeneratedPuzzleTemplate {
  return {
    id: `__PREFIX__-${size}-001`,
    difficulty: 'easy',
    size,
    solution: '__SOLUTION__',
  };
}
