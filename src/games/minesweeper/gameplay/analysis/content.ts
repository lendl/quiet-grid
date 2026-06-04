import {
  getMinesweeperLearningCenterContent as getContent,
  type MinesweeperMineFlagReason,
  type MinesweeperLearningCenterPatternKey,
} from '../../content/i18n';

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

interface MineFlagCopyParams {
  reason: MinesweeperMineFlagReason;
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
  return getContent().formatCellLabel({ row, col });
}

export function buildPatternNextMove(params: SafeRevealCopyParams): NextMoveCopy {
  const content = getContent();
  const clueLabel = params.clueCell ? formatCell(params.clueCell) : undefined;
  const secondaryClueLabel = params.secondaryClueCell
    ? formatCell(params.secondaryClueCell)
    : undefined;
  const tileLabel = content.tileLabel(params.targetCount);
  const mineCount = params.mineCount ?? 1;
  const mineLabel = content.mineLabel(mineCount);
  return content.nextMovePattern({
    patternKey: params.patternKey,
    clueLabel,
    secondaryClueLabel,
    tileLabel,
    mineLabel,
    mineCount,
  });
}

export function buildFlagNextMove(params: MineFlagCopyParams): NextMoveCopy {
  const content = getContent();
  const clueLabel = params.clueCell ? formatCell(params.clueCell) : undefined;
  const secondaryClueLabel = params.secondaryClueCell
    ? formatCell(params.secondaryClueCell)
    : undefined;
  const tileLabel = content.tileLabel(params.targetCount);
  const mineCount = params.mineCount ?? params.targetCount;
  const mineLabel = content.mineLabel(mineCount);
  return content.flagMovePattern({
    reason: params.reason,
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
