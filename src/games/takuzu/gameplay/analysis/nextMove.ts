import {
  buildAvoidTriosNextMove,
  buildAvoidTriosRepair,
  buildCompleteLinesNextMove,
  buildCompleteLinesRepair,
  buildEliminateFilledLinesNextMove,
  buildEliminateFilledLinesRepair,
  buildEliminateImpossibleCombinationsNextMove,
  buildFindPairsNextMove,
  buildPausedNextMove,
} from './content';
import {
  countValue,
  findAvoidTrioMoveInLine,
  findEmptyIndexes,
  findPairMoveInLine,
  getColumn,
  getImpossibleCombinationInsight,
  type HumanProofRule,
  type ImpossibleCombinationInsight,
  otherValue,
} from '../../core';
import { getCurrentLanguage } from '../../../../app/i18n';
import type {
  TakuzuNextMoveCell,
  TakuzuNextMoveHint,
  TakuzuNextMoveRuleKey,
  TakuzuNextMoveTargetCell,
  CellValue,
  Grid,
} from '../../types';

type LineKind = 'row' | 'column';

type CandidateMove = {
  row: number;
  col: number;
  value: 0 | 1;
  ruleKey: TakuzuNextMoveRuleKey;
  lineKind: LineKind;
  lineIndex: number;
  matchingLineIndex?: number;
  impossibleCombinationInsight?: ImpossibleCombinationInsight;
};

function getHumanProofRuleLabel(rule: HumanProofRule | null): string {
  const language = getCurrentLanguage();

  if (rule === null) {
    switch (language) {
      case 'nl':
        return 'de regels';
      case 'de':
        return 'den Regeln';
      case 'fr':
        return 'les règles';
      case 'es':
        return 'las reglas';
      case 'en':
      default:
        return 'the rules';
    }
  }

  switch (language) {
    case 'nl':
      switch (rule) {
        case 'find-pairs':
          return 'de regel voor gelijke paren';
        case 'avoid-trios':
          return "de regel zonder trio's";
        case 'complete-lines':
          return 'de regel voor complete lijnen';
        case 'eliminate-filled-lines':
          return 'de regel voor het uitsluiten van gelijke lijnen';
      }
    case 'de':
      switch (rule) {
        case 'find-pairs':
          return 'der Paare-Regel';
        case 'avoid-trios':
          return 'der Keine-Drillinge-Regel';
        case 'complete-lines':
          return 'der Regel für vollständige Linien';
        case 'eliminate-filled-lines':
          return 'der Regel zum Ausschließen gleicher Linien';
      }
    case 'fr':
      switch (rule) {
        case 'find-pairs':
          return 'la règle des paires';
        case 'avoid-trios':
          return 'la règle anti-trio';
        case 'complete-lines':
          return 'la règle des lignes complètes';
        case 'eliminate-filled-lines':
          return "la règle d’élimination des lignes identiques";
      }
    case 'es':
      switch (rule) {
        case 'find-pairs':
          return 'la regla de las parejas';
        case 'avoid-trios':
          return 'la regla de evitar tríos';
        case 'complete-lines':
          return 'la regla de completar líneas';
        case 'eliminate-filled-lines':
          return 'la regla de eliminar líneas iguales';
      }
    case 'en':
    default:
      switch (rule) {
        case 'find-pairs':
          return 'the find pairs rule';
        case 'avoid-trios':
          return 'the avoid trios rule';
        case 'complete-lines':
          return 'the complete lines rule';
        case 'eliminate-filled-lines':
          return 'the eliminate filled lines rule';
      }
  }

  return 'the rules';
}

function getLocalizedCellLabel(): string {
  switch (getCurrentLanguage()) {
    case 'nl':
      return 'cel';
    case 'de':
      return 'Zelle';
    case 'fr':
      return 'case';
    case 'es':
      return 'celda';
    case 'en':
    default:
      return 'cell';
  }
}

function getHumanProofSummaryLabel(stepCount: number, rule: HumanProofRule | null): string {
  if (stepCount === 1) {
    return getHumanProofRuleLabel(rule);
  }

  return getHumanProofRuleLabel(null);
}

