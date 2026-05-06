import { getMinesweeperLearningCenterContent as getContent } from '../i18n';

interface MinesweeperCellRef {
  row: number;
  col: number;
}

interface SafeRevealCopyParams {
  clueCell: MinesweeperCellRef;
  targetCount: number;
  mineCount: number;
}

interface NextMoveCopy {
  title: string;
  body: string;
}

function formatCell({ row, col }: MinesweeperCellRef): string {
  return `row ${row + 1}, column ${col + 1}`;
}

function pluralize(count: number, singular: string, plural: string): string {
  return count === 1 ? singular : plural;
}

export function buildSafeRevealNextMove(params: SafeRevealCopyParams): NextMoveCopy {
  const clueLabel = formatCell(params.clueCell);
  const tileLabel = pluralize(params.targetCount, 'tile', 'tiles');
  const mineLabel = pluralize(params.mineCount, 'mine', 'mines');
  return getContent().safeReveal(clueLabel, tileLabel, params.mineCount, mineLabel);
}

export function buildGuessNextMove(): NextMoveCopy {
  return getContent().guess;
}
