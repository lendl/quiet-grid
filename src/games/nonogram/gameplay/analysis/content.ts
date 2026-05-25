import { getNonogramStrings } from '../../content/i18n';
import type { NonogramLineOrientation } from '../../types';

function formatLineLabel(orientation: NonogramLineOrientation, index: number): string {
  return `${orientation === 'row' ? 'Row' : 'Column'} ${index + 1}`;
}

export function buildInvalidBoardCopy(
  orientation: NonogramLineOrientation,
  index: number,
): { title: string; body: string; lineLabel: string } {
  const strings = getNonogramStrings().play.analysis.invalidBoard;
  const lineLabel = formatLineLabel(orientation, index);
  return {
    title: strings.title,
    body: strings.body(lineLabel),
    lineLabel,
  };
}

export function buildOverlapFillCopy(
  orientation: NonogramLineOrientation,
  index: number,
  targetCount: number,
): { title: string; body: string; lineLabel: string } {
  const strings = getNonogramStrings().play.analysis.overlapFill;
  const lineLabel = formatLineLabel(orientation, index);
  return {
    title: strings.title,
    body: strings.body(lineLabel, targetCount),
    lineLabel,
  };
}

export function buildForcedEmptyCopy(
  orientation: NonogramLineOrientation,
  index: number,
  targetCount: number,
): { title: string; body: string; lineLabel: string } {
  const strings = getNonogramStrings().play.analysis.forcedEmpty;
  const lineLabel = formatLineLabel(orientation, index);
  return {
    title: strings.title,
    body: strings.body(lineLabel, targetCount),
    lineLabel,
  };
}

export function buildCompleteLineCopy(
  orientation: NonogramLineOrientation,
  index: number,
  targetCount: number,
): { title: string; body: string; lineLabel: string } {
  const strings = getNonogramStrings().play.analysis.completeLine;
  const lineLabel = formatLineLabel(orientation, index);
  return {
    title: strings.title,
    body: strings.body(lineLabel, targetCount),
    lineLabel,
  };
}

export { formatLineLabel as formatNonogramLineLabel };