function getLine(board: Grid, lineKind: LineKind, lineIndex: number): CellValue[] {
  return lineKind === 'row' ? board[lineIndex] : getColumn(board, lineIndex);
}

function getLineIndexes(size: number): number[] {
  return Array.from({ length: size }, (_, index) => index);
}

function buildLineCells(
  lineKind: LineKind,
  lineIndex: number,
  indexes: number[],
): TakuzuNextMoveCell[] {
  return indexes.map((index) => (
    lineKind === 'row'
      ? { row: lineIndex, col: index }
      : { row: index, col: lineIndex }
  ));
}

function buildLineTargets(
  lineKind: LineKind,
  lineIndex: number,
  indexes: number[],
  value: 0 | 1,
): TakuzuNextMoveTargetCell[] {
  return indexes.map((index) => (
    lineKind === 'row'
      ? { row: lineIndex, col: index, value }
      : { row: index, col: lineIndex, value }
  ));
}

function toHint(candidate: Omit<TakuzuNextMoveHint, 'kind'>): TakuzuNextMoveHint {
  return {
    kind: 'progress',
    ...candidate,
  };
}

function createPausedHint(): TakuzuNextMoveHint {
  const paused = buildPausedNextMove();
  return {
    kind: 'paused',
    title: paused.title,
    body: paused.body,
    evidenceCells: [],
    targetCells: [],
    highlightRows: [],
    highlightCols: [],
  };
}

