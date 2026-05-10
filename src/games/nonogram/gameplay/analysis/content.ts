import type { NonogramExplanationCopy } from '../../types';
import { getNonogramAnalysisContent } from '../../content/i18n';
import type { NonogramDeduction } from '../rules/solver';

export function describeNonogramDeduction(deduction: NonogramDeduction): NonogramExplanationCopy {
  const content = getNonogramAnalysisContent();
  const lineLabel = content.lineLabel(deduction.line.axis, deduction.line.index);
  const clueLabel = content.clueLabel(deduction.line.clues);
  const cellCount = deduction.targetCells.length;

  if (deduction.kind === 'complete-line') {
    return content.completeLine(lineLabel, clueLabel);
  }

  if (deduction.apply === 'filled') {
    return content.overlapFill(lineLabel, clueLabel, cellCount);
  }

  return content.forcedEmpty(lineLabel, clueLabel, cellCount);
}

export function getGroupedDeductionCopy(deduction: NonogramDeduction): NonogramExplanationCopy {
  const content = getNonogramAnalysisContent();
  return content.groupedStep(
    deduction.apply,
    content.lineLabel(deduction.line.axis, deduction.line.index),
    content.clueLabel(deduction.line.clues),
    deduction.targetCells.length,
  );
}
