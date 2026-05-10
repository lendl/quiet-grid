import type { PuzzleAnalysisPayload, PuzzleAnalysisSource } from '../../../../../app/analysis/types';
import { getMinesweeperAnalysisContent } from '../../../i18n';
import type { MinesweeperPlaySession } from '../../../gameplay/playContract';
import type { MinesweeperBoard } from '../../../types';
import { buildPatternNextMove } from '../../../gameplay/analysis/content';
import { analyzeMinesweeperLogicalMoves, getNextMinesweeperSafeRevealMove } from '../../../gameplay/analysis/nextMove';
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
  if (!source || source.puzzleTypeId !== 'minesweeper') {
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

function buildMinesweeperAnalysisInternal(source: MinesweeperAnalysisSource): MinesweeperAnalysisPayload | null {
  const content = getMinesweeperAnalysisContent();
  const board = cloneBoard(source.payload.board);
  const analysis = analyzeMinesweeperLogicalMoves(board, new Set<string>());
  if (!analysis) {
    return null;
  }

  const steps: MinesweeperAnalysisStep[] = analysis.steps.map((move, index) => {
    const copy = move.moveKind === 'flag-mine'
      ? content.groupedFlagStep({
          mineCount: move.targetCells.length,
        })
      : buildPatternNextMove({
          patternKey: move.patternKey ?? 'all-mines-accounted-for',
          clueCell: move.primaryClueCell,
          secondaryClueCell: move.secondaryClueCell,
          targetCount: move.targetCells.length,
          mineCount: move.mineCount,
        });
    const beforeState = cloneBoard(board);
    const afterState = move.moveKind === 'flag-mine'
      ? buildFlaggedBoardState(board, move.targetCells)
      : cloneBoard(board);

    return {
      key: `step-${index + 1}`,
      title: copy.title,
      body: copy.body,
      evidenceCells: move.evidenceCells,
      targetCells: move.targetCells,
      highlightRows: [],
      highlightCols: [],
      beforeState,
      afterState,
      safeTargetCells: move.moveKind === 'safe-reveal' ? move.targetCells : [],
      mineTargetCells: move.moveKind === 'flag-mine' ? move.targetCells : [],
    };
  });

  if (steps.length === 0) {
    return null;
  }

  return {
    puzzleTypeId: 'minesweeper',
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
    puzzleTypeId: 'minesweeper',
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
