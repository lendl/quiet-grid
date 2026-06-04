import { getTakuzuLearningCenterContent as getContent } from '../../content/i18n';

type LineKind = 'row' | 'column';

interface NextMoveCopy {
  title: string;
  body: string;
}

interface ProgressLineParams {
  lineKind: LineKind;
  lineIndex: number;
}

interface FindPairsProgressParams extends ProgressLineParams {
  repeatedValue: 0 | 1;
  targetValue: 0 | 1;
  targetCount: number;
}

interface AvoidTriosProgressParams extends ProgressLineParams {
  repeatedValue: 0 | 1;
  targetValue: 0 | 1;
}

interface CompleteLinesProgressParams extends ProgressLineParams {
  filledValue: 0 | 1;
  filledCount: number;
  targetValue: 0 | 1;
  targetCount: number;
}

interface EliminateFilledLinesProgressParams extends ProgressLineParams {
  matchingLineIndex: number;
  targetValue: 0 | 1;
  targetCount: number;
}

interface EliminateImpossibleCombinationsProgressParams extends ProgressLineParams {
  validCompletionCount: number;
  blockedValue: 0 | 1;
  targetValue: 0 | 1;
  cellLabel: string;
  contradictionKind: 'triple' | 'balance' | 'duplicate-line';
  contradictionLineKind: LineKind;
  contradictionLineIndex: number;
  proofRuleLabel: string;
}

interface AvoidTriosRecoveryParams extends ProgressLineParams {
  repeatedValue: 0 | 1;
}

interface CompleteLinesRecoveryParams extends ProgressLineParams {
  filledValue: 0 | 1;
  filledCount: number;
  limit: number;
}

interface EliminateFilledLinesRecoveryParams {
  lineKind: LineKind;
  firstLineIndex: number;
  secondLineIndex: number;
}

function formatLine(lineKind: LineKind, lineIndex: number): string {
  return `${lineKind} ${lineIndex + 1}`;
}

function pluralize(count: number, singular: string, plural: string): string {
  return count === 1 ? singular : plural;
}

export function buildPausedNextMove(): NextMoveCopy {
  return getContent().pausedNextMove;
}

export function buildFindPairsNextMove({
  lineKind,
  lineIndex,
  repeatedValue,
  targetValue,
  targetCount,
}: FindPairsProgressParams): NextMoveCopy {
  return getContent().findPairs(
    formatLine(lineKind, lineIndex),
    repeatedValue,
    targetValue,
    pluralize(targetCount, 'cell', 'cells'),
  );
}

export function buildAvoidTriosNextMove({
  lineKind,
  lineIndex,
  repeatedValue,
  targetValue,
}: AvoidTriosProgressParams): NextMoveCopy {
  return getContent().avoidTrios(formatLine(lineKind, lineIndex), repeatedValue, targetValue);
}

export function buildCompleteLinesNextMove({
  lineKind,
  lineIndex,
  filledValue,
  filledCount,
  targetValue,
  targetCount,
}: CompleteLinesProgressParams): NextMoveCopy {
  return getContent().completeLines(
    formatLine(lineKind, lineIndex),
    filledValue,
    filledCount,
    targetValue,
    pluralize(targetCount, 'cell', 'cells'),
  );
}

export function buildEliminateFilledLinesNextMove({
  lineKind,
  lineIndex,
  matchingLineIndex,
  targetValue,
  targetCount,
}: EliminateFilledLinesProgressParams): NextMoveCopy {
  return getContent().eliminateFilledLines(
    formatLine(lineKind, lineIndex),
    formatLine(lineKind, matchingLineIndex),
    targetValue,
    pluralize(targetCount, 'cell', 'cells'),
    pluralize(targetCount, lineKind, `${lineKind}s`),
  );
}

export function buildEliminateImpossibleCombinationsNextMove({
  lineKind,
  lineIndex,
  validCompletionCount,
  blockedValue,
  targetValue,
  cellLabel,
  contradictionKind,
  contradictionLineKind,
  contradictionLineIndex,
  proofRuleLabel,
}: EliminateImpossibleCombinationsProgressParams): NextMoveCopy {
  return getContent().eliminateImpossible(
    formatLine(lineKind, lineIndex),
    validCompletionCount,
    blockedValue,
    targetValue,
    cellLabel,
    contradictionKind,
    formatLine(contradictionLineKind, contradictionLineIndex),
    proofRuleLabel,
  );
}

export function buildAvoidTriosRepair({
  lineKind,
  lineIndex,
  repeatedValue,
}: AvoidTriosRecoveryParams): NextMoveCopy {
  return getContent().avoidTriosRepair(formatLine(lineKind, lineIndex), repeatedValue);
}

export function buildCompleteLinesRepair({
  lineKind,
  lineIndex,
  filledValue,
  filledCount,
  limit,
}: CompleteLinesRecoveryParams): NextMoveCopy {
  return getContent().completeLinesRepair(
    formatLine(lineKind, lineIndex),
    filledValue,
    filledCount,
    limit,
  );
}

export function buildEliminateFilledLinesRepair({
  lineKind,
  firstLineIndex,
  secondLineIndex,
}: EliminateFilledLinesRecoveryParams): NextMoveCopy {
  return getContent().eliminateFilledLinesRepair(
    formatLine(lineKind, firstLineIndex),
    formatLine(lineKind, secondLineIndex),
    pluralize(2, lineKind, `${lineKind}s`),
  );
}