function buildProgressHint(board: Grid, move: CandidateMove): TakuzuNextMoveHint {
  const line = getLine(board, move.lineKind, move.lineIndex);
  const targetIndex = move.lineKind === 'row' ? move.col : move.row;

  switch (move.ruleKey) {
    case 'find-pairs': {
      const repeatedIndexes =
        targetIndex + 2 < line.length
        && line[targetIndex + 1] !== null
        && line[targetIndex + 1] === line[targetIndex + 2]
          ? [targetIndex + 1, targetIndex + 2]
          : [targetIndex - 2, targetIndex - 1];
      const repeatedValue = line[repeatedIndexes[0]] as 0 | 1;
      const fp = buildFindPairsNextMove({
        lineKind: move.lineKind,
        lineIndex: move.lineIndex,
        repeatedValue,
        targetValue: move.value,
        targetCount: 1,
      });

      return toHint({
        ruleKey: move.ruleKey,
        title: fp.title,
        body: fp.body,
        evidenceCells: buildLineCells(move.lineKind, move.lineIndex, repeatedIndexes),
        targetCells: buildLineTargets(move.lineKind, move.lineIndex, [targetIndex], move.value),
        highlightRows: move.lineKind === 'row' ? [move.lineIndex] : [],
        highlightCols: move.lineKind === 'column' ? [move.lineIndex] : [],
      });
    }

    case 'avoid-trios': {
      const repeatedIndexes = [targetIndex - 1, targetIndex + 1];
      const repeatedValue = line[repeatedIndexes[0]] as 0 | 1;
      const atr = buildAvoidTriosNextMove({
        lineKind: move.lineKind,
        lineIndex: move.lineIndex,
        repeatedValue,
        targetValue: move.value,
      });

      return toHint({
        ruleKey: move.ruleKey,
        title: atr.title,
        body: atr.body,
        evidenceCells: buildLineCells(move.lineKind, move.lineIndex, repeatedIndexes),
        targetCells: buildLineTargets(move.lineKind, move.lineIndex, [targetIndex], move.value),
        highlightRows: move.lineKind === 'row' ? [move.lineIndex] : [],
        highlightCols: move.lineKind === 'column' ? [move.lineIndex] : [],
      });
    }

    case 'complete-lines': {
      const filledValue = otherValue(move.value);
      const evidenceIndexes = line.flatMap((value, index) => (value === filledValue ? [index] : []));
      const clp = buildCompleteLinesNextMove({
        lineKind: move.lineKind,
        lineIndex: move.lineIndex,
        filledValue,
        filledCount: evidenceIndexes.length,
        targetValue: move.value,
        targetCount: 1,
      });

      return toHint({
        ruleKey: move.ruleKey,
        title: clp.title,
        body: clp.body,
        evidenceCells: buildLineCells(move.lineKind, move.lineIndex, evidenceIndexes),
        targetCells: buildLineTargets(move.lineKind, move.lineIndex, [targetIndex], move.value),
        highlightRows: move.lineKind === 'row' ? [move.lineIndex] : [],
        highlightCols: move.lineKind === 'column' ? [move.lineIndex] : [],
      });
    }

    case 'eliminate-filled-lines': {
      const size = board.length;
      const allIndexes = getLineIndexes(size);
      const emptyIndexes = findEmptyIndexes(line);
      const efl = buildEliminateFilledLinesNextMove({
        lineKind: move.lineKind,
        lineIndex: move.lineIndex,
        matchingLineIndex: move.matchingLineIndex as number,
        targetValue: move.value,
        targetCount: 1,
      });

      return toHint({
        ruleKey: move.ruleKey,
        title: efl.title,
        body: efl.body,
        evidenceCells: [
          ...buildLineCells(
            move.lineKind,
            move.lineIndex,
            allIndexes.filter((index) => line[index] !== null && !emptyIndexes.includes(index)),
          ),
          ...buildLineCells(move.lineKind, move.matchingLineIndex as number, allIndexes),
        ],
        targetCells: buildLineTargets(move.lineKind, move.lineIndex, [targetIndex], move.value),
        highlightRows: move.lineKind === 'row' ? [move.lineIndex, move.matchingLineIndex as number] : [],
        highlightCols: move.lineKind === 'column' ? [move.lineIndex, move.matchingLineIndex as number] : [],
      });
    }

    case 'eliminate-impossible-combinations': {
      const insight = move.impossibleCombinationInsight;
      if (!insight) {
        throw new Error('Missing impossible combination insight for progress hint.');
      }

      const proofRuleLabel = getHumanProofSummaryLabel(insight.proofStepCount, insight.proofUsesRule);

      const eic = buildEliminateImpossibleCombinationsNextMove({
        lineKind: insight.lineKind,
        lineIndex: insight.lineIndex,
        validCompletionCount: insight.validCompletionCount,
        blockedValue: insight.blockedValue,
        targetValue: insight.forcedValue,
        cellLabel: getLocalizedCellLabel(),
        contradictionKind: insight.contradictionKind,
        contradictionLineKind: insight.contradictionLineKind,
        contradictionLineIndex: insight.contradictionLineIndex,
        proofRuleLabel,
      });

      return toHint({
        ruleKey: move.ruleKey,
        title: eic.title,
        body: eic.body,
        evidenceCells: buildLineCells(
          insight.lineKind,
          insight.lineIndex,
          getLine(board, insight.lineKind, insight.lineIndex)
            .flatMap((value, index) => (value !== null ? [index] : [])),
        ),
        targetCells: buildLineTargets(
          insight.lineKind,
          insight.lineIndex,
          [insight.lineKind === 'row' ? insight.col : insight.row],
          insight.forcedValue,
        ),
        highlightRows: insight.lineKind === 'row' ? [insight.lineIndex] : [],
        highlightCols: insight.lineKind === 'column' ? [insight.lineIndex] : [],
      });
    }
  }
}

function findTripleMismatch(board: Grid): TakuzuNextMoveHint | null {
  const size = board.length;

  for (let rowIndex = 0; rowIndex < size; rowIndex += 1) {
    for (let colIndex = 0; colIndex <= size - 3; colIndex += 1) {
      const first = board[rowIndex][colIndex];
      const second = board[rowIndex][colIndex + 1];
      const third = board[rowIndex][colIndex + 2];

      if (first === null || first !== second || second !== third) continue;

      const atr = buildAvoidTriosRepair({
        lineKind: 'row',
        lineIndex: rowIndex,
        repeatedValue: first,
      });

      return {
        kind: 'recovery',
        ruleKey: 'avoid-trios',
        title: atr.title,
        body: atr.body,
        evidenceCells: buildLineCells('row', rowIndex, [colIndex, colIndex + 1, colIndex + 2]),
        targetCells: [],
        highlightRows: [rowIndex],
        highlightCols: [],
      };
    }
  }

  for (let colIndex = 0; colIndex < size; colIndex += 1) {
    for (let rowIndex = 0; rowIndex <= size - 3; rowIndex += 1) {
      const first = board[rowIndex][colIndex];
      const second = board[rowIndex + 1][colIndex];
      const third = board[rowIndex + 2][colIndex];

      if (first === null || first !== second || second !== third) continue;

      const atr = buildAvoidTriosRepair({
        lineKind: 'column',
        lineIndex: colIndex,
        repeatedValue: first,
      });

      return {
        kind: 'recovery',
        ruleKey: 'avoid-trios',
        title: atr.title,
        body: atr.body,
        evidenceCells: buildLineCells('column', colIndex, [rowIndex, rowIndex + 1, rowIndex + 2]),
        targetCells: [],
        highlightRows: [],
        highlightCols: [colIndex],
      };
    }
  }

  return null;
}

