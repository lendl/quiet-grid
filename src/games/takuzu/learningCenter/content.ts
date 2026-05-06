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
  blockedValue: 0 | 1;
  targetValue: 0 | 1;
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
  if (getCurrentLanguage() === 'nl') {
    return {
      title: 'Nog geen duidelijke volgende zet',
      body: 'Dit deel van de puzzel biedt nu geen sterke volgende zet. Probeer een andere rij of kolom en vraag daarna opnieuw.',
    };
  }

  return {
    title: 'No clear next move yet',
    body: 'This part of the puzzle does not offer a strong next move right now. Try another row or column, then ask again.',
  };
}

export function buildFindPairsNextMove({
  lineKind,
  lineIndex,
  repeatedValue,
  targetValue,
  targetCount,
}: FindPairsProgressParams): NextMoveCopy {
  const lineLabel = formatLine(lineKind, lineIndex);
  const cellLabel = pluralize(targetCount, 'cell', 'cells');

  if (getCurrentLanguage() === 'nl') {
    return {
      title: `Volgende zet in ${lineLabel}`,
      body: `Plaats ${targetValue} in de gemarkeerde ${cellLabel}. Waarom: er staan al twee ${repeatedValue}'en naast elkaar in ${lineLabel}, dus nog een ${repeatedValue} zou drie op rij maken.`,
    };
  }

  return {
    title: `Next move in ${lineLabel}`,
    body: `Place ${targetValue} in the highlighted ${cellLabel}. Why: two ${repeatedValue}s already sit together in ${lineLabel}, so another ${repeatedValue} would create three in a row.`,
  };
}

export function buildAvoidTriosNextMove({
  lineKind,
  lineIndex,
  repeatedValue,
  targetValue,
}: AvoidTriosProgressParams): NextMoveCopy {
  const lineLabel = formatLine(lineKind, lineIndex);

  if (getCurrentLanguage() === 'nl') {
    return {
      title: `Volgende zet in ${lineLabel}`,
      body: `Plaats ${targetValue} in de gemarkeerde cel. Waarom: ${lineLabel} toont al ${repeatedValue} _ ${repeatedValue}, dus de open cel ertussen moet ${targetValue} zijn om drie op rij te vermijden.`,
    };
  }

  return {
    title: `Next move in ${lineLabel}`,
    body: `Place ${targetValue} in the highlighted cell. Why: ${lineLabel} already shows ${repeatedValue} _ ${repeatedValue}, so the open cell between them must be ${targetValue} to avoid three in a row.`,
  };
}

export function buildCompleteLinesNextMove({
  lineKind,
  lineIndex,
  filledValue,
  filledCount,
  targetValue,
  targetCount,
}: CompleteLinesProgressParams): NextMoveCopy {
  const lineLabel = formatLine(lineKind, lineIndex);
  const cellLabel = pluralize(targetCount, 'cell', 'cells');

  if (getCurrentLanguage() === 'nl') {
    return {
      title: `Volgende zet in ${lineLabel}`,
      body: `Plaats ${targetValue} in de gemarkeerde ${cellLabel}. Waarom: ${lineLabel} heeft al ${filledCount} ${filledValue}${filledCount === 1 ? '' : 's'}, dus de resterende open ${cellLabel} moeten ${targetValue} zijn om de lijn in balans te houden.`,
    };
  }

  return {
    title: `Next move in ${lineLabel}`,
    body: `Place ${targetValue} in the highlighted ${cellLabel}. Why: ${lineLabel} already has ${filledCount} ${filledValue}${filledCount === 1 ? '' : 's'}, so the remaining open ${cellLabel} must be ${targetValue} to keep the line balanced.`,
  };
}

export function buildEliminateFilledLinesNextMove({
  lineKind,
  lineIndex,
  matchingLineIndex,
  targetValue,
  targetCount,
}: EliminateFilledLinesProgressParams): NextMoveCopy {
  const lineLabel = formatLine(lineKind, lineIndex);
  const matchingLineLabel = formatLine(lineKind, matchingLineIndex);
  const cellLabel = pluralize(targetCount, 'cell', 'cells');

  if (getCurrentLanguage() === 'nl') {
    return {
      title: `Volgende zet in ${lineLabel}`,
      body: `Plaats ${targetValue} in de gemarkeerde ${cellLabel}. Waarom: als ${lineLabel} gelijk werd aan ${matchingLineLabel}, zouden complete ${pluralize(targetCount, lineKind, `${lineKind}s`)} niet uniek meer zijn.`,
    };
  }

  return {
    title: `Next move in ${lineLabel}`,
    body: `Place ${targetValue} in the highlighted ${cellLabel}. Why: if ${lineLabel} matched ${matchingLineLabel}, the completed ${pluralize(targetCount, lineKind, `${lineKind}s`)} would stop being unique.`,
  };
}

