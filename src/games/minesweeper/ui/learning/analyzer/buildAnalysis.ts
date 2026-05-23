import { groupItemsByKey } from '../../../../../app/analysis/grouping';
import type { PuzzleAnalysisPayload, PuzzleAnalysisSource } from '../../../../../app/analysis/types';
import { getMinesweeperAnalysisContent } from '../../../i18n';
import type { MinesweeperPlaySession } from '../../../gameplay/playContract';
import type { MinesweeperBoard } from '../../../types';
import { buildPatternNextMove } from '../../../gameplay/analysis/content';
import {
  analyzeMinesweeperLogicalMoves,
  getNextMinesweeperSafeRevealMove,
  type MinesweeperLogicalMoveStep,
} from '../../../gameplay/analysis/nextMove';
import type {
  MinesweeperAnalysisPayload,
  MinesweeperAnalysisStep,
  MinesweeperAnalysisSource,
} from './types';

function cloneBoard(board: MinesweeperBoard): MinesweeperBoard {
  return {
    ...board,
    cells: board.cells.map((row) => row.map((cell) => ({ ...cell }))),
  };
}

function buildFlaggedBoardState(
  board: MinesweeperBoard,
  targetCells: Array<{ row: number; col: number }>,
): MinesweeperBoard {
  const nextBoard = cloneBoard(board);

  targetCells.forEach(({ row, col }) => {
    const cell = nextBoard.cells[row]?.[col];
    if (!cell || cell.state !== 'hidden') {
      return;
    }

    cell.state = 'flagged';
  });

  return nextBoard;
}

function isMinesweeperBoard(value: unknown): value is MinesweeperBoard {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const candidate = value as Partial<MinesweeperBoard>;
  return typeof candidate.rows === 'number'
    && typeof candidate.cols === 'number'
    && typeof candidate.mines === 'number'
    && typeof candidate.generated === 'boolean'
    && (candidate.status === 'playing' || candidate.status === 'won' || candidate.status === 'lost')
    && Array.isArray(candidate.cells);
}

function isMinesweeperPlaySession(value: unknown): value is MinesweeperPlaySession {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const candidate = value as Partial<MinesweeperPlaySession>;
  return Boolean(candidate.puzzle)
    && typeof candidate.puzzle?.difficulty === 'string'
    && typeof candidate.puzzle?.rows === 'number'
    && typeof candidate.puzzle?.cols === 'number'
    && typeof candidate.puzzle?.mines === 'number'
    && isMinesweeperBoard(candidate.board);
}

function isMinesweeperAnalysisSource(
  source: PuzzleAnalysisSource | undefined,
): source is MinesweeperAnalysisSource {
  if (!source || source.gameId !== 'minesweeper') {
    return false;
  }

  const payload = source.payload as Partial<MinesweeperAnalysisSource['payload']> | undefined;
  return Boolean(payload?.puzzle)
    && typeof payload?.puzzle?.difficulty === 'string'
    && typeof payload?.puzzle?.rows === 'number'
    && typeof payload?.puzzle?.cols === 'number'
    && typeof payload?.puzzle?.mines === 'number'
    && isMinesweeperBoard(payload?.board);
}

function toCellKey(row: number, col: number): string {
  return `${row}:${col}`;
}

function compareCells(
  a: { row: number; col: number },
  b: { row: number; col: number },
): number {
  if (a.row !== b.row) {
    return a.row - b.row;
  }

  return a.col - b.col;
}

function dedupeCells<T extends { row: number; col: number }>(cells: T[]): T[] {
  const unique = new Map<string, T>();

  cells.forEach((cell) => {
    unique.set(toCellKey(cell.row, cell.col), cell);
  });

  return Array.from(unique.values()).sort(compareCells);
}

function getTargetGroupKey(step: MinesweeperLogicalMoveStep): string {
  const targets = dedupeCells(step.targetCells);
  return `${step.moveKind}:${targets.map((cell) => toCellKey(cell.row, cell.col)).join('|')}`;
}

function buildMinesweeperAnalysisInternal(source: MinesweeperAnalysisSource): MinesweeperAnalysisPayload | null {
  const content = getMinesweeperAnalysisContent();
  const board = cloneBoard(source.payload.board);
  const analysis = analyzeMinesweeperLogicalMoves(board, new Set<string>());
  if (!analysis) {
    return null;
  }

  const groupedMoves = groupItemsByKey(
    analysis.steps.filter((step) => step.moveKind === 'safe-reveal'),
    getTargetGroupKey,
  );

  const steps: MinesweeperAnalysisStep[] = groupedMoves.map(({ items }, index) => {
    const primaryMove = items[0];
    if (!primaryMove) {
      throw new Error('Minesweeper analysis group is empty.');
    }

    const mergedEvidenceCells = dedupeCells(items.flatMap((move) => move.evidenceCells));
    const mergedTargetCells = dedupeCells(items.flatMap((move) => move.targetCells));
    const copy = items.length > 1
      ? content.groupedSafeStep({
          targetCount: mergedTargetCells.length,
          reasonCount: items.length,
        })
      : buildPatternNextMove({
          patternKey: primaryMove.patternKey ?? 'all-mines-accounted-for',
          clueCell: primaryMove.primaryClueCell,
          secondaryClueCell: primaryMove.secondaryClueCell,
          targetCount: mergedTargetCells.length,
          mineCount: primaryMove.mineCount,
        });
    const beforeState = cloneBoard(board);
    const afterState = cloneBoard(board);

    return {
      key: `step-${index + 1}`,
      title: copy.title,
      body: copy.body,
      evidenceCells: mergedEvidenceCells,
      targetCells: mergedTargetCells,
      highlightRows: [],
      highlightCols: [],
      beforeState,
      afterState,
      safeTargetCells: mergedTargetCells,
      mineTargetCells: [],
    };
  });

  if (steps.length === 0) {
    return null;
  }

  return {
    gameId: 'minesweeper',
    steps,
    payload: {
      labels: {
        evidence: content.legendEvidence,
        safe: content.legendSafe,
        mine: content.legendMine,
      },
    },
  };
}

function hasSafeRevealStep(board: MinesweeperBoard, knownMineKeys: Set<string>): boolean {
  return getNextMinesweeperSafeRevealMove(board, knownMineKeys) !== null;
}

export function buildMinesweeperAnalysisSource(session: unknown): PuzzleAnalysisSource | null {
  if (!isMinesweeperPlaySession(session)) {
    return null;
  }

  return {
    gameId: 'minesweeper',
    payload: {
      puzzle: session.puzzle,
      board: cloneBoard(session.board),
    },
  };
}

export function supportsMinesweeperAnalysis(source: PuzzleAnalysisSource | undefined): boolean {
  if (!isMinesweeperAnalysisSource(source)) {
    return false;
  }

  return hasSafeRevealStep(cloneBoard(source.payload.board), new Set<string>());
}

export function buildMinesweeperAnalysis(source: PuzzleAnalysisSource): PuzzleAnalysisPayload {
  if (!isMinesweeperAnalysisSource(source)) {
    throw new Error('Minesweeper analysis source is invalid.');
  }

  const analysis = buildMinesweeperAnalysisInternal(source);
  if (!analysis) {
    throw new Error('Minesweeper analysis is unavailable for this loss state.');
  }

  return analysis;
}