function findBalanceMismatch(board: Grid): TakuzuNextMoveHint | null {
  const size = board.length;
  const limit = size / 2;

  for (let rowIndex = 0; rowIndex < size; rowIndex += 1) {
    const row = board[rowIndex];
    const zeroIndexes = row.flatMap((value, index) => (value === 0 ? [index] : []));
    const oneIndexes = row.flatMap((value, index) => (value === 1 ? [index] : []));

    if (zeroIndexes.length > limit) {
      const clr = buildCompleteLinesRepair({
        lineKind: 'row',
        lineIndex: rowIndex,
        filledValue: 0,
        filledCount: zeroIndexes.length,
        limit,
      });

      return {
        kind: 'recovery',
        ruleKey: 'complete-lines',
        title: clr.title,
        body: clr.body,
        evidenceCells: buildLineCells('row', rowIndex, zeroIndexes),
        targetCells: [],
        highlightRows: [rowIndex],
        highlightCols: [],
      };
    }

    if (oneIndexes.length > limit) {
      const clr = buildCompleteLinesRepair({
        lineKind: 'row',
        lineIndex: rowIndex,
        filledValue: 1,
        filledCount: oneIndexes.length,
        limit,
      });

      return {
        kind: 'recovery',
        ruleKey: 'complete-lines',
        title: clr.title,
        body: clr.body,
        evidenceCells: buildLineCells('row', rowIndex, oneIndexes),
        targetCells: [],
        highlightRows: [rowIndex],
        highlightCols: [],
      };
    }
  }

  for (let colIndex = 0; colIndex < size; colIndex += 1) {
    const column = getColumn(board, colIndex);
    const zeroIndexes = column.flatMap((value, index) => (value === 0 ? [index] : []));
    const oneIndexes = column.flatMap((value, index) => (value === 1 ? [index] : []));

    if (zeroIndexes.length > limit) {
      const clr = buildCompleteLinesRepair({
        lineKind: 'column',
        lineIndex: colIndex,
        filledValue: 0,
        filledCount: zeroIndexes.length,
        limit,
      });

      return {
        kind: 'recovery',
        ruleKey: 'complete-lines',
        title: clr.title,
        body: clr.body,
        evidenceCells: buildLineCells('column', colIndex, zeroIndexes),
        targetCells: [],
        highlightRows: [],
        highlightCols: [colIndex],
      };
    }

    if (oneIndexes.length > limit) {
      const clr = buildCompleteLinesRepair({
        lineKind: 'column',
        lineIndex: colIndex,
        filledValue: 1,
        filledCount: oneIndexes.length,
        limit,
      });

      return {
        kind: 'recovery',
        ruleKey: 'complete-lines',
        title: clr.title,
        body: clr.body,
        evidenceCells: buildLineCells('column', colIndex, oneIndexes),
        targetCells: [],
        highlightRows: [],
        highlightCols: [colIndex],
      };
    }
  }

  return null;
}