export function buildEliminateImpossibleCombinationsNextMove({
  lineKind,
  lineIndex,
  blockedValue,
  targetValue,
}: EliminateImpossibleCombinationsProgressParams): NextMoveCopy {
  const lineLabel = formatLine(lineKind, lineIndex);

  if (getCurrentLanguage() === 'nl') {
    return {
      title: `Volgende zet in ${lineLabel}`,
      body: `Plaats ${targetValue} in de gemarkeerde cel. Waarom: als deze cel ${blockedValue} was, zou ${lineLabel} later een ongeldig trio forceren, dus ${targetValue} is de enige waarde die de lijn oplosbaar houdt.`,
    };
  }

  return {
    title: `Next move in ${lineLabel}`,
    body: `Place ${targetValue} in the highlighted cell. Why: if this cell were ${blockedValue}, ${lineLabel} would force an invalid trio later, so ${targetValue} is the only value that keeps the line solvable.`,
  };
}

export function buildAvoidTriosRepair({
  lineKind,
  lineIndex,
  repeatedValue,
}: AvoidTriosRecoveryParams): NextMoveCopy {
  const lineLabel = formatLine(lineKind, lineIndex);

  if (getCurrentLanguage() === 'nl') {
    return {
      title: `Volgende zet om ${lineLabel} te herstellen`,
      body: `Pas een gemarkeerde cel in ${lineLabel} aan. Waarom: drie ${repeatedValue}'en op rij breken de regel zonder trio's.`,
    };
  }

  return {
    title: `Next move to repair ${lineLabel}`,
    body: `Change one highlighted cell in ${lineLabel}. Why: three ${repeatedValue}s in a row break the no-trios rule.`,
  };
}

export function buildCompleteLinesRepair({
  lineKind,
  lineIndex,
  filledValue,
  filledCount,
  limit,
}: CompleteLinesRecoveryParams): NextMoveCopy {
  const lineLabel = formatLine(lineKind, lineIndex);

  if (getCurrentLanguage() === 'nl') {
    return {
      title: `Volgende zet om ${lineLabel} opnieuw in balans te brengen`,
      body: `Pas een gemarkeerde cel in ${lineLabel} aan. Waarom: ${lineLabel} bevat al ${filledCount} ${filledValue}${filledCount === 1 ? '' : 's'}, maar de limiet is ${limit}.`,
    };
  }

  return {
    title: `Next move to rebalance ${lineLabel}`,
    body: `Change one highlighted cell in ${lineLabel}. Why: ${lineLabel} already contains ${filledCount} ${filledValue}${filledCount === 1 ? '' : 's'}, but limit is ${limit}.`,
  };
}

export function buildEliminateFilledLinesRepair({
  lineKind,
  firstLineIndex,
  secondLineIndex,
}: EliminateFilledLinesRecoveryParams): NextMoveCopy {
  const firstLineLabel = formatLine(lineKind, firstLineIndex);
  const secondLineLabel = formatLine(lineKind, secondLineIndex);
  const lineLabel = pluralize(2, lineKind, `${lineKind}s`);

  if (getCurrentLanguage() === 'nl') {
    return {
      title: `Volgende zet om gelijke ${lineLabel} te scheiden`,
      body: `Pas een gemarkeerde cel aan. Waarom: ${firstLineLabel} en ${secondLineLabel} zijn gelijk, maar complete ${lineLabel} moeten uniek blijven.`,
    };
  }

  return {
    title: `Next move to separate matching ${lineLabel}`,
    body: `Change one highlighted cell. Why: ${firstLineLabel} and ${secondLineLabel} match, but completed ${lineLabel} must stay unique.`,
  };
}
import { getCurrentLanguage } from '../../../app/i18n';
