import { getTakuzuLearningCenterContent as getContent } from '../../content/i18n';
import type { LineKind } from './helpers';

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
  proofStepCount: number;
  proofUsesRule: 'find-pairs' | 'avoid-trios' | 'complete-lines' | 'eliminate-filled-lines' | null;
  contradictionKind: 'triple' | 'balance' | 'duplicate-line';
  contradictionLineKind: LineKind;
  contradictionLineIndex: number;
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
    getContent().lineLabel(lineKind, lineIndex),
    repeatedValue,
    targetValue,
    getContent().cellLabel(targetCount),
  );
}

export function buildAvoidTriosNextMove({
  lineKind,
  lineIndex,
  repeatedValue,
  targetValue,
}: AvoidTriosProgressParams): NextMoveCopy {
  return getContent().avoidTrios(getContent().lineLabel(lineKind, lineIndex), repeatedValue, targetValue);
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
    getContent().lineLabel(lineKind, lineIndex),
    filledValue,
    filledCount,
    targetValue,
    getContent().cellLabel(targetCount),
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
    getContent().lineLabel(lineKind, lineIndex),
    getContent().lineLabel(lineKind, matchingLineIndex),
    targetValue,
    getContent().cellLabel(targetCount),
    getContent().lineKindLabel(lineKind, targetCount),
  );
}

export function buildEliminateImpossibleCombinationsNextMove({
  lineKind,
  lineIndex,
  validCompletionCount,
  blockedValue,
  targetValue,
  proofStepCount,
  proofUsesRule,
  contradictionKind,
  contradictionLineKind,
  contradictionLineIndex,
}: EliminateImpossibleCombinationsProgressParams): NextMoveCopy {
  const proofRuleLabel = getContent().ruleLabel(
    proofStepCount === 1 ? proofUsesRule : null
  );
  return getContent().eliminateImpossible(
    getContent().lineLabel(lineKind, lineIndex),
    validCompletionCount,
    blockedValue,
    targetValue,
    getContent().cellLabel(1),
    contradictionKind,
    getContent().lineLabel(contradictionLineKind, contradictionLineIndex),
    proofRuleLabel,
  );
}

export function buildAvoidTriosRepair({
  lineKind,
  lineIndex,
  repeatedValue,
}: AvoidTriosRecoveryParams): NextMoveCopy {
  return getContent().avoidTriosRepair(getContent().lineLabel(lineKind, lineIndex), repeatedValue);
}

export function buildCompleteLinesRepair({
  lineKind,
  lineIndex,
  filledValue,
  filledCount,
  limit,
}: CompleteLinesRecoveryParams): NextMoveCopy {
  return getContent().completeLinesRepair(
    getContent().lineLabel(lineKind, lineIndex),
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
    getContent().lineLabel(lineKind, firstLineIndex),
    getContent().lineLabel(lineKind, secondLineIndex),
    getContent().lineKindLabel(lineKind, 2),
  );
}