function findDuplicateMismatch(board: Grid): TakuzuNextMoveHint | null {
  const size = board.length;
  const allIndexes = getLineIndexes(size);
  const completedRows = board
    .map((row, rowIndex) => ({ row, rowIndex }))
    .filter(({ row }) => row.every((value) => value !== null));

  for (let firstIndex = 0; firstIndex < completedRows.length; firstIndex += 1) {
    for (let secondIndex = firstIndex + 1; secondIndex < completedRows.length; secondIndex += 1) {
      const firstRow = completedRows[firstIndex];
      const secondRow = completedRows[secondIndex];

      if (!firstRow.row.every((value, index) => value === secondRow.row[index])) continue;

      const efl = buildEliminateFilledLinesRepair({
        lineKind: 'row',
        firstLineIndex: firstRow.rowIndex,
        secondLineIndex: secondRow.rowIndex,
      });

      return {
        kind: 'recovery',
        ruleKey: 'eliminate-filled-lines',
        title: efl.title,
        body: efl.body,
        evidenceCells: [
          ...buildLineCells('row', firstRow.rowIndex, allIndexes),
          ...buildLineCells('row', secondRow.rowIndex, allIndexes),
        ],
        targetCells: [],
        highlightRows: [firstRow.rowIndex, secondRow.rowIndex],
        highlightCols: [],
      };
    }
  }

  const completedColumns = allIndexes
    .map((colIndex) => ({
      column: getColumn(board, colIndex),
      colIndex,
    }))
    .filter(({ column }) => column.every((value) => value !== null));

  for (let firstIndex = 0; firstIndex < completedColumns.length; firstIndex += 1) {
    for (let secondIndex = firstIndex + 1; secondIndex < completedColumns.length; secondIndex += 1) {
      const firstColumn = completedColumns[firstIndex];
      const secondColumn = completedColumns[secondIndex];

      if (!firstColumn.column.every((value, index) => value === secondColumn.column[index])) continue;

      const efl = buildEliminateFilledLinesRepair({
        lineKind: 'column',
        firstLineIndex: firstColumn.colIndex,
        secondLineIndex: secondColumn.colIndex,
      });

      return {
        kind: 'recovery',
        ruleKey: 'eliminate-filled-lines',
        title: efl.title,
        body: efl.body,
        evidenceCells: [
          ...buildLineCells('column', firstColumn.colIndex, allIndexes),
          ...buildLineCells('column', secondColumn.colIndex, allIndexes),
        ],
        targetCells: [],
        highlightRows: [],
        highlightCols: [firstColumn.colIndex, secondColumn.colIndex],
      };
    }
  }

  return null;
}

function findPairMove(board: Grid): CandidateMove | null {
  const size = board.length;

  for (let row = 0; row < size; row += 1) {
    const move = findPairMoveInLine(board[row]);
    if (move) {
      return {
        row,
        col: move.index,
        value: move.value,
        ruleKey: 'find-pairs',
        lineKind: 'row',
        lineIndex: row,
      };
    }
  }

  for (let col = 0; col < size; col += 1) {
    const move = findPairMoveInLine(getColumn(board, col));
    if (move) {
      return {
        row: move.index,
        col,
        value: move.value,
        ruleKey: 'find-pairs',
        lineKind: 'column',
        lineIndex: col,
      };
    }
  }

  return null;
}

function findAvoidTrioMove(board: Grid): CandidateMove | null {
  const size = board.length;

  for (let row = 0; row < size; row += 1) {
    const move = findAvoidTrioMoveInLine(board[row]);
    if (move) {
      return {
        row,
        col: move.index,
        value: move.value,
        ruleKey: 'avoid-trios',
        lineKind: 'row',
        lineIndex: row,
      };
    }
  }

  for (let col = 0; col < size; col += 1) {
    const move = findAvoidTrioMoveInLine(getColumn(board, col));
    if (move) {
      return {
        row: move.index,
        col,
        value: move.value,
        ruleKey: 'avoid-trios',
        lineKind: 'column',
        lineIndex: col,
      };
    }
  }

  return null;
}

