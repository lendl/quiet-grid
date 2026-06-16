import { getColumn } from '../../core';
import type {
  TakuzuNextMoveCell,
  TakuzuNextMoveHint,
  TakuzuNextMoveTargetCell,
  TakuzuNextMoveRuleKey,
  CellValue,
  Grid,
} from '../../types';
import type { ImpossibleCombinationInsight } from '../../core';

export type LineKind = 'row' | 'column';

export type CandidateMove = {
  row: number;
  col: number;
  value: 0 | 1;
  ruleKey: TakuzuNextMoveRuleKey;
  lineKind: LineKind;
  lineIndex: number;
  matchingLineIndex?: number;
  impossibleCombinationInsight?: ImpossibleCombinationInsight;
};

export function getLine(board: Grid, lineKind: LineKind, lineIndex: number): CellValue[] {
  return lineKind === 'row' ? board[lineIndex] : getColumn(board, lineIndex);
}

export function getLineIndexes(size: number): number[] {
  return Array.from({ length: size }, (_, index) => index);
}

export function buildLineCells(
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

export function buildLineTargets(
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

export function toHint(candidate: Omit<TakuzuNextMoveHint, 'kind'>): TakuzuNextMoveHint {
  return {
    kind: 'progress',
    ...candidate,
  };
}
