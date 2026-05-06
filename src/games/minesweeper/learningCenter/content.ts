import {
  getMinesweeperLearningCenterContent as getContent,
  type MinesweeperLearningCenterPatternKey,
} from '../i18n';

interface MinesweeperCellRef {
  row: number;
  col: number;
}

interface SafeRevealCopyParams {
  patternKey: MinesweeperLearningCenterPatternKey;
  clueCell?: MinesweeperCellRef;
  secondaryClueCell?: MinesweeperCellRef;
  targetCount: number;
  mineCount?: number;
}

interface NextMoveTeachingCopy {
  patternTitle: string;
  patternLabel: string;
  explanationTitle: string;
  explanation: string;
}

interface NextMoveCopy {
  title: string;
  body: string;
  teaching?: NextMoveTeachingCopy;
}

function formatCell({ row, col }: MinesweeperCellRef): string {
  return `row ${row + 1}, column ${col + 1}`;
}

function pluralize(count: number, singular: string, plural: string): string {
  return count === 1 ? singular : plural;
}

export function buildPatternNextMove(params: SafeRevealCopyParams): NextMoveCopy {
  const clueLabel = params.clueCell ? formatCell(params.clueCell) : undefined;
  const secondaryClueLabel = params.secondaryClueCell
    ? formatCell(params.secondaryClueCell)
    : undefined;
  const tileLabel = pluralize(params.targetCount, 'tile', 'tiles');
  const mineCount = params.mineCount ?? 1;
  const mineLabel = pluralize(mineCount, 'mine', 'mines');
  return getContent().nextMovePattern({
    patternKey: params.patternKey,
    clueLabel,
    secondaryClueLabel,
    tileLabel,
    mineLabel,
    mineCount,
  });
}

export function buildGuessNextMove(): NextMoveCopy {
  return getContent().guess;
}