function findCompleteLineMove(board: Grid): CandidateMove | null {
  const size = board.length;
  const half = size / 2;

  for (let row = 0; row < size; row += 1) {
    const line = board[row];
    const zeroes = countValue(line, 0);
    const ones = countValue(line, 1);
    if (zeroes === half || ones === half) {
      const fillValue = zeroes === half ? 1 : 0;
      const col = line.findIndex((cell) => cell === null);
      if (col !== -1) {
        return {
          row,
          col,
          value: fillValue,
          ruleKey: 'complete-lines',
          lineKind: 'row',
          lineIndex: row,
        };
      }
    }
  }

  for (let col = 0; col < size; col += 1) {
    const line = getColumn(board, col);
    const zeroes = countValue(line, 0);
    const ones = countValue(line, 1);
    if (zeroes === half || ones === half) {
      const fillValue = zeroes === half ? 1 : 0;
      const row = line.findIndex((cell) => cell === null);
      if (row !== -1) {
        return {
          row,
          col,
          value: fillValue,
          ruleKey: 'complete-lines',
          lineKind: 'column',
          lineIndex: col,
        };
      }
    }
  }

  return null;
}

function findEliminateFilledLinesRow(board: Grid): CandidateMove | null {
  const size = board.length;
  const completeRows = board
    .map((line, index) => ({ line, index }))
    .filter(({ line }) => line.every((cell) => cell !== null));

  for (let row = 0; row < size; row += 1) {
    const line = board[row];
    const emptyCols = line.flatMap((cell, col) => (cell === null ? [col] : []));
    if (emptyCols.length !== 2) {
      continue;
    }

    for (const complete of completeRows) {
      if (complete.index === row) {
        continue;
      }

      const matches = line.every((cell, col) => cell === null || cell === complete.line[col]);
      if (!matches) {
        continue;
      }

      const col = emptyCols[0];
      return {
        row,
        col,
        value: otherValue(complete.line[col] as 0 | 1),
        ruleKey: 'eliminate-filled-lines',
        lineKind: 'row',
        lineIndex: row,
        matchingLineIndex: complete.index,
      };
    }
  }

  return null;
}

function findEliminateFilledLinesColumn(board: Grid): CandidateMove | null {
  const size = board.length;
  const completeCols = Array.from({ length: size }, (_, index) => ({
    line: getColumn(board, index),
    index,
  })).filter(({ line }) => line.every((cell) => cell !== null));

  for (let col = 0; col < size; col += 1) {
    const line = getColumn(board, col);
    const emptyRows = line.flatMap((cell, row) => (cell === null ? [row] : []));
    if (emptyRows.length !== 2) {
      continue;
    }

    for (const complete of completeCols) {
      if (complete.index === col) {
        continue;
      }

      const matches = line.every((cell, row) => cell === null || cell === complete.line[row]);
      if (!matches) {
        continue;
      }

      const row = emptyRows[0];
      return {
        row,
        col,
        value: otherValue(complete.line[row] as 0 | 1),
        ruleKey: 'eliminate-filled-lines',
        lineKind: 'column',
        lineIndex: col,
        matchingLineIndex: complete.index,
      };
    }
  }

  return null;
}

function findImpossibleCombinationMove(board: Grid): CandidateMove | null {
  const size = board.length;

  for (let row = 0; row < size; row += 1) {
    for (let col = 0; col < size; col += 1) {
      const insight = getImpossibleCombinationInsight(board, row, col);
      if (insight) {
        return {
          row: insight.row,
          col: insight.col,
          value: insight.forcedValue,
          ruleKey: 'eliminate-impossible-combinations',
          lineKind: insight.lineKind,
          lineIndex: insight.lineIndex,
          impossibleCombinationInsight: insight,
        };
      }
    }
  }

  return null;
}

export function getTakuzuRecoveryHint(board: Grid): TakuzuNextMoveHint | null {
  return findTripleMismatch(board)
    ?? findBalanceMismatch(board)
    ?? findDuplicateMismatch(board);
}

export function getTakuzuProgressHint(board: Grid): TakuzuNextMoveHint | null {
  const move =
    findPairMove(board) ??
    findAvoidTrioMove(board) ??
    findCompleteLineMove(board) ??
    findEliminateFilledLinesRow(board) ??
    findEliminateFilledLinesColumn(board) ??
    findImpossibleCombinationMove(board);

  return move ? buildProgressHint(board, move) : null;
}

export function getTakuzuNextMoveHint(board: Grid): TakuzuNextMoveHint {
  return getTakuzuRecoveryHint(board)
    ?? getTakuzuProgressHint(board)
    ?? createPausedHint();
}
