import type { PuzzleDifficulty } from '../../../shared/types';

export const sudokuTechniques = [
  'naked-single',
  'hidden-single',
  'naked-pair',
  'hidden-pair',
  'pointing-pair-triple',
  'box-line-reduction',
  'x-wing',
  'swordfish',
  'xy-wing',
  'xyz-wing',
  'coloring',
  'chains',
] as const;

export type SudokuTechnique = typeof sudokuTechniques[number];

export const sudokuTechniqueDifficultyFloor: Record<SudokuTechnique, PuzzleDifficulty> = {
  'naked-single': 'easy',
  'hidden-single': 'easy',
  'naked-pair': 'easy',
  'hidden-pair': 'medium',
  'pointing-pair-triple': 'medium',
  'box-line-reduction': 'medium',
  'x-wing': 'hard',
  'swordfish': 'hard',
  'xy-wing': 'hard',
  'xyz-wing': 'expert',
  'coloring': 'expert',
  'chains': 'expert',
};

const sudokuTechniqueRank = sudokuTechniques.reduce<Record<SudokuTechnique, number>>((acc, technique, index) => {
  acc[technique] = index;
  return acc;
}, {} as Record<SudokuTechnique, number>);

export function compareSudokuTechniques(a: SudokuTechnique, b: SudokuTechnique): number {
  return sudokuTechniqueRank[a] - sudokuTechniqueRank[b];
}

export function getHardestSudokuTechnique(
  techniques: readonly SudokuTechnique[],
): SudokuTechnique | null {
  if (techniques.length === 0) {
    return null;
  }

  return techniques.reduce((hardest, technique) => (
    compareSudokuTechniques(technique, hardest) > 0 ? technique : hardest
  ));
}
