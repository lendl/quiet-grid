import type { PuzzleAnalysisPayload, PuzzleLossAnalysisSource } from '../../../../app/analysis/types';
import { getMinesweeperAnalysisContent } from '../../i18n';
import type { MinesweeperPlaySession } from '../../playContract';
import { revealMinesweeperCell } from '../../rules';
import type { MinesweeperBoard } from '../../types';
import { analyzeMinesweeperLogicalMoves } from '../nextMove';
import type {
  MinesweeperAnalysisPayload,
  MinesweeperAnalysisStep,
  MinesweeperLossAnalysisSource,
} from './types';

function cloneBoard(board: MinesweeperBoard): MinesweeperBoard {
  return {
    ...board,
    cells: board.cells.map((row) => row.map((cell) => ({ ...cell }))),
  };
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

function isMinesweeperLossAnalysisSource(
  source: PuzzleLossAnalysisSource | undefined,
): source is MinesweeperLossAnalysisSource {
  if (!source || source.puzzleTypeId !== 'minesweeper') {
    return false;
  }

  const payload = source.payload as Partial<MinesweeperLossAnalysisSource['payload']> | undefined;
  return Boolean(payload?.puzzle)
    && typeof payload?.puzzle?.difficulty === 'string'
    && typeof payload?.puzzle?.rows === 'number'
    && typeof payload?.puzzle?.cols === 'number'
    && typeof payload?.puzzle?.mines === 'number'
    && isMinesweeperBoard(payload?.board);
}

function buildAfterState(source: MinesweeperLossAnalysisSource, analysis: ReturnType<typeof analyzeMinesweeperLogicalMoves>) {
  if (!analysis) {
    return null;
  }

  let nextBoard = cloneBoard(source.payload.board);
  nextBoard.status = 'playing';

  analysis.mineTargetCells.forEach(({ row, col }) => {
    const cell = nextBoard.cells[row]?.[col];
    if (!cell || cell.state !== 'hidden') {
      return;
    }

    cell.state = 'flagged';
  });

  analysis.safeTargetCells.forEach(({ row, col }) => {
    const cell = nextBoard.cells[row]?.[col];
    if (!cell) {
      return;
    }

    if (cell.state === 'flagged') {
      cell.state = 'hidden';
    }

    nextBoard = revealMinesweeperCell(nextBoard, source.payload.puzzle, row, col);
  });

  return nextBoard;
}

function buildMinesweeperAnalysisInternal(source: MinesweeperLossAnalysisSource): MinesweeperAnalysisPayload | null {
  const analysis = analyzeMinesweeperLogicalMoves(source.payload.board);
  if (!analysis) {
    return null;
  }

  const content = getMinesweeperAnalysisContent();
  const copy = content.lossSummary({
    safeCount: analysis.safeTargetCells.length,
    mineCount: analysis.mineTargetCells.length,
  });
  const step: MinesweeperAnalysisStep = {
    key: 'step-1',
    title: copy.title,
    body: copy.body,
    evidenceCells: analysis.evidenceCells,
    targetCells: [...analysis.safeTargetCells, ...analysis.mineTargetCells],
    highlightRows: [],
    highlightCols: [],
    beforeState: cloneBoard(source.payload.board),
    afterState: buildAfterState(source, analysis) ?? cloneBoard(source.payload.board),
    safeTargetCells: analysis.safeTargetCells,
    mineTargetCells: analysis.mineTargetCells,
  };

  return {
    puzzleTypeId: 'minesweeper',
    steps: [step],
    payload: {
      labels: {
        evidence: content.legendEvidence,
        safe: content.legendSafe,
        mine: content.legendMine,
      },
    },
  };
}

export function buildMinesweeperLossAnalysisSource(session: unknown): PuzzleLossAnalysisSource | null {
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

export function supportsMinesweeperLossAnalysis(source: PuzzleLossAnalysisSource | undefined): boolean {
  return isMinesweeperLossAnalysisSource(source) && buildMinesweeperAnalysisInternal(source) !== null;
}

export function buildMinesweeperAnalysis(source: PuzzleLossAnalysisSource): PuzzleAnalysisPayload {
  if (!isMinesweeperLossAnalysisSource(source)) {
    throw new Error('Minesweeper analysis source is invalid.');
  }

  const analysis = buildMinesweeperAnalysisInternal(source);
  if (!analysis) {
    throw new Error('Minesweeper analysis is unavailable for this loss state.');
  }

  return analysis;
}
